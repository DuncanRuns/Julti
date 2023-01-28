package xyz.duncanruns.julti;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.PsapiUtil;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.duncanruns.julti.command.*;
import xyz.duncanruns.julti.instance.InstanceManager;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.resetting.DynamicWallResetManager;
import xyz.duncanruns.julti.resetting.MultiResetManager;
import xyz.duncanruns.julti.resetting.ResetManager;
import xyz.duncanruns.julti.resetting.WallResetManager;
import xyz.duncanruns.julti.util.*;
import xyz.duncanruns.julti.win32.Win32Con;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Julti {
    public static final String VERSION = getVersion();
    private static final Logger LOGGER = LogManager.getLogger("Julti");
    private static final Path stateOutputPath = JultiOptions.getJultiDir().resolve("state");

    private InstanceManager instanceManager = null;
    private ResetManager resetManager = null;
    private ScheduledExecutorService tickExecutor = null;
    private ScheduledExecutorService stateExecutor = null;
    private long last2SecCycle = 0;
    private boolean stopped = false;
    private boolean foundOBS = false;
    private String currentLocation = "W";
    private Dimension obsSceneSize = null;
    private final CommandManager commandManager = new CommandManager(new Command[]{
            new ResetCommand(),
            new LockCommand(),
            new LaunchCommand(),
            new CloseCommand(),
            new ActivateCommand(),
            new OptionCommand(),
            new SleepCommand(),
            new ChatMessageCommand(),
            new OpenToLanCommand(),
    });

    private static String getVersion() {
        // Thanks to answers from this: https://stackoverflow.com/questions/33020069/how-to-get-version-attribute-from-a-gradle-build-to-be-included-in-runtime-swing
        String ver = Julti.class.getPackage().getImplementationVersion();
        if (ver == null) {
            return "DEV";
        }
        return ver;
    }

    public void changeProfile(String newName) {
        storeLastInstances();
        JultiOptions.getInstance().trySave();
        JultiOptions.changeProfile(newName);
        log(Level.INFO, "Switched to profile \"" + newName + "\"");
        reloadManagers();
        setupHotkeys();
        ResetCounter.updateFile();
    }

    public void storeLastInstances() {
        List<String> instanceStrings = new ArrayList<>();
        for (MinecraftInstance instance : instanceManager.getInstances()) {
            instanceStrings.add(instance.getInstancePath().toString());
        }
        JultiOptions.getInstance().lastInstances = Collections.unmodifiableList(instanceStrings);
    }

    public static void log(Level level, String message) {
        LOGGER.log(level, message);
        LogReceiver.receive(level, message);
    }

    public void reloadManagers() {
        reloadInstanceManager();
        reloadResetManager();
    }

    public void setupHotkeys() {
        HotkeyUtil.stopGlobalHotkeyChecker();
        HotkeyUtil.clearGlobalHotkeys();

        JultiOptions options = JultiOptions.getInstance();

        HotkeyUtil.addGlobalHotkey(options.getHotkeyFromSetting("wallResetHotkey"), () -> resetManager.doWallFullReset());
        HotkeyUtil.addGlobalHotkey(options.getHotkeyFromSetting("wallSingleResetHotkey"), () -> resetManager.doWallSingleReset());
        HotkeyUtil.addGlobalHotkey(options.getHotkeyFromSetting("wallLockHotkey"), () -> resetManager.doWallLock());
        HotkeyUtil.addGlobalHotkey(options.getHotkeyFromSetting("wallPlayHotkey"), () -> resetManager.doWallPlay());
        HotkeyUtil.addGlobalHotkey(options.getHotkeyFromSetting("wallFocusResetHotkey"), () -> resetManager.doWallFocusReset());

        HotkeyUtil.addGlobalHotkey(options.getHotkeyFromSetting("resetHotkey"), () -> resetManager.doReset());
        HotkeyUtil.addGlobalHotkey(options.getHotkeyFromSetting("bgResetHotkey"), () -> resetManager.doBGReset());

        HotkeyUtil.startGlobalHotkeyChecker();
    }

    /**
     * Replaces the instanceManager in this Julti object with a new one based on saved instance paths.
     */
    private void reloadInstanceManager() {
        instanceManager = new InstanceManager(JultiOptions.getInstance().getLastInstancePaths());
    }

    private void reloadResetManager() {
        switch (JultiOptions.getInstance().resetMode) {
            case 0:
                resetManager = new MultiResetManager(this);
                break;
            case 1:
                resetManager = new WallResetManager(this);
                break;
            case 2:
                resetManager = new DynamicWallResetManager(this);
                break;
        }
    }

    public void redetectInstances() {
        log(Level.INFO, "Redetecting Instances...");
        instanceManager.redetectInstances();
        reloadInstancePositions();
        log(Level.INFO, instanceManager.getInstances().size() + " instances found.");
        storeLastInstances();
    }

    public void reloadInstancePositions() {
        instanceManager.getInstances().forEach(instance -> new Thread(instance::ensureWindowState, "position-reloader").start());
    }

    public void switchScene(MinecraftInstance instance) {
        switchScene(getInstanceManager().getInstances().indexOf(instance) + 1);
    }

    public void switchScene(final int i) {
        JultiOptions options = JultiOptions.getInstance();
        if (options.obsPressHotkeys) {
            if (i <= 9) {
                KeyboardUtil.releaseAllModifiers();
                int keyToPress = (options.obsUseNumpad ? KeyEvent.VK_NUMPAD0 : KeyEvent.VK_0) + i;
                List<Integer> keys = new ArrayList<>();
                if (options.obsUseAlt) {
                    keys.add(Win32Con.VK_MENU);
                }
                keys.add(keyToPress);
                KeyboardUtil.pressKeysForTime(keys, 100);
            } else {
                log(Level.ERROR, "Too many instances! Could not switch to a scene past 9.");
            }
        }
        currentLocation = String.valueOf(i);
    }

    public InstanceManager getInstanceManager() {
        return instanceManager;
    }

    public void switchToWallScene() {
        JultiOptions options = JultiOptions.getInstance();
        if (options.obsPressHotkeys) {
            KeyboardUtil.releaseAllModifiers();
            KeyboardUtil.pressKeysForTime(options.switchToWallHotkey, 100);
        }
        currentLocation = "W";
    }

    public void start() {
        generateResources();
        stopExecutors();
        reloadManagers();
        if (JultiOptions.getInstance().useAffinity)
            AffinityManager.start(this);
        setupHotkeys();
        tryOutputLSInfo();
        tickExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("julti").build());
        tryTick();
        tickExecutor.scheduleWithFixedDelay(this::tryTick, 25, 50, TimeUnit.MILLISECONDS);
        stateExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("julti").build());
        stateExecutor.scheduleWithFixedDelay(this::stateTick, 10, 20, TimeUnit.MILLISECONDS);
        log(Level.INFO, "Welcome to Julti!");
        String libraryPathThing = System.getProperty("java.library.path");
        log(Level.INFO, "You are running Julti v" + VERSION + " with java: " + libraryPathThing.substring(0, libraryPathThing.indexOf(";")));
    }

    private static void generateResources() {
        String[] filesToCopy = {
                "dirtcover.png",
                "lock.png",
                "blacksmith_example.png",
                "beach_example.png"
        };

        for (String name : filesToCopy) {
            try {
                Path dest = JultiOptions.getJultiDir().resolve(name);
                if (dest.toFile().exists()) continue;
                ResourceUtil.copyResourceToFile("/" + name, dest);
                log(Level.INFO, "Generated .Julti file " + name);
            } catch (Exception e) {
                log(Level.ERROR, "Failed to copy resource (" + e.getClass().getSimpleName() + "):\n" + e.getMessage() + "\n" + Arrays.toString(e.getStackTrace()));
            }
        }

        Path[] scriptLocations = {
                JultiOptions.getJultiDir().resolve("julti-obs-link.lua"),
                Paths.get(System.getProperty("user.home")).resolve("Documents").resolve("julti-obs-link.lua")
        };

        for (Path dest : scriptLocations) {
            try {
                String name = "julti-obs-link.lua";
                ResourceUtil.copyResourceToFile("/" + name, dest);
                log(Level.INFO, "Generated " + name + " file in " + dest.getName(dest.getNameCount() - 2));
            } catch (Exception e) {
                log(Level.ERROR, "Failed to copy resource (" + e.getClass().getSimpleName() + "):\n" + e.getMessage() + "\n" + Arrays.toString(e.getStackTrace()));
            }
        }
    }

    private void stopExecutors() {
        if (tickExecutor != null) {
            try {
                tickExecutor.shutdownNow();
            } catch (Exception ignored) {
            }
        }
        if (stateExecutor != null) {
            try {
                stateExecutor.shutdownNow();
            } catch (Exception ignored) {
            }
        }
    }

    public void tryOutputLSInfo() {
        JultiOptions options = JultiOptions.getInstance();
        List<MinecraftInstance> instances = instanceManager.getInstances();

        if (instances.size() == 0) return;

        MinecraftInstance instance = null;
        if (options.wideResetSquish > 1 || options.useBorderless) {
            instance = instances.get(0);
        } else {
            for (MinecraftInstance instanceCandidate : instances) {
                if (!instanceCandidate.hasWindow()) continue;
                instance = instanceCandidate;
                break;
            }
        }
        if (instance == null) return;

        int width = options.windowSize[0];
        int height = options.windowSize[1];
        if (options.wideResetSquish != 1f) {
            height = (int) (height / options.wideResetSquish);
        }

        if (!options.useBorderless) {
            if (options.wideResetSquish == 1f) {
                instance.ensureWindowState();
                sleep(50);
                Rectangle rect = instance.getWindowRectangle();
                width = rect.width;
                height = rect.height;
            }
            // These are Windows 10 border sizes
            // They may be inaccurate depending on the system (?)
            width -= 16;
            height -= 39;
        }

        int resettingGuiScale = instance.getResettingGuiScale(width, height);
        int loadingSquareSize = resettingGuiScale * 90;
        // extraHeight is for including the % loaded text above the loading square
        int extraHeight = resettingGuiScale * 19;

        // bottom left loading square: right crop, top crop
        String out = (width - loadingSquareSize) + "," + (height - (loadingSquareSize + extraHeight));
        // middle loading square: left/right crop, top crop, bottom crop
        String out2 = ((width - loadingSquareSize) / 2) + "," + (((height - loadingSquareSize) / 2 + (resettingGuiScale * 30)) - extraHeight) + "," + ((height - loadingSquareSize) / 2 - (resettingGuiScale * 30));

        try {
            Files.writeString(JultiOptions.getJultiDir().resolve("loadingsquarecrop"), out + "," + out2);
        } catch (Exception ignored) {
        }
    }

    private void tryTick() {
        try {
            tick();
        } catch (Exception e) {
            log(Level.ERROR, "Error during tick:" + e.getMessage());
        }
    }

    private void stateTick() {
        int i = 0;
        List<MinecraftInstance> instances = instanceManager.getInstances();
        Thread[] threads = new Thread[instances.size()];
        for (MinecraftInstance instance : instances) {
            Thread thread = new Thread(() -> {
                try {
                    instance.checkLog(this);
                } catch (Exception e) {
                    log(Level.ERROR, "Error while checking log for " + instance.getName() + ":\n" + e.getMessage() + " | " + Arrays.toString(e.getStackTrace()));
                }
            }, "state-checker");
            threads[i++] = thread;
            thread.start();
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException ignored) {
            }
        }
        tryOutputState();
    }

    private static void sleep(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }

    private void tick() {
        long current = System.currentTimeMillis();
        if (current - last2SecCycle > 2000) {
            last2SecCycle = current;

            manageMissingInstances();

            MinecraftInstance selectedInstance = getInstanceManager().getSelectedInstance();
            ensureCorrectSceneState(selectedInstance);
            ensureSleepBG(selectedInstance);

            // We don't have permission to write to the obs scripts folder unfortunately, so I'll leave this commented out
            // ensureScriptPlaced();

            resetManager.tick();
        }
    }

    private void tryOutputState() {
        JultiOptions options = JultiOptions.getInstance();
        // Lazy try except (I sorry)
        try {
            StringBuilder out = new StringBuilder(currentLocation);
            //(lockedInstances.contains(instance) ? 1 : 0) + (resetManager.shouldDirtCover(instance) ? 2 : 0)
            Dimension size = getOBSSceneSize();
            if (size == null) {
                size = new Dimension(options.windowSize[0], options.windowSize[1]);
            }
            List<MinecraftInstance> lockedInstances = resetManager.getLockedInstances();
            for (MinecraftInstance instance : instanceManager.getInstances()) {
                Rectangle instancePos = resetManager.getInstancePosition(instance, size);
                instancePos = new Rectangle(instancePos.x + options.instanceSpacing, instancePos.y + options.instanceSpacing, instancePos.width - (2 * options.instanceSpacing), instancePos.height - (2 * options.instanceSpacing));
                out.append(";")
                        .append((lockedInstances.contains(instance) ? 1 : 0) + (resetManager.shouldDirtCover(instance) ? 2 : 0))
                        .append(",")
                        .append(instancePos.x)
                        .append(",")
                        .append(instancePos.y)
                        .append(",")
                        .append(instancePos.width)
                        .append(",")
                        .append(instancePos.height);
            }
            Files.writeString(stateOutputPath, out.toString());
        } catch (Exception ignored) {
        }
    }

    private void manageMissingInstances() {
        if (instanceManager.manageMissingInstances(this::onInstanceLoad)) {
            resetManager.onMissingInstancesUpdate();
            tryOutputLSInfo();
        }
    }

    private void ensureCorrectSceneState(MinecraftInstance selectedInstance) {
        if (selectedInstance == null) {
            if (isWallActiveQuick()) {
                currentLocation = "W";
            }
        } else {
            currentLocation = String.valueOf(getInstanceManager().getInstances().indexOf(selectedInstance) + 1);
        }
    }

    private void ensureSleepBG(MinecraftInstance selectedInstance) {
        if (selectedInstance == null) {
            SleepBGUtil.disableLock();
        } else {
            SleepBGUtil.enableLock();
        }
    }

    public Dimension getOBSSceneSize() {
        if (obsSceneSize != null) return new Dimension(obsSceneSize);

        Path scriptSizeOutPath = JultiOptions.getJultiDir().resolve("obsscenesize");
        if (Files.exists(scriptSizeOutPath)) {
            try {
                String[] args = Files.readString(scriptSizeOutPath).trim().split(",");
                obsSceneSize = new Dimension(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
            } catch (Exception ignored) {
            }
        }
        if (obsSceneSize != null) return new Dimension(obsSceneSize);
        return null;
    }

    private void onInstanceLoad(MinecraftInstance minecraftInstance) {
        minecraftInstance.ensureWindowState();
        minecraftInstance.ensureGoodStandardSettings();
    }

    public boolean isWallActiveQuick() {
        return HwndUtil.obsWallCheckActiveQuick(JultiOptions.getInstance().obsWindowNameFormat);
    }

    public boolean isWallActive() {
        Pointer currentHwnd = HwndUtil.getCurrentHwnd();
        log(Level.DEBUG, "Running isWallActive(): currentHwnd=" + currentHwnd);
        return HwndUtil.isOBSWallHwnd(JultiOptions.getInstance().obsWindowNameFormat, currentHwnd);
    }

    public Rectangle getWallBounds() {
        JultiOptions options = JultiOptions.getInstance();
        Pointer obsWallHwnd = HwndUtil.getOBSWallHwnd(options.obsWindowNameFormat);
        if (obsWallHwnd == null) return null;
        return HwndUtil.getHwndRectangle(obsWallHwnd);
    }

    private void ensureScriptPlaced() {
        if (foundOBS) return;
        int[] processes = PsapiUtil.enumProcesses();
        Path executablePath = null;
        for (int process : processes) {
            executablePath = Path.of(HwndUtil.getProcessExecutable(process));
            String executablePathName = executablePath.getName(executablePath.getNameCount() - 1).toString();
            if (executablePathName.equals("obs64.exe") || executablePathName.equals("obs32.exe")) {
                foundOBS = true;
                break;
            }
        }
        if (!foundOBS) return;
        assert executablePath != null;
        Path scriptLocation = executablePath.getParent().getParent().getParent().resolve("data").resolve("obs-plugins").resolve("frontend-tools").resolve("scripts").resolve("julti-obs-link.lua");
        try {
            ResourceUtil.copyResourceToFile("/julti-obs-link.lua", scriptLocation);
        } catch (IOException e) {
            log(Level.ERROR, "Error while trying to copy link script to obs script folder, you can find the script in the .Julti folder or the documents folder");
        }
    }

    public void runCommand(final String commands) {
        for (String command : commands.split(";")) {
            commandManager.runCommand(command, this);
        }
    }

    public ResetManager getResetManager() {
        return resetManager;
    }

    public void stop() {
        stopExecutors();
        if (instanceManager != null) {
            storeLastInstances();
        }
        HotkeyUtil.stopGlobalHotkeyChecker();
        JultiOptions.getInstance().trySave();
        SleepBGUtil.disableLock();
        AffinityManager.stop();
        AffinityManager.release(this);
        stopped = true;
    }

    public boolean isStopped() {
        return stopped;
    }

    public void focusWall() {
        log(Level.DEBUG, "Attempting to focus wall...");
        JultiOptions options = JultiOptions.getInstance();
        SleepBGUtil.disableLock();
        Pointer hwnd = HwndUtil.getOBSWallHwnd(options.obsWindowNameFormat);
        if (hwnd == null) {
            log(Level.WARN, "No OBS Window found!");
            return;
        }
        HwndUtil.activateHwnd(hwnd);
        HwndUtil.maximizeHwnd(hwnd);
    }

    /**
     * Replaces each MinecraftInstance object currently loaded with a new one only containing the instance path.
     */
    public void resetInstanceData() {
        instanceManager.resetInstanceData();
        manageMissingInstances();
    }
}