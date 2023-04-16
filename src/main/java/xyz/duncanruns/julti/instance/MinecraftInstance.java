package xyz.duncanruns.julti.instance;

import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.HWND;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.instance.InstanceState.InWorldState;
import xyz.duncanruns.julti.management.ActiveWindowManager;
import xyz.duncanruns.julti.resetting.ResetHelper;
import xyz.duncanruns.julti.util.*;
import xyz.duncanruns.julti.win32.User32;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
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
    private final GameOptions gameOptions;
    private String name = null;

    private final StateTracker stateTracker;
    private final KeyPresser presser;

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
        this.gameOptions = new GameOptions();
        this.stateTracker = new StateTracker(path.resolve("wpstateout.txt"), this::onStateChange);
        this.presser = new KeyPresser(hwnd);
    }

    public MinecraftInstance(Path path) {
        this.hwnd = null;
        this.versionString = null;
        this.presser = null;
        this.gameOptions = null;
        this.stateTracker = new StateTracker(path.resolve("wpstateout.txt"), null);

        this.path = path;
        this.windowMissing = true;
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
    }

    public void discoverInformation() {
        // Find info like keybinds, standard settings, etc.

        // Process ID
        this.pid = PidUtil.getPidFromHwnd(this.hwnd);

        // Keybinds
        this.gameOptions.createWorldKey = GameOptionsUtil.getKey(this.getPath(), "key_Create New World");
        this.gameOptions.leavePreviewKey = GameOptionsUtil.getKey(this.getPath(), "key_Leave Preview");
        this.gameOptions.fullscreenKey = GameOptionsUtil.getKey(this.getPath(), "key_key.fullscreen");
        this.gameOptions.chatKey = GameOptionsUtil.getKey(this.getPath(), "key_key.chat");

        this.discoverName();
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
        boolean wasFullscreen = false;
        Rectangle ogRect = null;
        if (this.activeSinceReset && this.isFullscreen()) {
            wasFullscreen = true;
            ogRect = WindowStateUtil.getHwndRectangle(this.hwnd);
        }

        if (this.stateTracker.isCurrentState(InstanceState.TITLE)) {
            this.presser.pressShiftTabEnter();
            this.presser.pressKey(this.gameOptions.createWorldKey);
        } else {
            this.presser.pressKey(this.gameOptions.leavePreviewKey);
            this.presser.pressKey(this.gameOptions.createWorldKey);
        }
        this.resetPressed = true;
        this.resetEverPressed = true;
        this.openedToLan = false;

        if (wasFullscreen) {
            // Wait until window actually un-fullscreens
            // Or until 2 ish seconds have passed
            for (int i = 0; i < 200; i++) {
                if (!Objects.equals(ogRect, WindowStateUtil.getHwndRectangle(this.hwnd))) {
                    break;
                }
                sleep(10);
            }
        }

        this.activeSinceReset = false;
        if (this.windowStateChangedToPlaying) {
            // Do the window state change later to ensure resetting is fast
            Julti.doLater(this::ensureResettingWindowState);
        }
    }

    public void activate(boolean doingSetup) {
        if (this.isWindowMarkedMissing()) {
            return;
        }
        this.activeSinceReset = true;
        ActiveWindowManager.activateHwnd(this.hwnd);
        this.stateTracker.tryUpdate();
        if (this.stateTracker.isCurrentState(InstanceState.INWORLD)) {
            if (!doingSetup) {
                JultiOptions options = JultiOptions.getInstance();
                if ((options.unpauseOnSwitch || options.coopMode) && this.stateTracker.getInWorldType() == InWorldState.PAUSED) {
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
        if (doingSetup) {
            this.ensureResettingWindowState();
        } else {
            this.ensurePlayingWindowState();
        }
    }

    private void onStateChange() {
        // The next state after reset should be WAITING, if it changes to something that is not WAITING, send the key again
        if (this.resetPressed) {
            if (this.stateTracker.isCurrentState(InstanceState.WAITING)) {
                this.resetPressed = false;
            } else {
                this.reset();
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
            case GENERATING:
                ResetCounter.increment();
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
        //Julti.log(Level.INFO, this.getName() + "'s world loaded.");

        JultiOptions options = JultiOptions.getInstance();

        if (!bypassPieChartGate && options.pieChartOnLoad) {
            // Unpause
            if (this.stateTracker.getInWorldType() == InWorldState.PAUSED) {
                this.presser.pressEsc();
            }
            // Open pie chart
            this.presser.pressShiftF3();

            // Schedule the scheduling of the completion of onWorldLoad
            new Timer("delayed-world-load-scheduler").schedule(new TimerTask() {
                @Override
                public void run() {
                    Julti.doLater(() -> MinecraftInstance.this.onWorldLoad(true));
                }
            }, 150);

            return;
        }

        // Pause the game
        if (this.stateTracker.getInWorldType() == InWorldState.PAUSED) {
            // Game is paused already

            if (options.useF3) {
                // F3
                // Unpause and re-pause with f3+esc
                this.presser.pressEsc();
                this.presser.pressF3Esc();
            }
            // No F3 does not need to be handled since the game is already paused correctly

        } else {
            // Game is not yet paused

            if (options.useF3) {
                // F3
                this.presser.pressF3Esc();
            } else {
                // No F3
                this.presser.pressEsc();
            }
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
    }

    private void onPreviewLoad() {
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
        return Objects.equals(GameOptionsUtil.tryGetOption(this.getPath(), "fullscreen", false), "true");
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
        } catch (IOException ignored) {

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

    private void ensureWindowState(boolean useBorderless, boolean maximize, Rectangle bounds) {

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
            WindowStateUtil.setHwndRectangle(this.hwnd, bounds);
        }
    }

    public void ensureResettingWindowState() {
        JultiOptions options = JultiOptions.getInstance();
        this.ensureWindowState(
                options.useBorderless,
                !options.useBorderless && !(options.autoFullscreen && !options.usePlayingSizeWithFullscreen) && options.resettingWindowSize == options.playingWindowSize,
                new Rectangle(options.windowPos[0], options.windowPos[1], options.resettingWindowSize[0], options.resettingWindowSize[1])
        );
    }

    public void ensurePlayingWindowState() {
        JultiOptions options = JultiOptions.getInstance();
        if (options.autoFullscreen && !options.usePlayingSizeWithFullscreen) {
            return;
        }
        this.ensureWindowState(
                options.useBorderless,
                !options.useBorderless,
                new Rectangle(options.windowPos[0], options.windowPos[1], options.playingWindowSize[0], options.playingWindowSize[1])
        );
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
}
