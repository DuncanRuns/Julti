package xyz.duncanruns.julti.instance;

import com.sun.jna.platform.win32.WinDef.HWND;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.ResetCounter;
import xyz.duncanruns.julti.util.*;
import xyz.duncanruns.julti.win32.Win32Con;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static xyz.duncanruns.julti.util.SleepUtil.sleep;

public class MinecraftInstance {

    private static final Logger LOGGER = LogManager.getLogger("MinecraftInstance");
    private static final Pattern startPreviewPattern = Pattern.compile("^\\[\\d\\d:\\d\\d:\\d\\d] \\[.*/INFO]:  ?Starting Preview at \\(-?\\d+\\.\\d+, -?\\d+\\.\\d+, -?\\d+\\.\\d+\\)$");
    private static final Pattern startPreviewWithBiomePattern = Pattern.compile("^\\[\\d\\d:\\d\\d:\\d\\d] \\[.*/INFO]:  ?Starting Preview at \\(-?\\d+(\\.\\d+)?, -?\\d+(\\.\\d+)?, -?\\d+(\\.\\d+)?\\) in biome .+$");
    private static final Pattern spawnAreaPattern = Pattern.compile("^\\[\\d\\d:\\d\\d:\\d\\d] \\[.*/INFO]: .+: \\d+ ?%$");
    private static final Pattern advancementsLoadedPattern = Pattern.compile("^\\[\\d\\d:\\d\\d:\\d\\d] \\[.+/INFO]: Loaded 1?\\d advancements$");
    private static final Pattern openToLanPattern = Pattern.compile("^\\[\\d\\d:\\d\\d:\\d\\d] \\[.*/INFO]: Started serving on \\d+$");

    // Basic instance information
    private final WindowTitleInfo titleInfo;
    private HWND hwnd;
    private Path instancePath = null;
    private String name = null;

    // Missing Window Stuff
    private boolean notMC = false; // true when a MinecraftInstance is constructed with a window handle which points to a non-mc window
    private boolean missingReported = false;
    private boolean replaced = false;

    // Information to be discovered
    private ResetType resetType = null;
    private Integer createWorldKey = null;
    private Integer fullscreenKey = null;
    private Integer leavePreviewKey = null;
    private Boolean usingWorldPreview = null;
    private Boolean usingStandardSettings = null;
    private byte f1SS = -2; // -2 = undetermined, -1 = not present in SS, 0 = false in SS, 1 = true in SS

    // State tracking
    private boolean inPreview = false;
    private boolean worldLoaded = false;
    private long timeLastAppeared = -1L;
    private long lastPreviewStart = -1L;
    private long lastResetPress = -1L;
    private String biome = "";
    private int loadingPercent = 0;
    private boolean dirtCover = false;
    private boolean available = false;
    private boolean worldEverLoaded = false;
    private boolean shouldPressDelayedWLKeys = false; // "Should press delayed world load keys"
    private boolean activeSinceLastReset = false;
    private boolean openedToLan = false;
    private boolean firstActivate = true;

    // Log tracking
    private long logProgress = -1;
    private FileTime lastLogModify = null;
    private Integer pid = null;

    public MinecraftInstance(Path instancePath) {
        this.hwnd = null;
        this.titleInfo = new WindowTitleInfo();
        this.instancePath = instancePath;
        this.notMC = false;
    }

    public MinecraftInstance(HWND hwnd) {
        this.hwnd = hwnd;
        this.titleInfo = new WindowTitleInfo(this.getCurrentWindowTitle());
    }

    private String getCurrentWindowTitle() {
        if (!this.hasWindow()) {
            return "Missing Window";
        }
        return HwndUtil.getHwndTitle(this.hwnd);
    }

    public boolean hasWindow() {
        if (this.hwnd != null && HwndUtil.hwndExists(this.hwnd)) {
            return true;
        } else {
            this.hwnd = null;
            return false;
        }
    }

