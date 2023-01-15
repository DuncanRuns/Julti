package xyz.duncanruns.julti;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.PsapiUtil;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.duncanruns.julti.instance.InstanceManager;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.resetting.DynamicWallResetManager;
import xyz.duncanruns.julti.resetting.MultiResetManager;
import xyz.duncanruns.julti.resetting.ResetManager;
import xyz.duncanruns.julti.resetting.WallResetManager;
import xyz.duncanruns.julti.util.*;
import xyz.duncanruns.julti.win32.User32;
import xyz.duncanruns.julti.win32.Win32Con;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Julti {
    public static final String VERSION = getVersion();
    private static final Logger LOGGER = LogManager.getLogger("InstanceManager");
    private static final Path stateOutputPath = JultiOptions.getJultiDir().resolve("state");

    private InstanceManager instanceManager = null;
    private ResetManager resetManager = null;
    private ScheduledExecutorService tickExecutor = null;
    private ScheduledExecutorService stateExecutor = null;
    private long last2SecCycle = 0;
    private boolean stopped = false;
    private boolean foundOBS = false;
    private String currentLocation = "W";
    private final HashMap<String, Consumer<String[]>> commandMap = getCommandMap();
    private Dimension obsSceneSize = null;

    private static String getVersion() {
        // Thanks to answers from this: https://stackoverflow.com/questions/33020069/how-to-get-version-attribute-from-a-gradle-build-to-be-included-in-runtime-swing
        String ver = Julti.class.getPackage().getImplementationVersion();
        if (ver == null) {
            return "DEV";
        }
        return ver;
    }

    private static void sleep(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
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

    private HashMap<String, Consumer<String[]>> getCommandMap() {
        HashMap<String, Consumer<String[]>> map = new HashMap<>();
        map.put("redetect", this::runCommandRedetect);
        map.put("reset", this::runCommandReset);
        map.put("close", this::runCommandClose);
        map.put("activate", this::runCommandActivate);
        map.put("list", this::runCommandList);
        map.put("help", this::runCommandHelp);
        map.put("option", this::runCommandOption);
        map.put("remove", this::runCommandRemove);
        map.put("hotkey", this::runCommandHotkey);
        map.put("titles", this::runCommandTitles);
        map.put("profile", this::runCommandProfile);
        map.put("sorting", this::runCommandSorting);
        map.put("topmost", this::runCommandTopmost);
        return map;
    }

    private void runCommandTopmost(String[] strings) {
        User32.INSTANCE.SetWindowPos(HwndUtil.getCurrentHwnd(), new Pointer(-1), 0, 0, 800, 420, null);
    }

    private String combineArgs(String[] args) {
        StringBuilder out = new StringBuilder();
        for (String arg : args) {
            if (!out.toString().isEmpty()) {
                out.append(" ");
            }
            out.append(arg);
        }
        return out.toString();
    }

    private void runCommandProfile(String[] args) {
        if (args.length == 0) {
            log(Level.INFO, "Current options profile: " + JultiOptions.getInstance().getProfileName());
        } else if ("switch".equals(args[0])) {
            changeProfile(combineArgs(withoutFirst(args)));
        } else if ("duplicate".equals(args[0])) {
            String newName = combineArgs(withoutFirst(args));
            JultiOptions.getInstance().copyTo(newName);
            changeProfile(newName);
        } else {
            log(Level.ERROR, "No args given to profile command!");
        }
    }


    private void runCommandSorting(String[] args) {
        for (MinecraftInstance instance : instanceManager.getInstances()) {
            log(Level.INFO, instance.getName() + " -> " + instance.getNameSortingNum());
        }
    }

    private void runCommandTitles(String[] args) {
        getInstanceManager().renameWindows();
    }

    private void runCommandHotkey(final String[] args) {
        List<String> setHotkeyArgs = Arrays.asList("reset", "bgreset", "wallreset", "walllock", "wallplay", "wallfocusreset", "wallsinglereset");
        JultiOptions options = JultiOptions.getInstance();
        if (args.length == 0) {
            log(Level.ERROR, "No args given to hotkey command!");
        } else if ("list".equals(args[0])) {
            String out = "Hotkeys:\n" +
                    "Reset All (Wall): " + HotkeyUtil.formatKeys(options.wallResetHotkey) + "\n" +
                    "Reset Single (Wall): " + HotkeyUtil.formatKeys(options.wallSingleResetHotkey) + "\n" +
                    "Lock Instance (Wall): " + HotkeyUtil.formatKeys(options.wallLockHotkey) + "\n" +
                    "Play Instance (Wall): " + HotkeyUtil.formatKeys(options.wallPlayHotkey) + "\n" +
                    "Focus Reset (Wall): " + HotkeyUtil.formatKeys(options.wallFocusResetHotkey) + "\n" +
                    "Reset: " + HotkeyUtil.formatKeys(options.resetHotkey) + "\n" +
                    "Background Reset: " + HotkeyUtil.formatKeys(options.bgResetHotkey);
            log(Level.INFO, out);
        } else if (setHotkeyArgs.contains(args[0])) {
            log(Level.INFO, "Waiting 1 second to not pick up accidental keypress...");
            sleep(1000);
            log(Level.INFO, "Please press your desired hotkey.");
            HotkeyUtil.onNextHotkey(() -> true, hotkey -> {
                Thread.currentThread().setName("hotkeys");
                JultiOptions jultiOptions = JultiOptions.getInstance();
                switch (args[0]) {
                    case "reset":
                        jultiOptions.resetHotkey = hotkey.getKeys();
                        break;
                    case "bgreset":
                        jultiOptions.bgResetHotkey = hotkey.getKeys();
                        break;
                    case "wallreset":
                        jultiOptions.wallResetHotkey = hotkey.getKeys();
                        break;
                    case "wallsinglereset":
                        jultiOptions.wallSingleResetHotkey = hotkey.getKeys();
                        break;
                    case "walllock":
                        jultiOptions.wallLockHotkey = hotkey.getKeys();
                        break;
                    case "wallplay":
                        jultiOptions.wallPlayHotkey = hotkey.getKeys();
                        break;
                    case "wallfocusreset":
                        jultiOptions.wallFocusResetHotkey = hotkey.getKeys();
                        break;
                }
                log(Level.INFO, "Hotkey Set.");
                hotkey.wasPressed();
                setupHotkeys();
            });
        } else {
            log(Level.ERROR, "Unknown arg \"" + args[0] + "\".");
        }
    }

    private void runCommandRemove(String[] args) {
        int index = indexFromArg(args[0]);
        if (index != -1)
            instanceManager.removeInstanceByIndex(index);
    }

    private void runCommandRedetect(String[] args) {
        redetectInstances();
    }

    private void runCommandReset(String[] args) {
        List<MinecraftInstance> instances = getInstanceManager().getInstances();
        if (args.length == 0) {
            if (!resetManager.doReset()) {
                log(Level.ERROR, "No instances to reset / No instance selected.");
            }
        } else {
            final String input = args[0];
            if ("all".equals(input)) {
                for (MinecraftInstance instance : instances) {
                    instance.reset(instances.size() == 1);
                }
            } else if ("random".equals(input)) {
                instances.get(new Random().nextInt(instances.size())).reset(instances.size() == 1);
            } else if ("unselected".equals(input)) {
                resetManager.doBGReset();
            } else {
                int index = indexFromArg(input);
                if (index != -1)
                    instances.get(index).reset(instances.size() == 1);
            }
        }
    }

    private void runCommandClose(String[] args) {
        List<MinecraftInstance> instances = getInstanceManager().getInstances();
        if (args.length == 0) {
            log(Level.ERROR, "No args given to close command!");
        } else {
            final String input = args[0];
            if ("all".equals(input)) {
                for (MinecraftInstance instance : instances) {
                    instance.closeWindow();
                }
            } else if ("random".equals(input)) {
                instances.get(new Random().nextInt(instances.size())).closeWindow();
            } else {
                int index = indexFromArg(input);
                if (index != -1)
                    instances.get(index).closeWindow();
            }
        }
    }

    private void runCommandActivate(String[] args) {
        List<MinecraftInstance> instances = getInstanceManager().getInstances();
        if (args.length == 0) {
            log(Level.ERROR, "No args given to activate command!");
        } else {
            final String input = args[0];
            if ("random".equals(input)) {
                MinecraftInstance toActivate = instances.get(new Random().nextInt(instances.size()));
                toActivate.activate(instances.indexOf(toActivate) + 1);
            } else {
                int index = indexFromArg(input);
                if (index != -1) {
                    MinecraftInstance toActivate = instances.get(index);
                    toActivate.activate(instances.indexOf(toActivate) + 1);
                    switchScene(index + 1);
                }
            }
        }
    }

    private void runCommandList(String[] args) {
        int i = 0;
        for (MinecraftInstance instance : getInstanceManager().getInstances()) {
            log(Level.INFO, (++i) + ": " + instance.getName() + " - " + instance.getInstancePath());
        }
    }

    private void runCommandHelp(String[] args) {
        log(Level.INFO, "\n" +
                "--------------------\n" +
                "Commands:\n" +
                "\n" +
                "help -> Shows all commands\n" +
                "\n" +
                "redetect -> Sets current instances to the opened Minecraft instances\n" +
                "\n" +
                "remove <#> -> Removes a single instance from the current instances\n" +
                "\n" +
                "reset -> Functions the same as the reset hotkey\n" +
                "reset [all/random/unselected/#] -> Resets specified instance(s)\n" +
                "\n" +
                "close <all/random/#> -> Closes a specific / all instance(s)\n" +
                "\n" +
                "activate <random/#> -> Activates a specific instance\n" +
                "\n" +
                "list -> Lists all opened instances\n" +
                "\n" +
                "titles -> Set window titles to \"Minecraft* - Instance #\".\n" +
                "\n" +
                "profile -> States the current options profile\n" +
                "profile switch <profile name> -> Switch to another profile, or create a new one if it does not exist\n" +
                "profile duplicate <profile name> -> Duplicate the current options into another profile\n" +
                "\n" +
                "option -> Lists all options\n" +
                "option [option] -> Gets the current value of an option and gives an example to set it\n" +
                "option [option] [value] -> Sets the value of the option to the specified value\n" +
                "option open -> Opens the current options json file\n" +
                "option reload -> Reloads the current options json file\n" +
                "\n" +
                "hotkey list -> List all hotkeys.\n" +
                "hotkey <reset/bgreset/wallreset/wallsinglereset/walllock/wallplay/wallfocusreset> -> Rebinds a hotkey. After running the command, press the wanted hotkey for the chosen function\n" +
                "\n" +
                "sorting -> List all sorting numbers for the current instances.\n" +
                "--------------------"
        );
    }

    private void runCommandOption(String[] args) {
        JultiOptions options = JultiOptions.getInstance();
        if (args.length == 1 && args[0].equals("open")) {
            try {
                Desktop.getDesktop().open(JultiOptions.getSelectedProfilePath().toFile());
            } catch (IOException e) {
                log(Level.ERROR, "Error opening options file:\n" + e.getMessage());
            }
        } else if (args.length == 1 && args[0].equals("reload")) {
            JultiOptions.getInstance(true);
        } else if (args.length == 1) {
            String optionName = args[0];
            String value = options.getValueString(optionName);
            if (value == null) {
                log(Level.WARN, "Option \"" + optionName + "\" does not exist. ");
            } else {
                log(Level.INFO, "Option \"" + optionName + "\" has a value of: " + value);
            }
        } else if (args.length > 1) {
            String[] valueArgs = withoutFirst(args);
            String all = combineArgs(valueArgs);
            String optionName = args[0];
            if (options.trySetValue(optionName, all)) {
                log(Level.INFO, "Set \"" + optionName + "\" to " + options.getValueString(optionName) + ".");
            } else {
                log(Level.ERROR, "Could not set value.");
            }
        } else {
            StringBuilder optionNames = new StringBuilder();
            for (String optionName : options.getOptionNamesWithType()) {
                if (!optionNames.toString().isEmpty()) {
                    optionNames.append("\n");
                }
                optionNames.append("- ").append(optionName);
            }
            log(Level.INFO, "All available options:\n" + optionNames);
        }
    }

    private int indexFromArg(final String num) {
        List<MinecraftInstance> instances = getInstanceManager().getInstances();
        if (num.startsWith("~")) {
            // parseInt can handle "1", "-1", and "+1"
            int offset = Integer.parseInt(num.substring(1));

            MinecraftInstance selectedInstance = instanceManager.getSelectedInstance();
            if (selectedInstance == null) {
                return -1;
            }
            // index of current instance + offset wrapped around into instances size range
            return (instances.indexOf(selectedInstance) + (offset)) % instances.size();
        } else {
            // not relative
            return Integer.parseInt(num) - 1;
        }
    }

    public void redetectInstances() {
        log(Level.INFO, "Redetecting Instances...");
        instanceManager.redetectInstances();
        reloadInstancePositions();
        log(Level.INFO, instanceManager.getInstances().size() + " instances found.");
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

    public static void log(Level level, String message) {
        LOGGER.log(level, message);
        LogReceiver.receive(level, message);
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
        tickExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("julti").build());
        tryTick();
        tickExecutor.scheduleWithFixedDelay(this::tryTick, 25, 50, TimeUnit.MILLISECONDS);
        stateExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("julti").build());
        stateExecutor.scheduleWithFixedDelay(this::stateTick, 10, 20, TimeUnit.MILLISECONDS);
        log(Level.INFO, "Welcome to Julti v" + VERSION + "!");
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
            });
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

    private void tick() {
        JultiOptions options = JultiOptions.getInstance();
        long current = System.currentTimeMillis();
        if (current - last2SecCycle > 2000) {
            last2SecCycle = current;

            if (instanceManager.manageMissingInstances(this::onInstanceLoad)) {
                resetManager.onMissingInstancesUpdate();
            }

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

    private void onInstanceLoad(MinecraftInstance minecraftInstance) {
        minecraftInstance.ensureWindowState();
        minecraftInstance.ensureGoodStandardSettings();
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

    public boolean isWallActiveQuick() {
        return HwndUtil.isSavedObsActive();
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
            command = command.trim();
            String[] args = command.split(" ");
            if (args.length == 0 || "".equals(args[0].trim())) {
                return;
            }
            Consumer<String[]> commandConsumer = commandMap.getOrDefault(args[0], null);
            if (commandConsumer == null) {
                log(Level.ERROR, "Unknown Command \"" + command + "\"");
            } else {
                try {
                    commandConsumer.accept(withoutFirst(args));
                } catch (Exception e) {
                    log(Level.ERROR, "Error while running command \"" + command + "\":\n" + e.getMessage());
                }
            }
        }
    }

    private static String[] withoutFirst(String[] args) {
        return Arrays.copyOfRange(args, 1, args.length);
    }

    public ResetManager getResetManager() {
        return resetManager;
    }

    public void reloadInstancePositions() {
        instanceManager.getInstances().forEach(MinecraftInstance::ensureWindowState);
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

    public void storeLastInstances() {
        List<String> instanceStrings = new ArrayList<>();
        for (MinecraftInstance instance : instanceManager.getInstances()) {
            instanceStrings.add(instance.getInstancePath().toString());
        }
        JultiOptions.getInstance().lastInstances = Collections.unmodifiableList(instanceStrings);
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
        instanceManager.manageMissingInstances(this::onInstanceLoad);
    }
}