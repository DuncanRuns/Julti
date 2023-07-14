package xyz.duncanruns.julti.instance;

import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.HWND;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.affinity.AffinityManager;
import xyz.duncanruns.julti.instance.InstanceState.InWorldState;
import xyz.duncanruns.julti.management.ActiveWindowManager;
import xyz.duncanruns.julti.resetting.ResetHelper;
import xyz.duncanruns.julti.util.*;
import xyz.duncanruns.julti.util.FabricJarUtil.FabricJarInfo;
import xyz.duncanruns.julti.win32.User32;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static xyz.duncanruns.julti.util.SleepUtil.sleep;

public class MinecraftInstance {
    // Main information
    private final HWND hwnd;
    private int pid;
    private final Path path;
    private final String versionString;

    // Discoverable Information
    private GameOptions gameOptions = null;
    private String name = null;

    private final StateTracker stateTracker;
    private final KeyPresser presser;
    private final Scheduler scheduler;

    private boolean windowMissing = false;

    private boolean resetPressed = false;
    private boolean resetEverPressed = false;
    private boolean activeSinceReset = false;
    private boolean windowStateChangedToPlaying = false;
    private long lastSetVisible = 0;
    private boolean openedToLan = false;

    public MinecraftInstance(HWND hwnd, Path path, String versionString) {
        this.hwnd = hwnd;
        this.path = path;
        this.versionString = versionString;
        this.stateTracker = new StateTracker(path.resolve("wpstateout.txt"), this::onStateChange);
        this.presser = new KeyPresser(hwnd);
        this.scheduler = new Scheduler();
    }

    public MinecraftInstance(Path path) {
        this.hwnd = null;
        this.versionString = null;
        this.presser = null;
        this.scheduler = null;
        this.stateTracker = new StateTracker(path.resolve("wpstateout.txt"), null);

        this.path = path;
        this.windowMissing = true;
    }

    public void tick() {
        this.getStateTracker().tryUpdate();
        this.scheduler.checkSchedule();
    }

    /**
     * First checks if the window is already marked as missing, and if it is not marked already, calls the IsWindow
     * function to determine if it should be marked as missing.
     * <p>
     * This function uses a native windows call, to quickly check if the window is marked as missing, use {@link MinecraftInstance#isWindowMarkedMissing()}.
     *
     * @return true if the window is missing, otherwise false
     */
    public boolean checkWindowMissing() {
        if (this.isWindowMarkedMissing()) {
            return true;
        }
        if (!User32.INSTANCE.IsWindow(this.hwnd)) {
            this.markWindowMissing();
            return true;
        }
        return false;
    }

    /**
     * Check if the window is marked as missing. To actually check if the window is missing, use {@link MinecraftInstance#checkWindowMissing()}.
     *
     * @return true if the window has been marked as missing, otherwise false
     */
    public boolean isWindowMarkedMissing() {
        return this.windowMissing;
    }

    public void markWindowMissing() {
        if (!this.windowMissing) {
            Julti.log(Level.INFO, "Instance " + this.getName() + " has gone missing.");
        }

        this.windowMissing = true;
        this.scheduler.clear();
    }

    public void discoverInformation() {
        if (this.checkWindowMissing()) {
            return;
        }

        this.gameOptions = new GameOptions();

        // Find info like keybinds, standard settings, etc.

        // Process ID
        this.pid = PidUtil.getPidFromHwnd(this.hwnd);

        boolean pre113 = MCVersionUtil.isOlderThan(this.versionString, "1.13");
        // Keybinds
        this.gameOptions.createWorldKey = GameOptionsUtil.getKey(this.getPath(), "key_Create New World", pre113);
        this.gameOptions.leavePreviewKey = GameOptionsUtil.getKey(this.getPath(), "key_Leave Preview", pre113);
        this.gameOptions.fullscreenKey = GameOptionsUtil.getKey(this.getPath(), "key_key.fullscreen", pre113);
        this.gameOptions.chatKey = GameOptionsUtil.getKey(this.getPath(), "key_key.chat", pre113);

        this.gameOptions.pauseOnLostFocus = GameOptionsUtil.tryGetBoolOption(this.getPath(), "pauseOnLostFocus", true);

        this.checkFabricMods();

        this.discoverName();
    }