    private static String getOptionFromString(String optionName, String optionsString) {
        String[] lines = optionsString.trim().split("\\n");
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i];
            if (line.startsWith(optionName + ":")) {
                String[] optionKeyValArr = line.split(":");
                if (optionKeyValArr.length < 2) {
                    continue;
                }
                return optionKeyValArr[1];
            }
        }
        return null;
    }

    public boolean hasWindowOrBeenReplaced() {
        return this.hasWindow() || this.hasBeenReplaced();
    }

    public boolean hasBeenReplaced() {
        return this.replaced;
    }

    public void markReplaced() {
        this.replaced = true;
    }

    public boolean isUsingF1() {
        if (JultiOptions.getInstance().pieChartOnLoad) {
            return false;
        }

        // Stupid compact logic, probably don't touch
        if (this.f1SS != -2) {
            return this.f1SS != -1;
        }
        String out = this.tryGetStandardOption("f1");
        if (out == null) {
            this.f1SS = -1;
        } else if (out.equals("true")) {
            this.f1SS = 1;
        } else {
            this.f1SS = 0;
        }
        return this.f1SS != -1;
    }

    public void ensureGoodStandardSettings() {
        this.getInstancePath();
        if (!this.isUsingStandardSettings()) {
            return;
        }

        String[] goodSettings = new String[]{
                "changeOnResize:true",
                "fullscreen:false",
                "pauseOnLostFocus:false",
                "key_Cycle ChunkMap Positions:key.keyboard.unknown"
        };

        for (String setting : goodSettings) {

            String[] settingVals = setting.split(":");
            String optionName = settingVals[0];
            String desiredValue = settingVals[1];
            String currentValue = this.tryGetStandardOption(optionName);

            if (desiredValue.equals(currentValue)) {
                continue;
            }

            this.forceStandardSetting(optionName, desiredValue);
            log(Level.INFO, "Set \"" + optionName + "\" to \"" + desiredValue + "\" in standard settings for " + this.getName());
        }

        if (Objects.equals(this.getStandardOption("f1"), null)) {
            this.forceStandardSetting("f1", "false");
            log(Level.INFO, "Set \"f1\" to \"false\" in standard settings for " + this.getName());
        }
    }

    private void forceStandardSetting(String optionName, String optionValue) {
        Path path = this.getInstancePath().resolve("config").resolve("standardoptions.txt");
        while (true) {
            try {
                String contents = FileUtil.readString(path).trim();
                if (!contents.endsWith(".txt")) {
                    break;
                }
                Path deeperPath = Paths.get(contents);
                if (!Files.exists(deeperPath)) {
                    break;
                }
                path = deeperPath;
            } catch (IOException ignored) {
                break;
            }
        }
        List<String> currentLines;
        try {
            currentLines = Files.readAllLines(path);
        } catch (IOException ignored) {
            return;
        }

        StringBuilder out = new StringBuilder();

        for (String currentLine : currentLines) {
            if (!currentLine.startsWith(optionName + ":")) {
                out.append("\n").append(currentLine);
            }
        }
        out.append("\n").append(optionName).append(":").append(optionValue);

        try {
            FileUtil.writeString(path, out.toString().trim());
        } catch (IOException ignored) {
        }

    }

    public boolean isFullscreen() {
        return Objects.equals(this.tryGetOption("fullscreen", false), "true");
    }

    public String tryGetOption(String optionName, boolean tryUseSS) {

        // This should prevent any crazy out of pocket bullshits like 1 in a million parsing error situations
        try {
            return this.getOption(optionName, tryUseSS);
        } catch (Exception ignored) {
            return null;
        }
    }

    public String getOption(String optionName, boolean tryUseSS) {
        if (tryUseSS) {
            String out = this.tryGetStandardOption(optionName);
            if (out != null) {
                return out;
            }
        }

        Path path = this.getInstancePath().resolve("options.txt");

        if (!Files.exists(path)) {
            return null;
        }

        String out;
        try {
            out = FileUtil.readString(path);
        } catch (IOException e) {
            // This should never be reached
            return null;
        }

        return getOptionFromString(optionName, out).trim();
    }

    private String tryGetStandardOption(String optionName) {
        try {
            return this.getStandardOption(optionName);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String getStandardOption(String optionName) {
        String out = this.getStandardOption(optionName, this.getInstancePath().resolve("config").resolve("standardoptions.txt"));
        if (out != null) {
            return out.trim();
        }
        return null;
    }

    /**
     * Determines the gui scale that actually gets used during resets on this instance.
     */
    public int getResettingGuiScale(int resettingWidth, int resettingHeight) {
        // Get values
        int guiScale = 0;
        try {
            guiScale = Integer.parseInt(this.tryGetOption("guiScale", true));
        } catch (NumberFormatException ignored) {
        }
        boolean forceUnicodeFont = Objects.equals(this.tryGetOption("forceUnicodeFont", true), "true");

        // Minecraft code magic
        int i = 1;
        while ((i != guiScale)
                && (i < resettingWidth)
                && (i < resettingHeight)
                && (resettingWidth / (i + 1) >= 320)
                && (resettingHeight / (i + 1) >= 240)) {
            ++i;
        }
        if (forceUnicodeFont && i % 2 != 0) {
            ++i;
        }

        return i;
    }

    private String getStandardOption(String optionName, Path path) {
        if (!Files.exists(path)) {
            return null;
        }

        String out;
        try {
            out = FileUtil.readString(path).trim();
        } catch (IOException e) {
            // This should never be reached
            return null;
        }

        if (!out.contains("\n")) {
            if (out.endsWith(".txt")) {
                return this.getStandardOption(optionName, Paths.get(out));
            }
        }

        return getOptionFromString(optionName, out);
    }

    public boolean hasWindowQuick() {
        return this.hwnd != null;
    }

    public long getLastPreviewStart() {
        return this.lastPreviewStart;
    }

    public long getTimeLastAppeared() {
        return this.timeLastAppeared;
    }

    /**
     * Updates the timeLastAppeared field to the current time. Normally would be based on dirt covers, but can be set by a third party such as dynamic wall.
     */
    public void updateTimeLastAppeared() {
        this.timeLastAppeared = System.currentTimeMillis();
    }

    public String getOriginalTitle() {
        if (this.titleInfo.waiting()) {
            this.titleInfo.provide(HwndUtil.getHwndTitle(this.hwnd));
        }
        return this.titleInfo.getOriginalTitle();
    }

    public boolean isActuallyMC() {
        this.getInstancePath();
        return !this.notMC;
    }

    public Path getInstancePath() {
        if (this.instancePath != null) {
            return this.instancePath;
        }

        if (this.notMC || !this.hasWindow()) {
            return null;
        }

        this.instancePath = HwndUtil.getInstancePathFromPid(this.getPid());
        if (this.instancePath == null) {
            this.notMC = true;
        }

        return this.instancePath;
    }

    public int getPid() {
        if (this.pid == null) {
            this.pid = HwndUtil.getPidFromHwnd(this.hwnd);
            return this.pid;
        }
        return this.pid;
    }

    public HWND getHwnd() {
        // Note: if hwnd == null, the instance is unusable. The proper way to manage this is to replace the object with a new one.
        return this.hwnd;
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

    public String getName() {
        // Return existing name
        if (this.name != null) {
            return this.name;
        }

        // Get instance path
        Path instancePath = this.getInstancePath();
        if (instancePath == null) {
            return "Unknown Instance"; //This name should probably never be seen, regardless it is here.
        }

        // Check MultiMC/Prism name

        Path mmcConfigPath = instancePath.getParent().resolve("instance.cfg");
        if (Files.exists(mmcConfigPath)) {
            try {
                for (String line : Files.readAllLines(mmcConfigPath)) {
                    line = line.trim();
                    if (line.startsWith("name=")) {
                        this.name = StringEscapeUtils.unescapeJson(line.split("=")[1]);
                        return this.name;
                    }
                }
            } catch (Exception ignored) {
                // Fail, continue to get folder name instead
            }
        }

        if (instancePath.getName(instancePath.getNameCount() - 1).toString().equals(".minecraft")) {
            instancePath = instancePath.getParent();
            // If this runs, instancePath is no longer an accurate variable name, and describes the parent path
        }
        this.name = instancePath.getName(instancePath.getNameCount() - 1).toString();
        if (this.name.equals("Roaming")) {
            return "Default Launcher";
        }
        return this.name;
    }

    public int getWallSortingNum() {
        int i = 0;
        if (this.isPreviewLoaded()) {
            i += 1280000;
        }
        if (this.isWorldLoaded()) {
            i += 2560000;
        }
        i += 10000 * Math.max(0, this.getLoadingPercent());
        i += (System.currentTimeMillis() - this.getLastResetPress());
        return i;
    }

    public boolean isPreviewLoaded() {
        return this.inPreview;
    }

    public boolean isWorldLoaded() {
        return this.worldLoaded;
    }

    public int getLoadingPercent() {
        return this.loadingPercent;
    }

    public long getLastResetPress() {
        return this.lastResetPress;
    }

    public boolean justWentMissing() {
        if (!this.hasWindow() && !this.missingReported) {
            this.missingReported = true;
            return true;
        }
        return false;
    }

    public void pressFullscreenKey() {
        KeyboardUtil.sendKeyToHwnd(this.hwnd, this.getFullscreenKey());
    }

    public Integer getFullscreenKey() {
        if (this.fullscreenKey == null) {
            this.fullscreenKey = this.getKey("key_key.fullscreen");
        }
        return this.fullscreenKey;
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

        if (this.hwnd == null && that.hwnd == null) {
            return false;
        }

        return Objects.equals(this.hwnd, that.hwnd);
    }

    @Override
    public String toString() {
        return "Instance \"" + this.getName() + "\"";
    }

    /**
     * @param instanceNum -1 for not updating title, otherwise the real instance number (index + 1).
     */
    synchronized public void activate(int instanceNum) {
        JultiOptions options = JultiOptions.getInstance();
        this.activeSinceLastReset = true;
        if (this.hasWindow()) {
            if (!this.firstActivate) {
                new Thread(() -> this.ensureWindowState(false, false), "window-resizer").start();
            }
            HwndUtil.showHwnd(this.hwnd);
            HwndUtil.activateHwnd(this.hwnd);
            if (this.firstActivate) {
                this.firstActivate = false;
                this.clickTopLeftCorner();
            }
            if (this.worldLoaded) {
                new Thread(() -> {
                    int i = 100;
                    while (!this.isActive() && i > 0) {
                        sleep(10);
                        i--;
                    }
                    if (options.unpauseOnSwitch) {
                        this.pressEsc();
                        if (options.wideResetSquish > 1.0) {
                            // 2 Extra Escape Presses to make sure mouse is centered on next menu open
                            this.pressEsc();
                            this.pressEsc();
                        }
                    }
                    if (options.coopMode) {
                        this.openToLan(!options.unpauseOnSwitch);
                    }
                    if (this.shouldDoCleanWall()) {
                        this.pressF1();
                    }
                    if (options.autoFullscreen) {
                        this.pressFullscreenKey();
                    }
                    if (instanceNum != -1) {
                        this.setWindowTitle("Minecraft* - Instance " + instanceNum);
                    }
                }, "instance-activate-finisher").start();
            }
            if (instanceNum != -1) {
                this.setWindowTitle("Minecraft* - Instance " + instanceNum);
            }
            log(Level.INFO, "Activated instance " + this.getName());
        } else {
            log(Level.WARN, "Could not activate instance " + this.getName() + " (not opened)");
        }
    }

    private void clickTopLeftCorner() {
        MouseUtil.clickTopLeft(this.hwnd);
    }

    private boolean shouldDoCleanWall() {
        JultiOptions options = JultiOptions.getInstance();
        return options.resetMode != 0 && this.isUsingF1() && options.unpauseOnSwitch && options.cleanWall;
    }

    public void ensureWindowState() {
        this.ensureWindowState(false, true);
    }

    /**
     * Ensure window state resizes the window and sets its maximized/borderless state depending on Julti options and parameters
     *
     * @param force         skips any checks to see if the window state is already in an ideal state
     * @param allowSquished allows squished instances (where height = the set height divided by the squish level) as an ideal state
     */
    public void ensureWindowState(boolean force, boolean allowSquished) {
        JultiOptions options = JultiOptions.getInstance();

        // "Do nothing" conditions
        if (!options.letJultiMoveWindows) {
            return;
        }
        Rectangle rectangle = this.getWindowRectangle();

        boolean heightMatches = options.windowSize[1] == rectangle.height;
        boolean squishMatches = false;
        if (!force && !heightMatches && allowSquished) {
            squishMatches = options.windowSize[1] / options.wideResetSquish == rectangle.height;
        }
        if (!force && options.windowPos[0] == rectangle.x &&
                options.windowPos[1] == rectangle.y &&
                options.windowSize[0] == rectangle.width &&
                (heightMatches || squishMatches) &&
                options.useBorderless == this.isBorderless() &&
                (options.useBorderless || squishMatches || this.isMaximized())
        ) {
            return;
        }

        if (options.useBorderless) {
            this.setBorderless();
        } else {
            this.undoBorderless();
        }


        if (!options.useBorderless) {
            this.maximize();
        } else {
            this.restore();
            this.move(options.windowPos[0], options.windowPos[1], options.windowSize[0], options.windowSize[1]);
        }

    }

    public Rectangle getWindowRectangle() {
        return HwndUtil.getHwndRectangle(this.hwnd);
    }

    public boolean isBorderless() {
        return HwndUtil.isHwndBorderless(this.hwnd);
    }

    public boolean isMaximized() {
        return HwndUtil.isHwndMaximized(this.hwnd);
    }

    public void setBorderless() {
        HwndUtil.setHwndBorderless(this.hwnd);
    }

    public void undoBorderless() {
        HwndUtil.undoHwndBorderless(this.hwnd);
    }

    public void maximize() {
        HwndUtil.maximizeHwnd(this.hwnd);
    }

    public void restore() {
        HwndUtil.restoreHwnd(this.hwnd);
    }

    public void move(int x, int y, int w, int h) {
        HwndUtil.moveHwnd(this.hwnd, x, y, w, h);
    }

    private boolean isActive() {
        return Objects.equals(HwndUtil.getCurrentHwnd(), this.hwnd);
    }

    private void pressEsc() {
        KeyboardUtil.sendKeyToHwnd(this.hwnd, Win32Con.VK_ESCAPE);
    }

    public void openToLan(boolean alreadyInMenu) {
        if (this.openedToLan) {
            return;
        }
        KeyboardUtil.releaseAllModifiers();
        if (!alreadyInMenu) {
            this.pressEsc();
        }
        this.pressTab(7);
        this.pressEnter();
        this.pressShiftTab(1);
        this.pressEnter();
        this.pressTab(1);
        this.pressEnter();

    }

    public void setWindowTitle(String title) {
        if (this.hasWindow() && !JultiOptions.getInstance().preventWindowNaming) {
            HwndUtil.setHwndTitle(this.hwnd, title);
        }
    }

    private void pressTab(int tabTimes) {
        for (int i = 0; i < tabTimes; i++) {
            KeyboardUtil.sendKeyToHwnd(this.hwnd, Win32Con.VK_TAB);
        }
    }

    private void pressShiftTab(int tabTimes) {
        KeyboardUtil.sendKeyDownToHwnd(this.hwnd, Win32Con.VK_LSHIFT, true);
        this.pressTab(tabTimes);
        KeyboardUtil.sendKeyUpToHwnd(this.hwnd, Win32Con.VK_LSHIFT, true);
    }

    public boolean isUsingStandardSettings() {
        if (this.usingStandardSettings != null) {
            return this.usingStandardSettings;
        }

        boolean exists = this.doesModExist("standardsettings");
        this.usingStandardSettings = exists;
        return exists;
    }

    private void pressF1() {
        KeyboardUtil.sendKeyToHwnd(this.hwnd, Win32Con.VK_F1);
    }

    private boolean doesModExist(String modName) {
        Path modsPath = this.getInstancePath().resolve("mods");
        try (Stream<Path> list = Files.list(modsPath)) {
            for (Path modPath : list.collect(Collectors.toList())) {
                String jarName = modPath.getName(modPath.getNameCount() - 1).toString();
                if (jarName.startsWith(modName) && jarName.endsWith(".jar")) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public void squish(float squish) {
        if (squish == 1f) {
            return;
        }

        JultiOptions options = JultiOptions.getInstance();
        Rectangle resultRectangle = new Rectangle(options.windowPos[0], options.windowPos[1], options.windowSize[0], (int) (options.windowSize[1] / squish));
        if (this.isMaximized()) {
            this.restore();
        } else {
            if (this.getWindowRectangle().equals(resultRectangle)) {
                return;
            }
        }
        this.move(resultRectangle.x, resultRectangle.y, resultRectangle.width, resultRectangle.height);
    }

    public void closeWindow() {
        if (this.hasWindow()) {
            HwndUtil.sendCloseMessage(this.hwnd);
            log(Level.INFO, "Closed " + this.getName());
        } else {
            log(Level.WARN, "Could not close " + this.getName() + " because it is not open.");
        }
    }

    public static void log(Level level, String message) {
        LOGGER.log(level, message);
        LogReceiver.receive(level, message);
    }

    public void launch(String offlineName) {
        try {
            String multiMCPath = JultiOptions.getInstance().multiMCPath;
            if (!multiMCPath.isEmpty()) {
                String cmd;
                if (offlineName == null) {
                    cmd = multiMCPath.trim() + " --launch \"" + this.getInstanceFolderName() + "\"";
                } else {
                    cmd = multiMCPath.trim() + " --launch \"" + this.getInstanceFolderName() + "\" -o -n " + offlineName;
                }
                Runtime.getRuntime().exec(cmd);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getInstanceFolderName() {
        return this.getInstancePath().getName(this.getInstancePath().getNameCount() - 2).toString();
    }

    private void pressF3Esc() {
        KeyboardUtil.sendKeyDownToHwnd(this.hwnd, Win32Con.VK_F3, true);
        KeyboardUtil.sendKeyToHwnd(this.hwnd, Win32Con.VK_ESCAPE);
        KeyboardUtil.sendKeyUpToHwnd(this.hwnd, Win32Con.VK_F3, true);
    }

    private void pressF3() {
        KeyboardUtil.sendKeyToHwnd(this.hwnd, Win32Con.VK_F3);
    }

    synchronized public void reset(boolean singleInstance) {
        // If no window, do nothing
        if (!this.hasWindow()) {
            log(Level.INFO, "Could not reset instance " + this.getName() + " (not opened)");
            return;
        }

        // Before taking any action, store some info useful for fullscreen management
        boolean wasFullscreen = false;
        if (this.activeSinceLastReset) {
            wasFullscreen = this.isFullscreen();
        }
        Rectangle ogRect = null;
        if (wasFullscreen) {
            ogRect = this.getWindowRectangle();
        }

        // This delay is only used for pressing keys before the reset key
        boolean shouldDelay = false;
        if (this.isWorldLoaded() && this.activeSinceLastReset) {
            if (wasFullscreen) {
                this.pressFullscreenKey();
                shouldDelay = true;
            }
            if (this.isUsingF1()) {
                this.pressF1();
                shouldDelay = true;
            }
        }

        if (shouldDelay) {
            Rectangle finalOgRect = ogRect;
            boolean finalWasFullscreen = wasFullscreen;
            new Timer("reset-finisher").schedule(new TimerTask() {
                @Override
                public void run() {
                    MinecraftInstance.this.finishReset(singleInstance, finalWasFullscreen, finalOgRect);
                }
            }, 50);
        } else {
            this.finishReset(singleInstance, wasFullscreen, ogRect);
        }
    }

    private void finishReset(boolean singleInstance, boolean wasFullscreen, Rectangle ogRect) {
        JultiOptions options = JultiOptions.getInstance();

        this.pressResetKeys();

        //Update states
        this.worldLoaded = false;
        this.loadingPercent = -1;
        this.setInPreview(false);
        this.dirtCover = options.dirtReleasePercent >= 0;
        this.available = false;
        this.shouldPressDelayedWLKeys = false;
        this.activeSinceLastReset = false;

        if (wasFullscreen) {
            // Wait until window actually un-fullscreens
            // Or until 2 ish seconds have passed
            for (int i = 0; i < 200; i++) {
                if (!Objects.equals(ogRect, this.getWindowRectangle())) {
                    break;
                }
                sleep(10);
            }
        }

        // Window Resizing and Shid
        new Timer("delayed-window-fixer").schedule(new TimerTask() {
            @Override
            public void run() {
                if (!options.letJultiMoveWindows) {
                    return;
                }
                if (wasFullscreen && options.useBorderless) {
                    MinecraftInstance.this.setBorderless();
                }
                if (!singleInstance && options.letJultiMoveWindows) {
                    MinecraftInstance.this.squish(options.wideResetSquish);
                }
            }
        }, 50);

        // Log and reset counter update
        log(Level.INFO, "Reset instance " + this.getName());
        ResetCounter.increment();
    }

    private void pressResetKeys() {
        this.lastResetPress = System.currentTimeMillis();
        switch (this.getResetType()) {
            case MODERN_ATUM_EXIT:
                if (this.leavePreviewKey != null) {
                    KeyboardUtil.sendKeyToHwnd(this.hwnd, this.leavePreviewKey);
                }
                KeyboardUtil.sendKeyToHwnd(this.hwnd, this.getCreateWorldKey());
                break;
            case LEAVE_PREVIEW_EXIT:
                if (this.inPreview) {
                    KeyboardUtil.sendKeyToHwnd(this.hwnd, this.getLeavePreviewKey());
                } else {
                    this.runNoAtumLeave();
                }
                break;
            case VANILLA_EXIT:
                this.runNoAtumLeave();
        }
    }

    private void runNoAtumLeave() {
        WindowTitleInfo.Version version = this.titleInfo.getVersion();

        KeyboardUtil.sendKeyToHwnd(this.hwnd, Win32Con.VK_ESCAPE);
        if (version.getMajor() > 12) {
            this.pressShiftTab(1);
        } else if (version.getMajor() == 8 && version.getMinor() == 9) {
            sleep(70); // Magic Number
            // Anchiale Support
            for (int i = 0; i < 7; i++) {
                this.pressTab(1);
            }
        } else {
            sleep(70); // Magic Number
            this.pressTab(1);
        }
        this.pressEnter();
    }

    public boolean isUsingWorldPreview() {
        if (this.usingWorldPreview != null) {
            return this.usingWorldPreview;
        }

        boolean exists = this.doesModExist("worldpreview");
        this.usingWorldPreview = exists;
        return exists;
    }

    private Integer getLeavePreviewKey() {
        if (this.leavePreviewKey == null) {
            this.leavePreviewKey = this.getKey("key_Leave Preview");
        }
        return this.leavePreviewKey;
    }

    public Integer getCreateWorldKey() {
        if (this.createWorldKey == null) {
            this.createWorldKey = this.getKey("key_Create New World");
        }
        return this.createWorldKey;
    }

    private Integer getKey(String keybindingTranslation) {
        String out = this.tryGetOption(keybindingTranslation, true);
        if (out == null) {
            return null;
        }
        Integer vkFromMCTranslation = McKeyUtil.getVkFromMCTranslation(out);
        if (vkFromMCTranslation == null) {
            log(Level.WARN, "INVALID KEY IN OPTIONS: " + out);
        }
        return vkFromMCTranslation;
    }

    private ResetType getResetType() {
        if (this.resetType != null) {
            return this.resetType;
        }
        if (this.getCreateWorldKey() != null) {
            this.getLeavePreviewKey();
            this.resetType = ResetType.MODERN_ATUM_EXIT;
            log(Level.DEBUG, this + " using MODERN_ATUM_EXIT");
        } else if (this.getLeavePreviewKey() != null) {
            this.resetType = ResetType.LEAVE_PREVIEW_EXIT;
            log(Level.DEBUG, this + " using LEAVE_PREVIEW_EXIT");
        } else {
            this.resetType = ResetType.VANILLA_EXIT;
            log(Level.DEBUG, this + " using VANILLA_EXIT");
        }
        return this.resetType;
    }

    private void setInPreview(boolean inPreview) {
        if (inPreview && !this.inPreview) {
            this.lastPreviewStart = System.currentTimeMillis();
        }
        this.inPreview = inPreview;
    }

    public void checkLog(Julti julti) {
        if (this.hasWindow()) {
            String newLogContents = this.getNewLogContents();
            this.checkLogContents(newLogContents, julti);
        }
    }

    public boolean shouldDirtCover() {
        return this.dirtCover;
    }

    public boolean isAvailable() {
        return this.available;
    }

    private void setAvailable(Julti julti) {
        if (!this.available) {
            this.available = true;
            julti.getResetManager().notifyInstanceAvailable(this);
            this.updateTimeLastAppeared();
        }
    }

    public boolean hasPreviewEverStarted() {
        return this.lastPreviewStart != -1L;
    }

    public boolean hasWorldEverLoaded() {
        return this.worldEverLoaded;
    }

    public String getBiome() {
        return this.biome;
    }

    private void checkLogContents(String newLogContents, final Julti julti) {
        JultiOptions options = JultiOptions.getInstance();
        if (!newLogContents.isEmpty()) {
            for (String line : newLogContents.split("\n")) {
                line = line.trim();
                if (this.isUsingWorldPreview() && !options.autoResetForBeach && startPreviewPattern.matcher(line).matches()) {
                    this.onPreviewLoad(options, julti);
                } else if (this.isUsingWorldPreview() && options.autoResetForBeach && startPreviewWithBiomePattern.matcher(line).matches()) {
                    this.onPreviewLoadWithBiome(options, julti, line);
                } else if (advancementsLoadedPattern.matcher(line).matches()) {
                    this.onWorldLoad(options, julti);
                } else if ((this.isPreviewLoaded() || !this.isUsingWorldPreview()) && spawnAreaPattern.matcher(line).matches()) {
                    this.onPercentLoadingLog(julti, line);
                } else if (openToLanPattern.matcher(line).matches()) {
                    this.openedToLan = true;
                    if ((!options.coopMode) && options.noCopeMode) {
                        julti.getResetManager().doReset();
                    }
                }
            }
        }
    }

    private void onPercentLoadingLog(Julti julti, String line) {
        String[] args = line.replace(" %", "%").split(" ");
        try {
            this.loadingPercent = Integer.parseInt(args[args.length - 1].replace("%", ""));
            // Return if not using world preview as the below functionality would not matter
            if (!this.isUsingWorldPreview()) {
                return;
            }
            JultiOptions options = JultiOptions.getInstance();
            if (this.isPreviewLoaded() && this.dirtCover && this.loadingPercent >= options.dirtReleasePercent) {
                this.updateTimeLastAppeared();
                this.dirtCover = false;
                this.setAvailable(julti);
            } else {
                this.dirtCover = this.loadingPercent < options.dirtReleasePercent;
            }
        } catch (Exception ignored) {
        }
    }

    private void onWorldLoad(JultiOptions options, Julti julti) {
        log(Level.DEBUG, this.getName() + ": World loaded");

        // Return if world already loaded
        if (this.isWorldLoaded()) {
            return;
        }

        // Return if reset is supposed to happen
        if (this.loadingPercent == -1) {
            this.reset(false);
            return;
        }

        // Update states
        this.setInPreview(false);
        this.worldLoaded = true;
        this.worldEverLoaded = true;
        this.dirtCover = false;
        this.setAvailable(julti);
        this.loadingPercent = 100;
        this.openedToLan = false;

        // Key press shenanigans
        if (options.pieChartOnLoad) {
            this.pressShiftF3();
            this.shouldPressDelayedWLKeys = true;
            new Timer("world-loader").schedule(new TimerTask() {
                @Override
                public void run() {
                    if (MinecraftInstance.this.shouldPressDelayedWLKeys) {
                        MinecraftInstance.this.finishWorldLoad(julti);
                    }
                }
            }, 150);
        } else {
            this.finishWorldLoad(julti);
        }
    }

    private void finishWorldLoad(Julti julti) {
        JultiOptions options = JultiOptions.getInstance();
        boolean active = this.isActive();
        if (this.shouldDoCleanWall()) {
            // Simple xor considers all 4 cases of f1:true vs f1:false combined with instance currently active
            if (active ^ this.f1SS == 0) {
                this.pressF1();
            }
        }
        if (options.pauseOnLoad && (!active || !options.unpauseOnSwitch)) {
            if (options.useF3) {
                this.pressF3Esc();
            } else {
                this.pressEsc();
            }
        } else if (active) {
            if (options.coopMode) {
                this.openToLan(!options.unpauseOnSwitch);
            }
            if (options.autoFullscreen) {
                this.pressFullscreenKey();
            }
        }
        julti.getResetManager().notifyWorldLoaded(this);
    }

    private void onPreviewLoad(JultiOptions options, Julti julti) {
        log(Level.DEBUG, this.getName() + ": Preview loaded");

        this.setInPreview(true);
        this.dirtCover = options.dirtReleasePercent >= 0;
        this.loadingPercent = -1;
        this.worldLoaded = false;
        if (options.useF3) {
            this.pressF3Esc();
        }
        julti.getResetManager().notifyPreviewLoaded(this);
        if (options.dirtReleasePercent < 0) {
            this.setAvailable(julti);
        }
    }

    private void onPreviewLoadWithBiome(JultiOptions options, Julti julti, String line) {
        this.setInPreview(true);
        this.dirtCover = options.dirtReleasePercent >= 0;
        this.loadingPercent = -1;
        this.worldLoaded = false;
        if (options.useF3) {
            this.pressF3Esc();
        }
        String[] args = line.split(" ");
        this.biome = args[args.length - 1];
        julti.getResetManager().notifyPreviewLoaded(this);
        if (options.dirtReleasePercent < 0) {
            this.setAvailable(julti);
        }
    }

    private void pressShiftF3() {
        KeyboardUtil.sendKeyDownToHwnd(this.hwnd, Win32Con.VK_RSHIFT, true);
        this.pressF3();
        KeyboardUtil.sendKeyUpToHwnd(this.hwnd, Win32Con.VK_RSHIFT, true);
    }

    String getNewLogContents() {
        Path logPath = this.getLogPath();

        // If log progress has not been jumped, jump and return
        if (this.logProgress == -1) {
            this.tryJumpLogProgress();
            return "";
        }

        // If modification date has not changed, return
        try {
            FileTime newModifyTime = Files.getLastModifiedTime(logPath);
            if (!newModifyTime.equals(this.lastLogModify)) {
                this.lastLogModify = newModifyTime;
            } else {
                return "";
            }
        } catch (Exception ignored) {
            return "";
        }

        // If file size is significantly less than log progress, reset log progress
        try {
            long size = Files.size(logPath);
            if (size < (this.logProgress / 2)) {
                this.tryJumpLogProgress();
                log(Level.INFO, "Log reading restarted! (" + this.getName() + ")");
                return "";
            }
        } catch (IOException ignored) {
        }


        // Read new bytes then format and return as a string
        try (InputStream stream = Files.newInputStream(logPath)) {
            stream.skip(this.logProgress);

            ArrayList<Byte> byteList = new ArrayList<>();

            int next = stream.read();
            while (next != -1) {
                byteList.add((byte) next);
                this.logProgress++;
                next = stream.read();
            }

            byte[] bytes = new byte[byteList.size()];
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = byteList.get(i);
            }

            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return "";
        }
    }

    /**
     * Sets logProgress to the amount of bytes in the latest log of the instance.
     * Failure is ignored, as logProgress will still be -1 afterwards, indicating the task would still need to be done.
     */
    private void tryJumpLogProgress() {
        try {
            Path logPath = this.getLogPath();
            if (Files.isRegularFile(logPath)) {
                this.logProgress = Files.readAllBytes(logPath).length;
                this.lastLogModify = Files.getLastModifiedTime(logPath);
            }
        } catch (IOException ignored) {
        }
    }

    public Path getLogPath() {
        Path instancePath = this.getInstancePath();
        if (this.notMC) {
            return null;
        }
        return instancePath.resolve("logs").resolve("latest.log");
    }

    public void tryClearWorlds() {
        try {
            this.clearWorlds();
        } catch (Exception ignored) {
        }
    }

    private void clearWorlds() throws IOException {
        Path savesPath = this.getInstancePath().resolve("saves");
        List<Path> worldsToRemove = new ArrayList<>();
        for (String string : savesPath.toFile().list()) {
            if (!string.startsWith("_")) {
                worldsToRemove.add(savesPath.resolve(string));
            }
        }
        worldsToRemove.removeIf(path -> (!path.toFile().isDirectory()) || (path.resolve("Reset Safe.txt").toFile().isFile()));
        worldsToRemove.sort((o1, o2) -> (int) (o2.toFile().lastModified() - o1.toFile().lastModified()));
        for (int i = 0; i < 6 && !worldsToRemove.isEmpty(); i++) {
            worldsToRemove.remove(0);
        }
        int i = 0;
        int total = worldsToRemove.size();
        for (Path path : worldsToRemove) {
            if (++i % 50 == 0) {
                InstanceManager.log(Level.INFO, "Clearing " + this.getName() + ": " + i + "/" + total);
            }
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    public boolean wasPreviewInLastMillis(int millis) {
        return System.currentTimeMillis() - this.lastPreviewStart < millis;
    }

    public void openFolder() {
        try {
            Desktop.getDesktop().browse(this.getInstancePath().toUri());
        } catch (IOException ignored) {

        }
    }

    public void sendChatMessage(String chatMessage) {
        KeyboardUtil.sendKeyToHwnd(this.hwnd, 0x54);
        sleep(100); // magic number
        for (char c : chatMessage.toCharArray()) {
            KeyboardUtil.sendCharToHwnd(this.hwnd, c);
        }
        this.pressEnter();
    }

    private void pressEnter() {
        KeyboardUtil.sendKeyToHwnd(this.hwnd, Win32Con.VK_RETURN);
    }

    private enum ResetType {
        VANILLA_EXIT, // Esc+Shift+Tab+Enter always
        LEAVE_PREVIEW_EXIT, // Esc+Shift+Tab+Enter but use leavePreviewKey when in preview
        MODERN_ATUM_EXIT // Use createWorldKey always
    }
}
