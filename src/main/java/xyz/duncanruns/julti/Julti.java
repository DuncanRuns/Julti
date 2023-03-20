package xyz.duncanruns.julti;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sun.jna.platform.win32.PsapiUtil;
import com.sun.jna.platform.win32.WinDef.HWND;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.duncanruns.julti.cancelrequester.CancelRequester;
import xyz.duncanruns.julti.cancelrequester.CancelRequesters;
import xyz.duncanruns.julti.command.*;
import xyz.duncanruns.julti.instance.InstanceManager;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.resetting.*;
import xyz.duncanruns.julti.script.ScriptHotkeyData;
import xyz.duncanruns.julti.script.ScriptManager;
import xyz.duncanruns.julti.util.*;
import xyz.duncanruns.julti.win32.Win32Con;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static xyz.duncanruns.julti.util.SleepUtil.sleep;

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
            new WaitCommand(),
            new LogCommand(),
            new PlaysoundCommand(),
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
        this.storeLastInstances();
        JultiOptions.getInstance().trySave();
        JultiOptions.changeProfile(newName);
        log(Level.INFO, "Switched to profile \"" + newName + "\"");
        this.reloadManagers();
        this.setupHotkeys();
        ResetCounter.updateFile();
    }

    public void storeLastInstances() {
        List<String> instanceStrings = new ArrayList<>();
        for (MinecraftInstance instance : this.instanceManager.getInstances()) {
            instanceStrings.add(instance.getInstancePath().toString());
        }
        JultiOptions.getInstance().lastInstances = Collections.unmodifiableList(instanceStrings);
    }

    public static void log(Level level, String message) {
        LOGGER.log(level, message);
        LogReceiver.receive(level, message);
    }

    public void reloadManagers() {
        this.reloadInstanceManager();
        this.reloadResetManager();
    }

    public void setupHotkeys() {
        HotkeyUtil.stopGlobalHotkeyChecker();
        HotkeyUtil.clearGlobalHotkeys();

        JultiOptions options = JultiOptions.getInstance();

        HotkeyUtil.addGlobalHotkey(options.getHotkeyFromSetting("wallResetHotkey"), () -> {
            this.playActionSounds(this.resetManager.doWallFullReset());
        });
        HotkeyUtil.addGlobalHotkey(options.getHotkeyFromSetting("wallSingleResetHotkey"), () -> {
            this.playActionSounds(this.resetManager.doWallSingleReset());
        });
        HotkeyUtil.addGlobalHotkey(options.getHotkeyFromSetting("wallLockHotkey"), () -> {
            this.playActionSounds(this.resetManager.doWallLock());
        });
        HotkeyUtil.addGlobalHotkey(options.getHotkeyFromSetting("wallPlayHotkey"), () -> {
            this.playActionSounds(this.resetManager.doWallPlay());
        });
        HotkeyUtil.addGlobalHotkey(options.getHotkeyFromSetting("wallFocusResetHotkey"), () -> {
            this.playActionSounds(this.resetManager.doWallFocusReset());
        });
        HotkeyUtil.addGlobalHotkey(options.getHotkeyFromSetting("wallPlayLockHotkey"), () -> {
            this.playActionSounds(this.resetManager.doWallPlayLock());
        });

        HotkeyUtil.addGlobalHotkey(options.getHotkeyFromSetting("resetHotkey"), () -> {
            this.playActionSounds(this.resetManager.doReset());
        });
        HotkeyUtil.addGlobalHotkey(options.getHotkeyFromSetting("bgResetHotkey"), () -> {
            this.playActionSounds(this.resetManager.doBGReset());
        });

        HotkeyUtil.addGlobalHotkey(options.getHotkeyFromSetting("cancelScriptHotkey"), ScriptManager::requestCancel);

        this.setupScriptHotkeys();

        HotkeyUtil.startGlobalHotkeyChecker();
    }

    /**
     * Replaces the instanceManager in this Julti object with a new one based on saved instance paths.
     */
    private void reloadInstanceManager() {
        this.instanceManager = new InstanceManager(JultiOptions.getInstance().getLastInstancePaths());
    }

    private void reloadResetManager() {
        switch (JultiOptions.getInstance().resetMode) {
            case 0:
                this.resetManager = new MultiResetManager(this);
                break;
            case 1:
                this.resetManager = new WallResetManager(this);
                break;
            case 2:
                this.resetManager = new DynamicWallResetManager(this);
                break;
        }
    }

    private void playActionSounds(List<ActionResult> actionResults) {
        JultiOptions options = JultiOptions.getInstance();

        // Reset Sounds
        int instancesReset = (int) actionResults.stream().filter(actionResult -> actionResult.equals(ActionResult.INSTANCE_RESET)).count();
        if (instancesReset > 1) {
            SoundUtil.playSound(options.multiResetSound, options.multiResetVolume);
        } else if (instancesReset == 1) {
            SoundUtil.playSound(options.singleResetSound, options.singleResetVolume);
        }

        // Lock Sound
        if (actionResults.contains(ActionResult.INSTANCE_LOCKED)) {
            SoundUtil.playSound(options.lockSound, options.lockVolume);
        }

        // Play Sound
        if (actionResults.contains(ActionResult.INSTANCE_ACTIVATED)) {
            SoundUtil.playSound(options.playSound, options.playVolume);
        }
    }

    private void setupScriptHotkeys() {
        JultiOptions options = JultiOptions.getInstance();


        final List<String> scriptNames = ScriptManager.getScriptNames();
        // Remove any hotkeys of non-existent scripts
        options.scriptHotkeys.removeIf(s -> {
            ScriptHotkeyData scriptHotkeyData = ScriptHotkeyData.parseString(s);
            if (scriptHotkeyData == null) {
                return true;
            }
            return !scriptNames.contains(scriptHotkeyData.scriptName);
        });
        options.scriptHotkeys.stream().map(ScriptHotkeyData::parseString).filter(Objects::nonNull).filter(data -> data.keys.size() > 0).forEach(data -> {
            HotkeyUtil.addGlobalHotkey(data.ignoreModifiers ? new HotkeyUtil.HotkeyIM(data.keys) : new HotkeyUtil.Hotkey(data.keys), () -> {
                boolean instanceActive = this.instanceManager.getSelectedInstance() != null;
                boolean wallActive = !instanceActive && this.isWallActive();
                if ((!instanceActive) && (!wallActive)) {
                    return;
                }
                ScriptManager.runScript(this, data.scriptName, true, (byte) (instanceActive ? 1 : 2));
            });
        });
    }

    public boolean isWallActive() {
        HWND currentHwnd = HwndUtil.getCurrentHwnd();
        log(Level.DEBUG, "Running isWallActive(): currentHwnd=" + currentHwnd);
        return HwndUtil.isOBSWallHwnd(JultiOptions.getInstance().obsWindowNameFormat, currentHwnd);
    }

    public void redetectInstances() {
        log(Level.INFO, "Redetecting Instances...");
        this.instanceManager.redetectInstances();
        this.reloadInstancePositions();
        log(Level.INFO, this.instanceManager.getInstances().size() + " instances found.");
        this.storeLastInstances();
    }

    public void reloadInstancePositions() {
        this.instanceManager.getInstances().forEach(instance -> new Thread(instance::ensureWindowState, "position-reloader").start());
    }

    public void switchScene(MinecraftInstance instance) {
        this.switchScene(this.getInstanceManager().getInstanceNum(instance));
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
        this.currentLocation = String.valueOf(i);
    }

    public InstanceManager getInstanceManager() {
        return this.instanceManager;
    }

    public void start() {
        generateResources();
        this.stopExecutors();
        ScriptManager.reload();
        this.reloadManagers();
        if (JultiOptions.getInstance().useAffinity) {
            AffinityManager.start(this);
        }
        this.setupHotkeys();
        this.tryOutputLSInfo();
        this.tickExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("julti").build());
        this.tryTick();
        this.tickExecutor.scheduleWithFixedDelay(this::tryTick, 25, 50, TimeUnit.MILLISECONDS);
        this.stateExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("julti").build());
        this.stateExecutor.scheduleWithFixedDelay(this::stateTick, 10, 20, TimeUnit.MILLISECONDS);
        log(Level.INFO, "Welcome to Julti!");
        String libraryPathThing = System.getProperty("java.library.path");
        log(Level.INFO, "You are running Julti v" + VERSION + " with java: " + libraryPathThing.substring(0, libraryPathThing.indexOf(";")));
    }

    private static void generateResources() {
        JultiOptions.ensureJultiDir();
        JultiOptions.getJultiDir().resolve("sounds").toFile().mkdirs();

        String[] filesToCopy = {
                "dirtcover.png",
                "lock.png",
                "blacksmith_example.png",
                "beach_example.png",
                "sounds/click.wav",
                "sounds/plop.wav"
        };

        for (String name : filesToCopy) {
            try {
                Path dest = JultiOptions.getJultiDir().resolve(name);
                if (dest.toFile().exists()) {
                    continue;
                }
                ResourceUtil.copyResourceToFile("/" + name, dest);
                log(Level.INFO, "Generated .Julti file " + name);
            } catch (Exception e) {
                log(Level.ERROR, "Failed to copy resource (" + e.getClass().getSimpleName() + "):\n" + e);
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
                log(Level.ERROR, "Failed to copy resource (" + e.getClass().getSimpleName() + "):\n" + e);
            }
        }
    }

    private void stopExecutors() {
        if (this.tickExecutor != null) {
            try {
                this.tickExecutor.shutdownNow();
            } catch (Exception ignored) {
            }
        }
        if (this.stateExecutor != null) {
            try {
                this.stateExecutor.shutdownNow();
            } catch (Exception ignored) {
            }
        }
    }

    public void tryOutputLSInfo() {
        JultiOptions options = JultiOptions.getInstance();
        List<MinecraftInstance> instances = this.instanceManager.getInstances();

        if (instances.size() == 0) {
            return;
        }

        MinecraftInstance instance = instances.get(0);
        for (MinecraftInstance instanceCandidate : instances) {
            if (instanceCandidate.hasWindow()) {
                instance = instanceCandidate;
                break;
            }
        }

        if (!instance.hasWindowQuick() && !options.useBorderless && options.wideResetSquish == 1f) {
            log(Level.WARN, "Could not output verification cropping info because maximized windows are being used for resetting. Because the taskbar be different sizes depending on the user, an instance must first be opened in order to determine its window size while resetting.");
            return;
        }

        int width = options.windowSize[0];
        int height = options.windowSize[1];
        if (options.wideResetSquish != 1f) {
            height = (int) (height / options.wideResetSquish);
        }

        if (!options.useBorderless) {
            if (options.wideResetSquish == 1f) {
                instance.ensureWindowState(true, false);
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
            FileUtil.writeString(JultiOptions.getJultiDir().resolve("loadingsquarecrop"), out + "," + out2);
        } catch (Exception ignored) {
        }
    }

    private void tryTick() {
        try {
            this.tick();
        } catch (Exception e) {
            log(Level.ERROR, "Error during tick:\n" + e);
        }
    }

    private void stateTick() {
        int i = 0;
        List<MinecraftInstance> instances = this.instanceManager.getInstances();
        Thread[] threads = new Thread[instances.size()];
        for (MinecraftInstance instance : instances) {
            Thread thread = new Thread(() -> {
                try {
                    instance.checkLog(this);
                } catch (Exception e) {
                    log(Level.ERROR, "Error while checking log for " + instance.getName() + ":\n" + e);
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
        this.tryOutputState();
    }


    private void tick() {
        long current = System.currentTimeMillis();
        if (current - this.last2SecCycle > 2000) {
            this.last2SecCycle = current;

            this.manageMissingInstances();

            MinecraftInstance selectedInstance = this.getInstanceManager().getSelectedInstance();
            this.ensureCorrectSceneState(selectedInstance);
            this.ensureSleepBG(selectedInstance);

            // We don't have permission to write to the obs scripts folder unfortunately, so I'll leave this commented out
            // ensureScriptPlaced();

            this.resetManager.tick();
        }
    }

    private void tryOutputState() {
        JultiOptions options = JultiOptions.getInstance();
        // Lazy try except (I sorry)
        try {
            StringBuilder out = new StringBuilder(this.currentLocation);
            //(lockedInstances.contains(instance) ? 1 : 0) + (resetManager.shouldDirtCover(instance) ? 2 : 0)
            Dimension size = this.getOBSSceneSize();
            if (size == null) {
                size = new Dimension(options.windowSize[0], options.windowSize[1]);
            }
            List<MinecraftInstance> lockedInstances = this.resetManager.getLockedInstances();
            for (MinecraftInstance instance : this.instanceManager.getInstances()) {
                Rectangle instancePos = this.resetManager.getInstancePosition(instance, size);
                instancePos = new Rectangle(instancePos.x + options.instanceSpacing, instancePos.y + options.instanceSpacing, instancePos.width - (2 * options.instanceSpacing), instancePos.height - (2 * options.instanceSpacing));
                out.append(";")
                        .append((lockedInstances.contains(instance) ? 1 : 0) + (this.resetManager.shouldDirtCover(instance) ? 2 : 0))
                        .append(",")
                        .append(instancePos.x)
                        .append(",")
                        .append(instancePos.y)
                        .append(",")
                        .append(instancePos.width)
                        .append(",")
                        .append(instancePos.height);
            }
            FileUtil.writeString(stateOutputPath, out.toString());
        } catch (Exception ignored) {
        }
    }

    private void manageMissingInstances() {
        if (this.instanceManager.manageMissingInstances(this::onInstanceLoad)) {
            this.resetManager.onMissingInstancesUpdate();
            this.tryOutputLSInfo();
        }
    }

    private void ensureCorrectSceneState(MinecraftInstance selectedInstance) {
        if (selectedInstance == null) {
            if (this.isWallActiveQuick()) {
                this.currentLocation = "W";
            }
        } else {
            this.currentLocation = String.valueOf(this.getInstanceManager().getInstanceNum(selectedInstance));
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
        if (this.obsSceneSize != null) {
            return new Dimension(this.obsSceneSize);
        }

        Path scriptSizeOutPath = JultiOptions.getJultiDir().resolve("obsscenesize");
        if (Files.exists(scriptSizeOutPath)) {
            try {
                String[] args = FileUtil.readString(scriptSizeOutPath).trim().split(",");
                this.obsSceneSize = new Dimension(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
            } catch (Exception ignored) {
            }
        }
        if (this.obsSceneSize != null) {
            return new Dimension(this.obsSceneSize);
        }
        return null;
    }

    private void onInstanceLoad(MinecraftInstance instance) {
        instance.ensureWindowState();
        instance.ensureGoodStandardSettings();
        instance.activate(this.instanceManager.getInstanceNum(instance));
        this.instanceManager.renameWindows();
    }

    public boolean isWallActiveQuick() {
        return HwndUtil.obsWallCheckActiveQuick(JultiOptions.getInstance().obsWindowNameFormat);
    }

    public Rectangle getWallBounds() {
        JultiOptions options = JultiOptions.getInstance();
        HWND obsWallHwnd = HwndUtil.getOBSWallHwnd(options.obsWindowNameFormat);
        if (obsWallHwnd == null) {
            return null;
        }
        return HwndUtil.getHwndRectangle(obsWallHwnd);
    }

    private void ensureScriptPlaced() {
        if (this.foundOBS) {
            return;
        }
        int[] processes = PsapiUtil.enumProcesses();
        Path executablePath = null;
        for (int process : processes) {
            executablePath = Paths.get(HwndUtil.getProcessExecutable(process));
            String executablePathName = executablePath.getName(executablePath.getNameCount() - 1).toString();
            if (executablePathName.equals("obs64.exe") || executablePathName.equals("obs32.exe")) {
                this.foundOBS = true;
                break;
            }
        }
        if (!this.foundOBS) {
            return;
        }
        assert executablePath != null;
        Path scriptLocation = executablePath.getParent().getParent().getParent().resolve("data").resolve("obs-plugins").resolve("frontend-tools").resolve("scripts").resolve("julti-obs-link.lua");
        try {
            ResourceUtil.copyResourceToFile("/julti-obs-link.lua", scriptLocation);
        } catch (IOException e) {
            log(Level.ERROR, "Error while trying to copy link script to obs script folder, you can find the script in the .Julti folder or the documents folder");
        }
    }

    public void runCommand(final String commands) {
        this.runCommand(commands, CancelRequesters.NEVER_CANCEL_REQUESTER);
    }

    public void runCommand(final String commands, CancelRequester cancelRequester) {
        for (String command : commands.split(";")) {
            this.commandManager.runCommand(command, this, cancelRequester);
        }
    }

    public ResetManager getResetManager() {
        return this.resetManager;
    }

    public void stop() {
        this.stopExecutors();
        if (this.instanceManager != null) {
            this.storeLastInstances();
        }
        HotkeyUtil.stopGlobalHotkeyChecker();
        JultiOptions.getInstance().trySave();
        SleepBGUtil.disableLock();
        AffinityManager.stop();
        AffinityManager.release(this);
        this.stopped = true;
    }

    public boolean isStopped() {
        return this.stopped;
    }

    public void focusWall() {
        log(Level.DEBUG, "Attempting to focus wall...");
        JultiOptions options = JultiOptions.getInstance();
        SleepBGUtil.disableLock();
        HWND hwnd = HwndUtil.getOBSWallHwnd(options.obsWindowNameFormat);
        if (hwnd == null) {
            log(Level.WARN, "No OBS Window found!");
            return;
        }
        HwndUtil.activateHwnd(hwnd);
        HwndUtil.maximizeHwnd(hwnd);
        this.switchToWallScene();
    }

    public void switchToWallScene() {
        JultiOptions options = JultiOptions.getInstance();
        if (options.obsPressHotkeys) {
            KeyboardUtil.releaseAllModifiers();
            KeyboardUtil.pressKeysForTime(options.switchToWallHotkey, 100);
        }
        this.currentLocation = "W";
    }

    /**
     * Replaces each MinecraftInstance object currently loaded with a new one only containing the instance path.
     */
    public void resetInstanceData() {
        this.instanceManager.resetInstanceData();
        this.manageMissingInstances();
    }
}