    private void checkFabricMods() {
        try {
            this.gameOptions.jars = FabricJarUtil.getAllJarInfos(this.getPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        FabricJarInfo wpInfo = FabricJarUtil.getJarInfo(this.gameOptions.jars, "worldpreview");

        boolean hasStateOutput = true;
        if (wpInfo == null && FabricJarUtil.getJarInfo(this.gameOptions.jars, "state-output") == null) {
            hasStateOutput = false;
        } else if (wpInfo != null) {
            Matcher matcher = Pattern.compile("\\d+").matcher(wpInfo.version);
            if (!matcher.find() || Integer.parseInt(matcher.group()) < 3) {
                hasStateOutput = false;
            }
        }

        if (!hasStateOutput) {
            Julti.log(Level.WARN, "Warning: Instance \"" + this + "\" does not have an updated version of world preview or the state output mod and will likely not function!");
        }

        boolean hasSS = FabricJarUtil.getJarInfo(this.gameOptions.jars, "standardsettings") != null;

        if (!hasSS) {
            Julti.log(Level.WARN, "Warning: Instance \"" + this + " does not have the standard settings mod!");
        }

    }

    private void discoverName() {
        // Get instance path
        Path instancePath = this.getPath();

        // Check MultiMC/Prism name
        if (this.usesMultiMC()) {
            try {
                Path mmcConfigPath = instancePath.getParent().resolve("instance.cfg");
                for (String line : Files.readAllLines(mmcConfigPath)) {
                    line = line.trim();
                    if (line.startsWith("name=")) {
                        this.name = StringEscapeUtils.unescapeJson(line.split("=")[1]);
                    }
                }
            } catch (Exception ignored) {
                // Failed to check if it uses MultiMC, ignore and move on to taking folder name
            }
        }

        if (instancePath.getName(instancePath.getNameCount() - 1).toString().equals(".minecraft")) {
            instancePath = instancePath.getParent();
            // If this runs, instancePath is no longer an accurate variable name, and describes the parent path
        }
        String name = instancePath.getName(instancePath.getNameCount() - 1).toString();
        if (name.equals("Roaming")) {
            name = "Default Launcher";
        }
        this.name = name;
    }

    private boolean usesMultiMC() {
        return Files.exists(this.getPath().getParent().resolve("instance.cfg"));
    }


    public MinecraftInstance createLazyCopy() {
        return new MinecraftInstance(this.getHwnd(), this.getPath(), this.getVersionString());
    }

    public HWND getHwnd() {
        return this.hwnd;
    }

    public int getPid() {
        return this.pid;
    }

    public Path getPath() {
        return this.path;
    }

    public String getVersionString() {
        return this.versionString;
    }

    public void reset() {
        this.scheduler.clear();
        // Press Reset Keys
        if (this.stateTracker.isCurrentState(InstanceState.TITLE)) {
            if (MCVersionUtil.isOlderThan(this.versionString, "1.9")) {
                this.presser.pressKey(this.gameOptions.createWorldKey); // Thanks pix
            } else if (MCVersionUtil.isOlderThan(this.versionString, "1.16")) {
                this.presser.pressShiftTabEnter();
            } else {
                this.presser.pressKey(this.gameOptions.createWorldKey);
            }
        } else {
            this.presser.pressKey(this.gameOptions.leavePreviewKey);
            this.presser.pressKey(this.gameOptions.createWorldKey);
        }

        // Set values
        this.resetPressed = true;
        this.resetEverPressed = true;
        this.openedToLan = false;
        this.activeSinceReset = false;

        // Jump affinity
        AffinityManager.jumpPrePreviewAffinity(this);

        // Increment Reset Counter
        ResetCounter.increment();

        if (this.windowStateChangedToPlaying) {
            // Do the window state change later to ensure resetting is fast
            Julti.doLater(() -> this.ensureResettingWindowState(true));
        }

        this.scheduler.schedule(() -> {
            if (this.resetPressed) {
                this.resetPressed = false;
            }
        }, 5000);
    }

    public void activate(boolean doingSetup) {
        if (this.isWindowMarkedMissing()) {
            return;
        }
        this.scheduler.clear();
        this.activeSinceReset = true;

        JultiOptions options = JultiOptions.getInstance();

        AffinityManager.pause();
        AffinityManager.jumpPlayingAffinity(this); // Affinity Jump (BRAND NEW TECH POGGERS)
        ActiveWindowManager.activateHwnd(this.hwnd);
        if (!doingSetup && (!options.autoFullscreen || options.usePlayingSizeWithFullscreen)) {
            this.ensurePlayingWindowState(false);
        }
        AffinityManager.unpause();

        if (this.stateTracker.isCurrentState(InstanceState.INWORLD)) {
            if (!doingSetup) {
                if ((options.unpauseOnSwitch || options.coopMode)) {
                    this.presser.pressEsc();
                    this.presser.pressEsc();
                    this.presser.pressEsc();
                }
                if (options.coopMode) {
                    this.openToLan(true);
                }
                if (options.autoFullscreen) {
                    this.presser.pressKey(this.gameOptions.fullscreenKey);
                }
            }
        }
        if (!options.autoFullscreen && doingSetup) {
            Julti.doLater(() -> this.ensureResettingWindowState(false));
        }
    }

    private void onStateChange() {
        // The next state after reset should be WAITING, if it changes to something that is not WAITING, send the key again
        if (this.resetPressed) {
            if (this.stateTracker.isCurrentState(InstanceState.WAITING) || this.stateTracker.isCurrentState(InstanceState.PREVIEWING)) {
                this.resetPressed = false;
                this.scheduler.clear();
            } else {
                return;
            }
        }
        switch (this.stateTracker.getInstanceState()) {
            case PREVIEWING:
                this.onPreviewLoad();
                break;
            case INWORLD:
                this.onWorldLoad();
                break;
        }
    }

    public int getResetSortingNum() {
        if (!this.resetEverPressed) {
            return 10000000;
        }
        if (this.resetPressed) {
            return -2;
        }
        return this.stateTracker.getResetSortingNum();
    }

    private void onWorldLoad() {
        this.onWorldLoad(false);
    }

    private void onWorldLoad(boolean bypassPieChartGate) {
        this.scheduler.clear();

        JultiOptions options = JultiOptions.getInstance();

        if (this.gameOptions.pauseOnLostFocus) {
            Julti.log(Level.WARN, "Instance " + this + " has pauseOnLostFocus, some features cannot be used!");
            return;
        }

        if (!bypassPieChartGate && options.pieChartOnLoad) {
            // Open pie chart
            this.presser.pressShiftF3();

            // Schedule the completion of onWorldLoad
            this.scheduler.schedule(() -> this.onWorldLoad(true), 150);

            return;
        }

        if (options.useF3) {
            // F3
            this.presser.pressF3Esc();
        } else {
            // No F3
            this.presser.pressEsc();
        }

        // Unpause if window is active
        if (ActiveWindowManager.isWindowActive(this.hwnd)) {
            if (options.unpauseOnSwitch || options.coopMode) {
                this.presser.pressEsc();
            }

            if (options.coopMode) {
                this.openToLan(true);
            }

            if (options.autoFullscreen) {
                this.presser.pressKey(this.gameOptions.fullscreenKey);
            }
        }
        ResetHelper.getManager().notifyWorldLoaded(this);
        if (ActiveWindowManager.isWindowActive(this.hwnd)) {
            return;
        }
        this.scheduler.schedule(() -> {
            if (ActiveWindowManager.getActiveHwnd().equals(this.hwnd)) {
                return;
            }
            if (!(this.stateTracker.isCurrentState(InstanceState.INWORLD) && this.stateTracker.getInWorldType().equals(InWorldState.UNPAUSED))) {
                return;
            }
            if (options.useF3) {
                this.presser.pressF3Esc();
            } else {
                this.presser.pressEsc();
            }

        }, 1000);
    }

    private void onPreviewLoad() {
        this.scheduler.clear();
        if (JultiOptions.getInstance().useF3) {
            this.presser.pressF3Esc();
        }
        ResetHelper.getManager().notifyPreviewLoaded(this);
    }

    public boolean isResettable() {
        return (
                this.stateTracker.isResettable()
        ) && (
                System.currentTimeMillis() - this.lastSetVisible > JultiOptions.getInstance().wallResetCooldown
        );
    }

    /**
     * A method to call when an instance is shown to ensure the wallResetCooldown setting takes effect.
     */
    public void setVisible() {
        this.lastSetVisible = System.currentTimeMillis();
    }

    public boolean shouldCoverWithDirt() {
        return this.resetPressed || this.stateTracker.shouldCoverWithDirt();
    }

    public boolean isResetPressed() {
        return this.resetPressed;
    }

    @Override
    public int hashCode() {
        int result = this.hwnd.hashCode();
        result = 31 * result + this.path.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }

        MinecraftInstance that = (MinecraftInstance) o;

        return this.path.equals(that.path);
    }

    @Override
    public String toString() {
        return this.getName();
    }

    // This exists so a million wrapper methods don't have to
    public StateTracker getStateTracker() {
        return this.stateTracker;
    }

    public String getName() {
        if (this.name == null) {
            this.discoverName();
        }
        return this.name;
    }


    public boolean isFullscreen() {
        if (MCVersionUtil.isOlderThan(this.versionString, "1.16")) {
            return this.activeSinceReset && JultiOptions.getInstance().autoFullscreen && WindowStateUtil.isHwndBorderless(this.hwnd);
        } else {
            return GameOptionsUtil.tryGetBoolOption(this.getPath(), "fullscreen", false);
        }
    }

    public boolean hasWindow() {
        return !this.isWindowMarkedMissing();
    }

    private String getMMCInstanceFolderName() {
        // If the instance does not belong to MultiMC, this may be invalid
        return this.path.getName(this.path.getNameCount() - 2).toString();
    }

    public void launch(String offlineName) {
        try {
            String multiMCPath = JultiOptions.getInstance().multiMCPath;
            if (!multiMCPath.isEmpty()) {
                String cmd;
                if (offlineName == null) {
                    cmd = multiMCPath.trim() + " --launch \"" + this.getMMCInstanceFolderName() + "\"";
                } else {
                    cmd = multiMCPath.trim() + " --launch \"" + this.getMMCInstanceFolderName() + "\" -o -n " + offlineName;
                }
                Runtime.getRuntime().exec(cmd);

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void closeWindow() {
        if (User32.INSTANCE.IsWindow(this.hwnd)) {
            User32.INSTANCE.SendNotifyMessageA(this.hwnd, new WinDef.UINT(User32.WM_SYSCOMMAND), new WinDef.WPARAM(/*SC_CLOSE*/0xF060), new WinDef.LPARAM(0));
            Julti.log(Level.INFO, "Closed " + this.getName());
        } else {
            Julti.log(Level.WARN, "Could not close " + this.getName() + " because it is not open.");
        }
    }

    public void openFolder() {
        try {
            Desktop.getDesktop().browse(this.path.toUri());
        } catch (IOException e) {
            Julti.log(Level.ERROR, "Failed to open instance folder:\n" + ExceptionUtil.toDetailedString(e));
        }
    }

    /**
     * Returns a sorting number which is the sum of all groups of digit characters.
     * For example, "Multi 1.16 02" would be 1+16+2 = 19.
     *
     * @return a sorting number to correctly sort instances.
     */
    public int getNameSortingNum() {
        AtomicInteger i = new AtomicInteger(0);
        String name = this.getName();

        List<MatchResult> results = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\d+").matcher(name);
        while (matcher.find()) {
            results.add(matcher.toMatchResult());
        }


        results.forEach(matchResult -> {
            String section = name.substring(matchResult.start(), matchResult.end());
            if (section.length() == 0) {
                return;
            }
            while (section.length() > 1 && section.startsWith("0")) {
                section = section.substring(1);
            }
            i.addAndGet(Integer.parseInt(section));
        });
        return i.get();
    }

    private void ensureWindowState(boolean useBorderless, boolean maximize, Rectangle bounds, boolean offload) {
        if (!JultiOptions.getInstance().letJultiMoveWindows) {
            return;
        }

        boolean currentlyBorderless = WindowStateUtil.isHwndBorderless(this.hwnd);
        boolean currentlyMaximized = WindowStateUtil.isHwndMaximized(this.hwnd);
        Rectangle currentBounds = WindowStateUtil.getHwndRectangle(this.hwnd);

        if (useBorderless && !currentlyBorderless) {
            WindowStateUtil.setHwndBorderless(this.hwnd);
        } else if (currentlyBorderless && !useBorderless) {
            WindowStateUtil.undoHwndBorderless(this.hwnd);
        }

        if (currentlyMaximized && !maximize) {
            WindowStateUtil.restoreHwnd(this.hwnd);
        } else if (maximize && !currentlyMaximized) {
            WindowStateUtil.maximizeHwnd(this.hwnd);
        }

        // If we are maximizing, there is no point trying to set size/position
        if (maximize) {
            return;
        }

        if (!currentBounds.equals(bounds)) {
            if (offload) {
                WindowStateUtil.queueSetHwndRectangle(this.hwnd, bounds);
            } else {
                WindowStateUtil.setHwndRectangle(this.hwnd, bounds);
            }
        }
    }

    public void ensureResettingWindowState(boolean offload) {
        JultiOptions options = JultiOptions.getInstance();
        this.ensureWindowState(
                options.useBorderless,
                // maximize if
                !options.useBorderless && // We aren't using borderless and
                        (!options.autoFullscreen || options.usePlayingSizeWithFullscreen) && // We aren't using fullscreen (except if we override it) and
                        options.resettingWindowSize == options.playingWindowSize, // there's a difference in the window sizes in the first place
                new Rectangle(options.windowPos[0], options.windowPos[1], options.resettingWindowSize[0], options.resettingWindowSize[1]),
                offload);
    }

    public void ensurePlayingWindowState(boolean offload) {
        JultiOptions options = JultiOptions.getInstance();
        this.ensureWindowState(
                options.useBorderless,
                (!options.useBorderless) && (!options.autoFullscreen || options.useMaximizeWithFullscreen),
                new Rectangle(options.windowPos[0], options.windowPos[1], options.playingWindowSize[0], options.playingWindowSize[1]),
                offload);
        this.windowStateChangedToPlaying = true;
    }

    public void openToLan(boolean skipUnpauseCheck) {
        if (this.openedToLan) {
            return;
        } else if (!this.stateTracker.isCurrentState(InstanceState.INWORLD)) {
            return;
        } else if (WindowTitleUtil.getHwndTitle(this.hwnd).endsWith("(LAN)")) {
            this.openedToLan = true;
            return;
        }

        if (!skipUnpauseCheck) {
            this.getToUnpausedState();
        }

        this.presser.releaseAllModifiers();
        this.presser.pressEsc();
        this.presser.pressTab(7);
        this.presser.pressEnter();
        this.presser.pressShiftTab(1);
        if (MCVersionUtil.isNewerThan(this.versionString, "1.16.5")) {
            this.presser.pressTab(2);
        }
        this.presser.pressEnter();
        this.presser.pressTab();
        this.presser.pressEnter();
        this.openedToLan = true;
    }

    public void sendChatMessage(String chatMessage, boolean skipUnpauseCheck) {
        if (!skipUnpauseCheck) {
            this.getToUnpausedState();
        }
        this.presser.pressKey(this.gameOptions.chatKey);
        sleep(100);
        for (char c : chatMessage.toCharArray()) {
            KeyboardUtil.sendCharToHwnd(this.hwnd, c);
        }
        this.presser.pressEnter();
    }

    private void getToUnpausedState() {
        sleep(50);
        this.getStateTracker().tryUpdate();
        while (!this.stateTracker.getInWorldType().equals(InWorldState.UNPAUSED)) {
            this.presser.pressEsc();
            sleep(50);
            if (!this.getStateTracker().tryUpdate()) {
                Julti.log(Level.ERROR, "Failed to update state for instance " + this.getName() + "!");
            }
        }
    }

    public void ensureNotFullscreen() {
        if ((!this.activeSinceReset) || (!this.isFullscreen())) {
            Julti.log(Level.DEBUG, "Skipping fullscreen check because " + (!this.activeSinceReset ? "the instance was not active" : "the instance is not in fullscreen"));
            return;
        }

        Julti.log(Level.DEBUG, "Pressing fullscreen key...");
        this.presser.pressKey(this.gameOptions.fullscreenKey);

        Julti.log(Level.DEBUG, "Waiting for fullscreen option to turn false...");
        do {
            sleep(5);
        } while (this.isFullscreen());

        int i = 0;

        Julti.log(Level.DEBUG, "Waiting for window border to reappear...");
        // Fullscreened MC windows are naturally borderless, and using isHwndBorderless works for checking this
        while (WindowStateUtil.isHwndBorderless(this.hwnd) && (i++ < 50)) {
            sleep(5);
        }
        Julti.log(Level.DEBUG, "ensureNotFullscreen complete (" + i + ")");
    }

    public void logAndCopyInfo() {

        final StringBuilder toCopy = new StringBuilder();

        Consumer<String> consumer = s -> {
            Julti.log(Level.INFO, s);
            if (!toCopy.toString().isEmpty()) {
                toCopy.append("\n");
            }
            toCopy.append(s);
        };

        Julti.log(Level.INFO, "");
        Julti.log(Level.INFO, "");
        consumer.accept("Info output for " + this);
        consumer.accept("hwnd = " + this.hwnd);
        consumer.accept("pid = " + this.pid);
        consumer.accept("path = " + this.path);
        consumer.accept("versionString = " + this.versionString);
        consumer.accept("gameOptions = " + this.gameOptions);
        consumer.accept("name = " + this.name);
        consumer.accept("stateTracker = " + this.stateTracker);
        consumer.accept("windowMissing = " + this.windowMissing);
        consumer.accept("resetPressed = " + this.resetPressed);
        consumer.accept("resetEverPressed = " + this.resetEverPressed);
        consumer.accept("activeSinceReset = " + this.activeSinceReset);
        consumer.accept("windowStateChangedToPlaying = " + this.windowStateChangedToPlaying);
        consumer.accept("lastSetVisible = " + this.lastSetVisible);
        consumer.accept("openedToLan = " + this.openedToLan);
        consumer.accept("isResettable() = " + this.isResettable());
        Julti.log(Level.INFO, "");
        Julti.log(Level.INFO, "");

        KeyboardUtil.copyToClipboard(toCopy.toString());
    }
}
