package xyz.duncanruns.julti.instance;

import com.sun.jna.Pointer;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.ResetCounter;
import xyz.duncanruns.julti.util.HwndUtil;
import xyz.duncanruns.julti.util.KeyboardUtil;
import xyz.duncanruns.julti.util.LogReceiver;
import xyz.duncanruns.julti.util.McKeyUtil;
import xyz.duncanruns.julti.win32.Win32Con;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MinecraftInstance {

    private static final Logger LOGGER = LogManager.getLogger("MinecraftInstance");
    private static final Pattern startPreviewPattern = Pattern.compile("^\\[\\d\\d:\\d\\d:\\d\\d] \\[.*/INFO]: Starting Preview at \\(-?\\d+\\.\\d+, -?\\d+\\.\\d+, -?\\d+\\.\\d+\\)$");
    private static final Pattern startPreviewWithBiomePattern = Pattern.compile("^\\[\\d\\d:\\d\\d:\\d\\d] \\[.*/INFO]: Starting Preview at \\(-?\\d+(\\.\\d+)?, -?\\d+(\\.\\d+)?, -?\\d+(\\.\\d+)?\\) in biome .+$");
    private static final Pattern spawnAreaPattern = Pattern.compile("^\\[\\d\\d:\\d\\d:\\d\\d] \\[.*/INFO]: .+: \\d+ ?%$");
    private static final Pattern advancementsLoadedPattern = Pattern.compile("^\\[\\d\\d:\\d\\d:\\d\\d] \\[.+/INFO]: Loaded \\d advancements$");
    private static final Pattern openToLanPattern = Pattern.compile("^\\[\\d\\d:\\d\\d:\\d\\d] \\[.*/INFO]: Started serving on \\d+$");

    // Basic instance information
    private final WindowTitleInfo titleInfo;
    private Pointer hwnd;
    private Path instancePath = null;
    private String name = null;

    // Missing Window Stuff
    private boolean notMC = false; // true when a MinecraftInstance is constructed with a window handle which points to a non-mc window
    private boolean missingReported = false;

    // Information to be discovered
    private ResetType resetType = null;
    private Integer createWorldKey = null;
    private Integer fullscreenKey = null;
    private Integer leavePreviewKey = null;
    private Boolean usingWorldPreview = null;
    private Boolean usingStandardSettings = null;

    // State tracking
    private boolean inPreview = false;
    private boolean worldLoaded = false;
    private long lastPreviewStart = -1L;
    private long lastResetPress = -1L;
    private String biome = "";
    private int loadingPercent = 0;
    private boolean dirtCover = false;
    boolean worldEverLoaded = false;
    boolean shouldPressDelayedWLKeys = false; // "Should press delayed world load keys"

    // Log tracking
    private long logProgress = -1;
    private FileTime lastLogModify = null;
    private Integer pid = null;

    public MinecraftInstance(Path instancePath) {
        this.hwnd = null;
        this.titleInfo = new WindowTitleInfo();
        this.instancePath = instancePath;
        this.notMC = false;

        ensureGoodStandardSettings();
    }

    public MinecraftInstance(Pointer hwnd) {
        this.hwnd = hwnd;
        this.titleInfo = new WindowTitleInfo(getCurrentWindowTitle());

        getInstancePath();
        ensureGoodStandardSettings();
    }

    private static String getOptionFromString(String optionName, String optionsString) {
        String[] lines = optionsString.trim().split("\\n");
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i];
            if (line.startsWith(optionName + ":")) return line.split(":")[1];
        }
        return null;
    }

    private void ensureGoodStandardSettings() {
        if (!isUsingStandardSettings()) return;

        String[] goodSettings = new String[]{
                "f1:true",
                "changeOnResize:true",
                "fullscreen:false",
                "pauseOnLostFocus:false",
                "key_Cycle ChunkMap Positions:key.keyboard.unknown"
        };

        for (String setting : goodSettings) {

            String[] settingVals = setting.split(":");
            String optionName = settingVals[0];
            String desiredValue = settingVals[1];
            String currentValue = getStandardOption(optionName);

            if (desiredValue.equals(currentValue)) continue;

            forceStandardSetting(optionName, desiredValue);
            log(Level.INFO, "Set \"" + optionName + "\" to \"" + desiredValue + "\" in standard settings for " + getName());
        }
    }

    private void forceStandardSetting(String optionName, String optionValue) {
        Path path = instancePath.resolve("config").resolve("standardoptions.txt");
        while (true) {
            try {
                String contents = Files.readString(path).trim();
                if (!contents.endsWith(".txt"))
                    break;
                Path deeperPath = Path.of(contents);
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
            if (!currentLine.startsWith(optionName + ":"))
                out.append("\n").append(currentLine);
        }
        out.append("\n").append(optionName).append(":").append(optionValue);

        try {
            Files.writeString(path, out.toString().trim());
        } catch (IOException ignored) {
        }

    }

    private String getCurrentWindowTitle() {
        if (!hasWindow()) return "Missing Window";
        return HwndUtil.getHwndTitle(hwnd);
    }

    public boolean isFullscreen() {
        return Objects.equals(getOption("fullscreen", false), "true");
    }

    public String getOption(String optionName, boolean tryUseSS) {
        if (tryUseSS) {
            String out = getStandardOption(optionName);
            if (out != null) return out;
        }

        Path path = instancePath.resolve("options.txt");

        if (!Files.exists(path)) return null;

        String out;
        try {
            out = Files.readString(path);
        } catch (IOException e) {
            // This should never be reached
            return null;
        }

        return getOptionFromString(optionName, out);
    }

    private String getStandardOption(String optionName) {
        String out = getStandardOption(optionName, instancePath.resolve("config").resolve("standardoptions.txt"));
        if (out != null) return out.trim();
        return null;
    }

    private String getStandardOption(String optionName, Path path) {
        if (!Files.exists(path)) return null;

        String out;
        try {
            out = Files.readString(path).trim();
        } catch (IOException e) {
            // This should never be reached
            return null;
        }

        if (!out.contains("\n")) {
            if (out.endsWith(".txt")) {
                return getStandardOption(optionName, Path.of(out));
            }
        }

        return getOptionFromString(optionName, out);
    }

    public boolean hasWindowQuick() {
        return hwnd != null;
    }

    synchronized public long getLastPreviewStart() {
        return lastPreviewStart;
    }

    public long getLastResetPress() {
        return lastResetPress;
    }

    public String getOriginalTitle() {
        if (titleInfo.waiting()) {
            titleInfo.provide(HwndUtil.getHwndTitle(hwnd));
        }
        return titleInfo.getOriginalTitle();
    }

    public boolean isActuallyMC() {
        getInstancePath();
        return !notMC;
    }

    public Path getInstancePath() {
        if (instancePath != null) return instancePath;

        if (notMC || !hasWindow()) return null;

        instancePath = HwndUtil.getInstancePathFromPid(getPid());
        if (instancePath == null) {
            notMC = true;
        }

        return instancePath;
    }

    public boolean hasWindow() {
        if (hwnd != null && HwndUtil.hwndExists(hwnd)) {
            return true;
        } else {
            hwnd = null;
            return false;
        }
    }

    public int getPid() {
        if (pid == null) {
            pid = HwndUtil.getPidFromHwnd(hwnd);
            return pid;
        }
        return pid;
    }

    public Pointer getHwnd() {
        // Note: if hwnd == null, the instance is unusable. The proper way to manage this is to replace the object with a new one.
        return hwnd;
    }

    /**
     * Returns a sorting number which is the sum of all groups of digit characters.
     * For example, "Multi 1.16 02" would be 1+16+2 = 19.
     *
     * @return a sorting number to correctly sort instances.
     */
    public int getSortingNum() {
        AtomicInteger i = new AtomicInteger(0);
        String name = getName();
        Pattern.compile("\\d+").matcher(name).results().forEach(matchResult -> {
            String section = name.substring(matchResult.start(), matchResult.end());
            if (section.length() == 0) return;
            while (section.length() > 1 && section.startsWith("0")) {
                section = section.substring(1);
            }
            i.addAndGet(Integer.parseInt(section));
        });
        return i.get();
    }

    public String getName() {
        // Return existing name
        if (name != null) {
            return name;
        }

        // Get instance path
        Path instancePath = getInstancePath();
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
                        name = StringEscapeUtils.unescapeJson(line.split("=")[1]);
                        return name;
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
        name = instancePath.getName(instancePath.getNameCount() - 1).toString();
        if (name.equals("Roaming")) {
            return "Default Launcher";
        }
        return name;
    }

    public boolean justWentMissing() {
        if (!hasWindow() && !missingReported) {
            missingReported = true;
            return true;
        }
        return false;
    }

    public void pressFullscreenKey() {
        KeyboardUtil.sendKeyToHwnd(hwnd, getFullscreenKey());
    }

    public Integer getFullscreenKey() {
        if (fullscreenKey == null) {
            fullscreenKey = getKey("key_key.fullscreen");
        }
        return fullscreenKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MinecraftInstance that = (MinecraftInstance) o;

        if (hwnd == null && that.hwnd == null) {
            return false;
        }

        return Objects.equals(hwnd, that.hwnd);
    }

    @Override
    public String toString() {
        return "Instance \"" + getName() + "\"";
    }

    /**
     * @param instanceNum -1 for not updating title, otherwise the real instance number (index + 1).
     */
    synchronized public void activate(int instanceNum) {
        JultiOptions options = JultiOptions.getInstance();
        if (hasWindow()) {
            new Thread(this::ensureWindowState).start();
            HwndUtil.showHwnd(hwnd);
            HwndUtil.activateHwnd(hwnd);
            if (worldLoaded) {
                new Thread(() -> {
                    int i = 100;
                    while (!isActive() && i > 0) {
                        sleep(10);
                        i--;
                    }
                    if (options.unpauseOnSwitch) {
                        pressEsc();
                        if (options.wideResetSquish > 1.0) {
                            // 2 Extra Escape Presses to make sure mouse is centered on next menu open
                            pressEsc();
                            pressEsc();
                        }
                    }
                    if (options.coopMode) {
                        openToLan(!options.unpauseOnSwitch);
                    }
                    if (isUsingStandardSettings()) {
                        pressF1();
                    }
                }).start();
            }
            if (instanceNum != -1) setWindowTitle("Minecraft* - Instance " + instanceNum);
            log(Level.INFO, "Activated instance " + getName());
        } else {
            log(Level.WARN, "Could not activate instance " + getName() + " (not opened)");
        }
    }

    public void ensureWindowState() {
        JultiOptions options = JultiOptions.getInstance();

        // "Do nothing" conditions
        if (!options.letJultiMoveWindows) return;
        Rectangle rectangle = getWindowRectangle();
        if (options.windowPos[0] == rectangle.x &&
                options.windowPos[1] == rectangle.y &&
                options.windowSize[0] == rectangle.width &&
                options.windowSize[1] == rectangle.height &&
                options.useBorderless == isBorderless() &&
                (options.useBorderless || isMaximized())
        ) return;

        if (options.useBorderless) setBorderless();
        else undoBorderless();


        if (!options.useBorderless) maximize();
        else {
            restore();
            move(options.windowPos[0], options.windowPos[1], options.windowSize[0], options.windowSize[1]);
        }

    }

    private boolean isActive() {
        return Objects.equals(HwndUtil.getCurrentHwnd(), hwnd);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }

    private void pressEsc() {
        KeyboardUtil.sendKeyToHwnd(hwnd, Win32Con.VK_ESCAPE);
    }

    public void openToLan(boolean alreadyInMenu) {
        KeyboardUtil.releaseAllModifiers();
        if (!alreadyInMenu)
            pressEsc();
        pressTab(7);
        pressEnter();
        pressShiftTab(1);
        pressEnter();
        pressTab(1);
        pressEnter();

    }

    public boolean isUsingStandardSettings() {
        if (usingStandardSettings != null) return usingStandardSettings;

        boolean exists = doesModExist("standardsettings");
        usingStandardSettings = exists;
        return exists;
    }

    private void pressF1() {
        KeyboardUtil.sendKeyToHwnd(hwnd, Win32Con.VK_F1);
    }

    public void setWindowTitle(String title) {
        if (hasWindow()) {
            HwndUtil.setHwndTitle(hwnd, title);
        }
    }

    public static void log(Level level, String message) {
        LOGGER.log(level, message);
        LogReceiver.receive(level, message);
    }

    public Rectangle getWindowRectangle() {
        return HwndUtil.getHwndRectangle(hwnd);
    }

    public boolean isBorderless() {
        return HwndUtil.isHwndBorderless(hwnd);
    }

    public boolean isMaximized() {
        return HwndUtil.isHwndMaximized(hwnd);
    }

    public void setBorderless() {
        HwndUtil.setHwndBorderless(hwnd);
    }

    public void undoBorderless() {
        HwndUtil.undoHwndBorderless(hwnd);
    }

    public void maximize() {
        HwndUtil.maximizeHwnd(hwnd);
    }

    public void restore() {
        HwndUtil.restoreHwnd(hwnd);
    }

    public void move(int x, int y, int w, int h) {
        HwndUtil.moveHwnd(hwnd, x, y, w, h);
    }

    private void pressTab(int tabTimes) {
        for (int i = 0; i < tabTimes; i++)
            KeyboardUtil.sendKeyToHwnd(hwnd, Win32Con.VK_TAB);
    }

    private void pressEnter() {
        KeyboardUtil.sendKeyToHwnd(hwnd, Win32Con.VK_RETURN);
    }

    private void pressShiftTab(int tabTimes) {
        KeyboardUtil.sendKeyDownToHwnd(hwnd, Win32Con.VK_LSHIFT, true);
        pressTab(tabTimes);
        KeyboardUtil.sendKeyUpToHwnd(hwnd, Win32Con.VK_LSHIFT, true);
    }

    private boolean doesModExist(String modName) {
        Path modsPath = instancePath.resolve("mods");
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
        if (squish == 1f) return;

        JultiOptions options = JultiOptions.getInstance();
        Rectangle resultRectangle = new Rectangle(options.windowPos[0], options.windowPos[1], options.windowSize[0], (int) (options.windowSize[1] / squish));
        if (isMaximized()) {
            restore();
        } else {
            if (getWindowRectangle().equals(resultRectangle)) return;
        }
        move(resultRectangle.x, resultRectangle.y, resultRectangle.width, resultRectangle.height);
    }

    public void closeWindow() {
        if (hasWindow()) {
            HwndUtil.sendCloseMessage(hwnd);
            log(Level.INFO, "Closed " + getName());
        } else {
            log(Level.WARN, "Could not close " + getName() + " because it is not open.");
        }
    }

    public void launch(String offlineName) {
        try {
            String multiMCPath = JultiOptions.getInstance().multiMCPath;
            if (!multiMCPath.isEmpty()) {
                String cmd;
                if (offlineName == null) {
                    cmd = multiMCPath.trim() + " --launch \"" + getName() + "\"";
                } else {
                    cmd = multiMCPath.trim() + " --launch \"" + getName() + "\" -o -n " + offlineName;
                }
                Runtime.getRuntime().exec(cmd);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void pressF3Esc() {
        KeyboardUtil.sendKeyDownToHwnd(hwnd, Win32Con.VK_F3, true);
        KeyboardUtil.sendKeyToHwnd(hwnd, Win32Con.VK_ESCAPE);
        KeyboardUtil.sendKeyUpToHwnd(hwnd, Win32Con.VK_F3, true);
    }

    private void pressF3() {
        KeyboardUtil.sendKeyToHwnd(hwnd, Win32Con.VK_F3);
    }

    synchronized public void reset(boolean singleInstance) {
        // If no window, do nothing
        if (!hasWindow()) {
            log(Level.INFO, "Could not reset instance " + getName() + " (not opened)");
            return;
        }

        JultiOptions options = JultiOptions.getInstance();

        // Before taking any action, store some info useful for fullscreen management
        final boolean wasFullscreen = isFullscreen();
        Rectangle ogRect = null;
        if (wasFullscreen) {
            ogRect = getWindowRectangle();
        }

        if (isWorldLoaded()) {
            if (isFullscreen()) pressFullscreenKey();
            if (isUsingStandardSettings()) {
                pressF1();
                // Delay is needed for f1/f3 to actually do something
                sleep(50);
            }
        }
        pressResetKey();

        //Update states
        worldLoaded = false;
        loadingPercent = -1;
        setInPreview(false);
        dirtCover = true;
        shouldPressDelayedWLKeys = false;

        if (wasFullscreen) {
            // Wait until window actually un-fullscreens
            // Or until 2 ish seconds have passed
            for (int i = 0; i < 200; i++) {
                if (!Objects.equals(ogRect, getWindowRectangle())) break;
                sleep(10);
            }
        }

        // Window Resizing and Shid
        new Timer("delayed-window-fixer").schedule(new TimerTask() {
            @Override
            public void run() {
                if (!options.letJultiMoveWindows) return;
                if (wasFullscreen && options.useBorderless) {
                    setBorderless();
                }
                if (!singleInstance && options.letJultiMoveWindows)
                    squish(options.wideResetSquish);
            }
        }, 50);

        // Log and reset counter update
        log(Level.INFO, "Reset instance " + getName());
        ResetCounter.increment();
    }

    private void pressResetKey() {
        lastResetPress = System.currentTimeMillis();
        switch (getResetType()) {
            case NEW_ATUM:
                if (getLeavePreviewKey() != null)
                    KeyboardUtil.sendKeyToHwnd(hwnd, getLeavePreviewKey());
                KeyboardUtil.sendKeyToHwnd(hwnd, getCreateWorldKey());
            case HAS_PREVIEW:
                if (inPreview) {
                    KeyboardUtil.sendKeyToHwnd(hwnd, getLeavePreviewKey());
                } else {
                    runNoAtumLeave();
                }
            case EXIT_WORLD:
                runNoAtumLeave();
        }
    }

    private void runNoAtumLeave() {
        WindowTitleInfo.Version version = titleInfo.getVersion();

        KeyboardUtil.sendKeyToHwnd(hwnd, Win32Con.VK_ESCAPE);
        if (version.getMajor() > 12) {
            pressShiftTab(1);
        } else if (version.getMajor() == 8 && version.getMinor() == 9) {
            sleep(70); // Magic Number
            // Anchiale Support
            for (int i = 0; i < 7; i++) {
                pressTab(1);
            }
        } else {
            sleep(70); // Magic Number
            pressTab(1);
        }
        pressEnter();
    }

    public boolean isUsingWorldPreview() {
        if (usingWorldPreview != null) return usingWorldPreview;

        boolean exists = doesModExist("worldpreview");
        usingWorldPreview = exists;
        return exists;
    }

    private Integer getLeavePreviewKey() {
        if (leavePreviewKey == null) {
            leavePreviewKey = getKey("key_Leave Preview");
        }
        return leavePreviewKey;
    }

    private Integer getCreateWorldKey() {
        if (createWorldKey == null) {
            createWorldKey = getKey("key_Create New World");
        }
        return createWorldKey;
    }

    private Integer getKey(String keybindingTranslation) {
        String out = getOption(keybindingTranslation, true);
        if (out == null) return null;
        return McKeyUtil.getVkFromMCTranslation(out);
    }

    private ResetType getResetType() {
        if (resetType != null) return resetType;
        if (getCreateWorldKey() != null) {
            resetType = ResetType.NEW_ATUM;
        } else if (getLeavePreviewKey() != null) {
            resetType = ResetType.HAS_PREVIEW;
        } else {
            resetType = ResetType.EXIT_WORLD;
        }
        return resetType;
    }

    synchronized private void setInPreview(boolean inPreview) {
        if (inPreview && !this.inPreview) lastPreviewStart = System.currentTimeMillis();
        this.inPreview = inPreview;
    }

    public void checkLog(Julti julti) {
        if (hasWindow()) {
            String newLogContents = getNewLogContents();
            checkLogContents(newLogContents, julti);
        }
    }

    public boolean shouldDirtCover() {
        return dirtCover;
    }

    public boolean hasPreviewEverStarted() {
        return lastPreviewStart != -1L;
    }

    public boolean hasWorldEverLoaded() {
        return worldEverLoaded;
    }

    synchronized public boolean isWorldLoaded() {
        return worldLoaded;
    }

    public int getLoadingPercent() {
        return loadingPercent;
    }

    synchronized public boolean isPreviewLoaded() {
        return inPreview;
    }

    public String getBiome() {
        return biome;
    }

    synchronized private void checkLogContents(String newLogContents, final Julti julti) {
        JultiOptions options = JultiOptions.getInstance();
        if (!newLogContents.isEmpty()) {
            for (String line : newLogContents.split("\n")) {
                line = line.trim();
                if (isUsingWorldPreview() && !options.autoResetForBeach && startPreviewPattern.matcher(line).matches()) {
                    onPreviewLoad(options, julti);
                } else if (isUsingWorldPreview() && options.autoResetForBeach && startPreviewWithBiomePattern.matcher(line).matches()) {
                    onPreviewLoadWithBiome(options, julti, line);
                } else if (advancementsLoadedPattern.matcher(line).matches()) {
                    onWorldLoad(options, julti);
                } else if ((isPreviewLoaded() || !isUsingWorldPreview()) && spawnAreaPattern.matcher(line).matches()) {
                    onPercentLoadingLog(line);
                } else if ((!options.coopMode) && options.noCopeMode && openToLanPattern.matcher(line).matches()) {
                    julti.getResetManager().doReset();
                }
            }
        }
    }

    private void onPercentLoadingLog(String line) {
        String[] args = line.replace(" %", "%").split(" ");
        try {
            loadingPercent = Integer.parseInt(args[args.length - 1].replace("%", ""));
            dirtCover = loadingPercent < JultiOptions.getInstance().dirtReleasePercent;
        } catch (Exception ignored) {
        }
    }

    private void onWorldLoad(JultiOptions options, Julti julti) {
        // Return if reset is supposed to happen
        if (loadingPercent == -1) {
            reset(false);
            return;
        }

        // Update states
        setInPreview(false);
        worldLoaded = true;
        worldEverLoaded = true;
        dirtCover = false;
        loadingPercent = 100;

        // Key press shenanigans
        if (options.pieChartOnLoad) {
            pressShiftF3();
            shouldPressDelayedWLKeys = true;
            new Timer("world-loader").schedule(new TimerTask() {
                @Override
                public void run() {
                    if (shouldPressDelayedWLKeys)
                        worldLoadKeyPresses(options, julti);
                }
            }, 150);
        } else {
            worldLoadKeyPresses(options, julti);
        }
    }

    private void worldLoadKeyPresses(JultiOptions options, Julti julti) {
        boolean active = isActive();
        if (active && isUsingStandardSettings())
            pressF1();
        if (options.pauseOnLoad && (!active || !options.unpauseOnSwitch)) {
            if (options.useF3) {
                pressF3Esc();
            } else {
                pressEsc();
            }
        } else if (active) {
            if (options.coopMode)
                openToLan(!options.unpauseOnSwitch);
        }
        julti.getResetManager().notifyWorldLoaded(this);
    }

    private void onPreviewLoad(JultiOptions options, Julti julti) {
        setInPreview(true);
        dirtCover = true;
        loadingPercent = -1;
        worldLoaded = false;
        if (options.useF3) {
            pressF3Esc();
        }
        julti.getResetManager().notifyPreviewLoaded(this);
    }

    private void onPreviewLoadWithBiome(JultiOptions options, Julti julti, String line) {
        setInPreview(true);
        dirtCover = true;
        loadingPercent = -1;
        worldLoaded = false;
        if (options.useF3) {
            pressF3Esc();
        }
        String[] args = line.split(" ");
        biome = args[args.length - 1];
        julti.getResetManager().notifyPreviewLoaded(this);
    }

    private void pressShiftF3() {
        KeyboardUtil.sendKeyDownToHwnd(hwnd, Win32Con.VK_RSHIFT, true);
        pressF3();
        KeyboardUtil.sendKeyUpToHwnd(hwnd, Win32Con.VK_RSHIFT, true);
    }

    String getNewLogContents() {
        Path logPath = getLogPath();

        // If log progress has not been jumped, jump and return
        if (logProgress == -1) {
            tryJumpLogProgress();
            return "";
        }

        // If modification date has not changed, return
        try {
            FileTime newModifyTime = Files.getLastModifiedTime(logPath);
            if (!newModifyTime.equals(lastLogModify)) {
                lastLogModify = newModifyTime;
            } else {
                return "";
            }
        } catch (Exception ignored) {
            return "";
        }

        // If file size is significantly less than log progress, reset log progress
        try {
            long size = Files.size(logPath);
            if (size < (logProgress / 2)) {
                tryJumpLogProgress();
                log(Level.INFO, "Log reading restarted! (" + getName() + ")");
                return "";
            }
        } catch (IOException ignored) {
        }


        // Read new bytes then format and return as a string
        try (InputStream stream = Files.newInputStream(logPath)) {
            stream.skip(logProgress);

            ArrayList<Byte> byteList = new ArrayList<>();

            int next = stream.read();
            while (next != -1) {
                byteList.add((byte) next);
                logProgress++;
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
            Path logPath = getLogPath();
            if (Files.isRegularFile(logPath)) {
                logProgress = Files.readAllBytes(logPath).length;
                lastLogModify = Files.getLastModifiedTime(logPath);
            }
        } catch (IOException ignored) {
        }
    }

    public Path getLogPath() {
        Path instancePath = getInstancePath();
        if (notMC) {
            return null;
        }
        return instancePath.resolve("logs").resolve("latest.log");
    }

    public void tryClearWorlds() {
        try {
            clearWorlds();
        } catch (Exception ignored) {
        }
    }

    private void clearWorlds() throws IOException {
        Path savesPath = getInstancePath().resolve("saves");
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
            if (i++ % 50 == 0) {
                InstanceManager.log(Level.INFO, (100 * (i / total)) + "%");
            }
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    public boolean wasPreviewInLastMillis(int millis) {
        return System.currentTimeMillis() - lastPreviewStart < millis;
    }

    public void openFolder() {
        try {
            Desktop.getDesktop().browse(getInstancePath().toUri());
        } catch (IOException ignored) {

        }
    }

    private enum ResetType {
        EXIT_WORLD, // Esc+Shift+Tab+Enter always
        HAS_PREVIEW, // Esc+Shift+Tab+Enter but use leavePreviewKey when in preview
        NEW_ATUM // Use createWorldKey always
    }
}
