package xyz.duncanruns.julti.instance;

import com.sun.jna.Pointer;
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
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class MinecraftInstance {

    private static final Logger LOGGER = LogManager.getLogger("MinecraftInstance");
    private static final Pattern advancementsLoadedPattern = Pattern.compile("^\\[\\d\\d:\\d\\d:\\d\\d] \\[.+/INFO]: Loaded 0 advancements$");
    private static final Pattern startPreviewPattern = Pattern.compile("^\\[\\d\\d:\\d\\d:\\d\\d] \\[.*/INFO]: Starting Preview at \\(-?\\d+\\.\\d+, -?\\d+\\.\\d+, -?\\d+\\.\\d+\\)$");
    private static final Pattern spawnAreaPattern = Pattern.compile("^\\[\\d\\d:\\d\\d:\\d\\d] \\[.*/INFO]: Preparing spawn area: \\d+%$");
    private static final Pattern openToLanPattern = Pattern.compile("^\\[\\d\\d:\\d\\d:\\d\\d] \\[.*/INFO]: Started serving on \\d+$");
    private static final Pattern startPreviewWithBiomePattern = Pattern.compile("^\\[\\d\\d:\\d\\d:\\d\\d] \\[.*/INFO]: Starting Preview at \\(-?\\d+(\\.\\d+)?, -?\\d+(\\.\\d+)?, -?\\d+(\\.\\d+)?\\) in biome .+$");

    // Basic instance information
    private final WindowTitleInfo titleInfo;
    private Pointer hwnd;
    private Path instancePath = null;

    // Missing Window Stuff
    private boolean notMC = false; // true when a MinecraftInstance is constructed with a window handle which points to a non-mc window
    private boolean missingReported = false;

    // Information to be discovered
    private ResetType resetType = null;
    private Integer createWorldKey = null;
    private Integer fullscreenKey = null;
    private Integer leavePreviewKey = null;

    // State tracking
    private boolean inPreview = false;
    private boolean worldLoaded = false;
    private long lastPreviewStart = -1L;
    private long lastResetPress = -1L;
    private String biome = "";
    private int loadingPercent = 0;
    private boolean dirtCover = false;
    boolean fullscreen = false;

    // Log tracking
    private long logProgress = -1;
    private FileTime lastLogModify = null;
    private Integer pid = null;


    public MinecraftInstance(Pointer hwnd) {
        this.hwnd = hwnd;
        this.titleInfo = new WindowTitleInfo(getCurrentWindowTitle());
    }

    private String getCurrentWindowTitle() {
        if (!hasWindow()) return "Missing Window";
        return HwndUtil.getHwndTitle(hwnd);
    }

    public boolean hasWindow() {
        if (hwnd != null && HwndUtil.hwndExists(hwnd)) {
            return true;
        } else {
            hwnd = null;
            return false;
        }
    }

    public MinecraftInstance(Path instancePath) {
        this.hwnd = null;
        this.titleInfo = new WindowTitleInfo();
        this.instancePath = instancePath;
        this.notMC = false;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }

    public boolean isFullscreen() {
        return fullscreen;
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

    private Integer getCreateWorldKey() {
        if (createWorldKey == null) {
            createWorldKey = getKey("key_Create New World");
        }
        return createWorldKey;
    }

    private Integer getLeavePreviewKey() {
        if (leavePreviewKey == null) {
            leavePreviewKey = getKey("key_Leave Preview");
        }
        return leavePreviewKey;
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
     * Returns a sorting number which can be summarized as follows: For each character in the instance name that is a
     * number, add 11 plus the number itself. For example, "Multi 16 2" would be (11+1)+(11+6)+(11+2) = 42.
     *
     * @return a sorting number to correctly sort instances.
     */
    public int getSortingNum() {
        int i = 0;
        for (char c : getName().toCharArray()) {
            if (c >= '0' && c <= '9') {
                i += 11 + (c - '0');
            }
        }
        return i;
    }

    public String getName() {
        Path instancePath = getInstancePath();
        if (instancePath == null) {
            return "Unknown Instance"; //This name should probably never be seen, regardless it is here.
        }
        if (instancePath.getName(instancePath.getNameCount() - 1).toString().equals(".minecraft")) {
            instancePath = instancePath.getParent();
        }
        String name = instancePath.getName(instancePath.getNameCount() - 1).toString();
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
        fullscreen = !fullscreen;
    }

    private Integer getFullscreenKey() {
        if (fullscreenKey == null) {
            fullscreenKey = getKey("key_key.fullscreen");
        }
        return fullscreenKey;
    }

    private Integer getKey(String keybindingTranslation) {
        Path optionsPath = getInstancePath().resolve("options.txt");
        try {
            for (String line : Files.readAllLines(optionsPath)) {
                String[] args = line.split(":");
                if (args.length > 1 && keybindingTranslation.equals(args[0])) {
                    return McKeyUtil.getVkFromMCTranslation(args[1]);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
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
                    if (options.useFullscreen) {
                        pressFullscreenKey();
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

    public void setWindowTitle(String title) {
        if (hasWindow()) {
            HwndUtil.setHwndTitle(hwnd, title);
        }
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

    public ScreenCapUtil.ImageInfo captureScreen() {
        return ScreenCapUtil.capture(this.hwnd);
    }

    public void closeWindow() {
        if (hasWindow()) {
            HwndUtil.sendCloseMessage(hwnd);
            log(Level.INFO, "Closed " + getName());
        } else {
            log(Level.WARN, "Could not close " + getName() + " because it is not open.");
        }
    }

    public static void log(Level level, String message) {
        LOGGER.log(level, message);
        LogReceiver.receive(level, message);
    }

    public void launch() {
        try {
            String multiMCPath = JultiOptions.getInstance().multiMCPath;
            if (!multiMCPath.isEmpty())
                Runtime.getRuntime().exec(new String[]{multiMCPath, "--launch", getName()});
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

        pressResetKey();
        worldLoaded = false;
        loadingPercent = -1;
        setInPreview(false);
        dirtCover = true;
        log(Level.INFO, "Reset instance " + getName());

        final boolean wasFullscreen = fullscreen;

        // Wait until safe to continue
        if (wasFullscreen) {
            int ogx = getWindowRectangle().x;
            // Wait until window actually un-fullscreens
            // Or until 2 ish seconds have passed
            for (int i = 0; i < 200; i++) {
                if (getWindowRectangle().x != ogx) break;
                sleep(10);
            }
            fullscreen = false;
        }


        new Thread(() -> {
            if (wasFullscreen && options.useBorderless) {
                setBorderless();
            }
            if (!singleInstance && options.letJultiMoveWindows)
                squish(options.wideResetSquish);
        }).start();


        ResetCounter.increment();
    }

    private void pressResetKey() {
        lastResetPress = System.currentTimeMillis();
        switch (getResetType()) {
            case NEW_ATUM:
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

    private ResetType getResetType() {
        if (resetType == null) {
            if (getCreateWorldKey() != null) {
                resetType = ResetType.NEW_ATUM;
            } else if (getLeavePreviewKey() != null) {
                resetType = ResetType.HAS_PREVIEW;
            } else {
                resetType = ResetType.EXIT_WORLD;
            }
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
            JultiOptions options = JultiOptions.getInstance();
            runLogCheck(newLogContents, options, julti);
        }
    }

    public boolean shouldDirtCover() {
        return dirtCover;
    }

    public boolean hasPreviewEverStarted() {
        return lastPreviewStart != -1L;
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

    synchronized private void runLogCheck(String newLogContents, JultiOptions options, final Julti julti) {
        if (!newLogContents.isEmpty()) {
            for (String line : newLogContents.split("\n")) {
                line = line.trim();
                if (!options.autoResetForBeach && startPreviewPattern.matcher(line).matches()) {
                    setInPreview(true);
                    worldLoaded = false;
                    if (options.useF3) {
                        pressF3Esc();
                    }
                    julti.getResetManager().notifyPreviewLoaded(this);
                } else if (options.autoResetForBeach && startPreviewWithBiomePattern.matcher(line).matches()) {
                    setInPreview(true);
                    worldLoaded = false;
                    if (options.useF3) {
                        pressF3Esc();
                    }
                    String[] args = line.split(" ");
                    biome = args[args.length - 1];
                    julti.getResetManager().notifyPreviewLoaded(this);
                } else if (advancementsLoadedPattern.matcher(line).matches()) {
                    setInPreview(false);
                    worldLoaded = true;
                    // Check loading percent progress before removing dirt cover in case of badly timed reset
                    if (loadingPercent > 50) {
                        dirtCover = false;
                    }
                    boolean active = isActive();
                    if (options.pauseOnLoad && (!active || !options.unpauseOnSwitch)) {
                        if (options.useF3) {
                            pressF3Esc();
                        } else {
                            pressEsc();
                        }
                    } else if (active) {
                        if (options.coopMode)
                            openToLan(!options.unpauseOnSwitch);
                        if (options.useFullscreen)
                            pressFullscreenKey();
                    }
                    julti.getResetManager().notifyWorldLoaded(this);
                } else if (isPreviewLoaded() && spawnAreaPattern.matcher(line).matches()) {
                    String[] args = line.split(" ");
                    try {
                        loadingPercent = Integer.parseInt(args[args.length - 1].replace("%", ""));
                    } catch (Exception ignored) {
                    }
                    if (loadingPercent >= JultiOptions.getInstance().dirtReleasePercent) {
                        dirtCover = false;
                    }
                } else if ((!options.coopMode) && options.noCopeMode && openToLanPattern.matcher(line).matches()) {
                    julti.getResetManager().doReset();
                }
            }
        }
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
                InstanceManager.log(Level.INFO, (100 * i / total) + "%");
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
