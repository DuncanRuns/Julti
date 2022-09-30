package xyz.duncanruns.julti;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sun.jna.Pointer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.duncanruns.julti.instance.InstanceManager;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.options.JultiOptions;
import xyz.duncanruns.julti.util.HotkeyUtil;
import xyz.duncanruns.julti.util.HwndUtil;
import xyz.duncanruns.julti.util.KeyboardUtil;
import xyz.duncanruns.julti.win32.Win32Con;

import javax.annotation.Nullable;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Julti {
    private static final Logger LOGGER = LogManager.getLogger("InstanceManager");
    private static final String VERSION = getVersion();
    private InstanceManager instanceManager;
    private ScheduledExecutorService tickExecutor;
    private ScheduledExecutorService logCheckExecutor;
    private long last2SecCycle;
    private long lastWorldClear;
    private long lastAction;
    private Pointer selectedHwnd;
    private boolean hasHidden;
    private final HashMap<String, Consumer<String[]>> commandMap = getCommandMap();

    public Julti() {
        instanceManager = null;
        tickExecutor = null;
        logCheckExecutor = null;

        last2SecCycle = 0;
        lastWorldClear = 0;
        lastAction = 0;

        hasHidden = true;

        selectedHwnd = null;
    }

    private static String getVersion() {
        // Thanks to answers from this: https://stackoverflow.com/questions/33020069/how-to-get-version-attribute-from-a-gradle-build-to-be-included-in-runtime-swing
        String ver = Julti.class.getPackage().getImplementationVersion();
        if (ver == null) {
            return "DEV";
        }
        return ver;
    }

    public static void log(Level level, String message) {
        LOGGER.log(level, message);
    }

    private static void sleep(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }

    private HashMap<String, Consumer<String[]>> getCommandMap() {
        HashMap<String, Consumer<String[]>> map = new HashMap<>();
        map.put("redetect", this::runCommandRedetect);
        map.put("reset", this::runCommandReset);
        map.put("close", this::runCommandClose);
        map.put("activate", this::runCommandActivate);
        map.put("list", this::runCommandList);
        map.put("help", this::runCommandHelp);
        map.put("hide", this::runCommandHide);
        map.put("option", this::runCommandOption);
        return map;
    }

    private void runCommandRedetect(String[] args) {
        redetectInstances();
    }

    private void runCommandReset(String[] args) {
        List<MinecraftInstance> instances = getInstanceManager().getInstances();
        if (args.length == 0) {
            if (!reset()) {
                log(Level.ERROR, "No instances to reset / No instance selected.");
            }
        } else {
            final String input = args[0];
            if (Objects.equals(input, "all")) {
                for (MinecraftInstance instance : instances) {
                    instance.reset();
                }
            } else if (Objects.equals(input, "random")) {
                instances.get(new Random().nextInt(instances.size())).reset();
            } else if (Objects.equals(input, "background")) {
                backgroundReset();
            } else if (Objects.equals(input, "unselected")) {
                backgroundReset();
            } else {
                instances.get(Integer.parseInt(input) - 1).reset();
            }
        }
    }

    private void runCommandClose(String[] args) {
        List<MinecraftInstance> instances = getInstanceManager().getInstances();
        if (args.length == 0) {
            log(Level.ERROR, "No args given to close command!");
        } else {
            final String input = args[0];
            if (Objects.equals(input, "all")) {
                for (MinecraftInstance instance : instances) {
                    instance.closeWindow();
                }
            } else if (Objects.equals(input, "random")) {
                instances.get(new Random().nextInt(instances.size())).closeWindow();
            } else {
                instances.get(Integer.parseInt(input) - 1).closeWindow();
            }
        }
    }

    private void runCommandActivate(String[] args) {
        List<MinecraftInstance> instances = getInstanceManager().getInstances();
        if (args.length == 0) {
            log(Level.ERROR, "No args given to activate command!");
        } else {
            final String input = args[0];
            if (Objects.equals(input, "random")) {
                instances.get(new Random().nextInt(instances.size())).activate();
            } else {
                instances.get(Integer.parseInt(input) - 1).activate();
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
        log(Level.INFO, "Commands:" + "\nredetect -> Sets current instances to the opened Minecraft instances" + "\nreset -> Reset current instance and activate the next instance" + "\nreset [all/random/unselected/background/#] -> Resets instance(s)" + "\nclose [all/random/#] -> Closes a specific / all instance(s)" + "\nactivate [random/#] -> Activates a specific instance" + "\nlist -> Lists all opened instances" + "\nhelp -> Shows all commands" + "\nhide [all/random/unselected/#] -> Hides instance(s)" + "\noption -> Lists all options" + "\noption [option] -> Gets the current value of an option and gives an example to set it" + "\noption [option] [value] -> Sets the value of the option to the specified value");
    }

    private void runCommandHide(String[] args) {
        List<MinecraftInstance> instances = getInstanceManager().getInstances();
        if (args.length == 0) {
            MinecraftInstance selectedInstance = getSelectedInstance();
            if (selectedInstance == null) {
                log(Level.ERROR, "No instances to hide / No instance selected.");
            } else {
                for (MinecraftInstance instanceToHide : instances) {
                    if (!Objects.equals(selectedInstance, instanceToHide)) {
                        instanceToHide.setSizeZero();
                    }
                }
            }
        } else {
            final String input = args[0];
            if (Objects.equals(input, "all")) {
                for (MinecraftInstance instance : instances) {
                    instance.setSizeZero();
                }
            } else if (Objects.equals(input, "random")) {
                instances.get(new Random().nextInt(instances.size())).setSizeZero();
            } else if (Objects.equals(input, "unselected")) {
                MinecraftInstance selectedInstance = getSelectedInstance();
                for (MinecraftInstance instanceToHide : instances) {
                    if (!Objects.equals(selectedInstance, instanceToHide)) {
                        instanceToHide.setSizeZero();
                    }
                }
            } else {
                instances.get(Integer.parseInt(input) - 1).setSizeZero();
            }
            hasHidden = true;
        }
    }

    private void runCommandOption(String[] args) {
        if (args.length == 1 && args[0].equals("open")) {
            try {
                Desktop.getDesktop().open(JultiOptions.getSelectedProfilePath().toFile());
            } catch (IOException e) {
                log(Level.ERROR, "Error opening options file:\n" + e.getMessage());
            }
        } else if (args.length == 1 && args[0].equals("reload")) {
            JultiOptions.getInstance(true);
        } else {
            log(Level.WARN, "The option command is not yet implemented. Please change options in " + JultiOptions.getSelectedProfilePath() + "." + "\nYou can also run \"option open\" to open the file and \"option reload\" to reload them.");
        }
    }

    public void redetectInstances() {
        log(Level.INFO, "Redetecting Instances...");
        instanceManager.redetectInstances();
        for (MinecraftInstance instance : instanceManager.getInstances()) {
            onInstanceLoad(instance);
        }
        log(Level.INFO, instanceManager.getInstances().size() + " instances found.");
    }

    public InstanceManager getInstanceManager() {
        return instanceManager;
    }

    private boolean reset() {
        updateLastActionTime();
        JultiOptions options = JultiOptions.getInstance();
        List<MinecraftInstance> instances = instanceManager.getInstances();

        // Return if no instances
        if (instances.size() == 0) {
            return false;
        }

        // Get selected instance and next instance, return if no selected instance,
        // if there is only a single instance, reset it and return.
        MinecraftInstance selectedInstance = getSelectedInstance();
        if (selectedInstance == null) {
            return false;
        }
        if (instances.size() == 1) {
            selectedInstance.reset(true);
            return true;
        }

        int nextInstInd = instances.indexOf(selectedInstance) + 1;
        if (nextInstInd >= instances.size()) {
            nextInstInd = 0;
        }
        MinecraftInstance nextInstance = instances.get(nextInstInd);

        nextInstance.activate();
        switchScene(nextInstInd + 1);

        if (hasHidden) {
            unHideAndWait();
            instances.forEach(MinecraftInstance::reset);
        } else {
            selectedInstance.reset();
        }

        String toCopy = JultiOptions.getInstance().clipboardOnReset;
        if (!toCopy.isEmpty()) {
            KeyboardUtil.copyToClipboard(toCopy);
        }
        return true;
    }

    @Nullable
    private MinecraftInstance getSelectedInstance() {
        Pointer hwnd = HwndUtil.getCurrentHwnd();
        List<MinecraftInstance> instances = instanceManager.getInstances();
        for (MinecraftInstance instance : instances) {
            if (instance.hasWindow() && instance.getHwnd().equals(hwnd)) {
                return instance;
            }
        }
        return null;
    }

    private void updateLastActionTime() {
        lastAction = System.currentTimeMillis();
    }

    private void switchScene(final int i) {
        JultiOptions options = JultiOptions.getInstance();
        if (i <= 9 && options.obsPressHotkey) {
            int keyToPress = (options.obsUseNumpad ? KeyEvent.VK_NUMPAD0 : KeyEvent.VK_0) + i;
            List<Integer> keys = new ArrayList<>();
            if (options.obsUseAlt) {
                keys.add(Win32Con.VK_MENU);
            }
            keys.add(keyToPress);
            KeyboardUtil.pressKeysForTime(keys, 100L);
        } else {
            log(Level.ERROR, "Too many instances! Could not switch to a scene past 9.");
        }
    }

    private void unHideAndWait() {
        List<MinecraftInstance> instances = instanceManager.getInstances();
        updateLastActionTime();
        JultiOptions options = JultiOptions.getInstance();
        hasHidden = false;
        for (MinecraftInstance instance : instances) {
            if (options.useBorderless) {
                instance.borderlessAndMove(options.windowPos[0], options.windowPos[1], options.windowSize[0], options.windowSize[1]);
            } else {
                instance.maximize();
            }
        }
        sleep(100);
    }

    public void runCommand(final String command) {
        String[] args = command.split(" ");
        if (args.length == 0 || Objects.equals(args[0].trim(), "")) {
            return;
        }
        Consumer<String[]> commandConsumer = commandMap.getOrDefault(args[0], null);
        if (commandConsumer == null) {
            log(Level.ERROR, "Unknown Command \"" + command + "\"");
        } else {
            try {
                commandConsumer.accept(Arrays.copyOfRange(args, 1, args.length));
            } catch (Exception e) {
                log(Level.ERROR, "Error while running command \"" + command + "\":\n" + e.getMessage());
            }
        }
    }

    public void start() {
        stopExecutors();
        reloadInstanceManager();
        setupHotkeys();
        tickExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("julti").build());
        tickExecutor.scheduleWithFixedDelay(this::tryTick, 25, 50, TimeUnit.MILLISECONDS);
        logCheckExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("julti").build());
        logCheckExecutor.scheduleWithFixedDelay(this::logCheckTick, 12, 25, TimeUnit.MILLISECONDS);
        log(Level.INFO, "Welcome to Julti v" + VERSION + "!");
    }

    /**
     * Replaces the instanceManager in this Julti object with a new one based on saved instance paths.
     */
    public void reloadInstanceManager() {
        instanceManager = new InstanceManager(JultiOptions.getInstance().getLastInstancePaths());
    }

    public void setupHotkeys() {
        HotkeyUtil.stopGlobalHotkeyChecker();
        HotkeyUtil.clearGlobalHotkeys();

        JultiOptions options = JultiOptions.getInstance();

        HotkeyUtil.addGlobalHotkey(new HotkeyUtil.Hotkey(options.resetHotkey), this::onResetKey);
        HotkeyUtil.addGlobalHotkey(new HotkeyUtil.Hotkey(options.hideHotkey), this::onHideKey);
        HotkeyUtil.addGlobalHotkey(new HotkeyUtil.Hotkey(options.bgResetHotkey), this::onBGResetKey);

        for (Map.Entry<String, List<Integer>> entry : options.extraHotkeys.entrySet()) {
            HotkeyUtil.addGlobalHotkey(new HotkeyUtil.Hotkey(entry.getValue()), () -> this.runCommand(entry.getKey()));
        }

        HotkeyUtil.startGlobalHotkeyChecker();
    }

    private void tryTick() {
        try {
            tick();
        } catch (Exception e) {
            log(Level.ERROR, "Error during tick:" + e.getMessage());
        }
    }

    private void tick() {
        JultiOptions options = JultiOptions.getInstance();
        if (System.currentTimeMillis() - last2SecCycle > 2000) {
            last2SecCycle = System.currentTimeMillis();

            instanceManager.manageMissingInstances(this::onInstanceLoad);

            Pointer newHwnd = HwndUtil.getCurrentHwnd();
            if (!Objects.equals(newHwnd, selectedHwnd)) {
                selectedHwnd = newHwnd;
                updateLastActionTime();
            }
        }

        if (options.autoClearWorlds && (System.currentTimeMillis() - lastWorldClear) > 20000) {
            lastWorldClear = System.currentTimeMillis();
            instanceManager.clearAllWorlds();
        }

        if (options.autoHide && (System.currentTimeMillis() - lastAction) > 60000L * options.autoHideTime) {
            hideNonSelectedInstances();
        }
    }

    private void logCheckTick() {
        int i = 0;
        for (MinecraftInstance instance : instanceManager.getInstances()) {
            i++;
            try {
                instance.checkLog();
            } catch (Exception e) {
                log(Level.ERROR, "Error while checking log for instance #" + i + ":\n" + e.getMessage());
            }
        }
    }

    private void onResetKey() {
        try {
            reset();
        } catch (Exception e) {
            log(Level.ERROR, "Error during reset:\n" + e.getMessage());
        }
    }

    private void onHideKey() {
        try {
            hideNonSelectedInstances();
        } catch (Exception e) {
            log(Level.ERROR, "Error during hiding:\n" + e.getMessage());
        }
    }

    private void onBGResetKey() {
        try {
            backgroundReset();
        } catch (Exception e) {
            log(Level.ERROR, "Error during background reset:" + e.getMessage());
        }
    }

    private void backgroundReset() {
        updateLastActionTime();
        if (hasHidden) {
            unHideAndWait();
        }
        MinecraftInstance selectedInstance = getSelectedInstance();
        if (selectedInstance == null) {
            return;
        }
        for (MinecraftInstance instance : instanceManager.getInstances()) {
            if (!instance.equals(selectedInstance)) {
                instance.reset();
            }
        }
    }

    private void onInstanceLoad(MinecraftInstance minecraftInstance) {
        JultiOptions options = JultiOptions.getInstance();
        if (options.useBorderless) {
            minecraftInstance.borderlessAndMove(options.windowPos[0], options.windowPos[1], options.windowSize[0], options.windowSize[1]);
        }
    }

    private void hideNonSelectedInstances() {
        updateLastActionTime();
        List<MinecraftInstance> instances = instanceManager.getInstances();

        // Return if less than 2 instances.
        if (instances.size() < 2) {
            return;
        }

        // Get selected instance and next instance, return if no selected instance.
        MinecraftInstance selectedInstance = getSelectedInstance();
        if (selectedInstance == null) {
            return;
        }

        // Hide instances
        for (MinecraftInstance instance : instances) {
            if (instance.equals(selectedInstance)) {
                continue;
            }
            instance.setSizeZero();
        }

        hasHidden = true;
    }

    public void stop() {
        stopExecutors();
        if (instanceManager != null) {
            storeLastInstances();
        }
        HotkeyUtil.stopGlobalHotkeyChecker();
        JultiOptions.getInstance().trySave();
    }

    private void stopExecutors() {
        if (tickExecutor != null) {
            try {
                tickExecutor.shutdownNow();
            } catch (Exception ignored) {
            }
        }
        if (logCheckExecutor != null) {
            try {
                logCheckExecutor.shutdownNow();
            } catch (Exception ignored) {
            }
        }
    }

    private void storeLastInstances() {
        List<String> instanceStrings = new ArrayList<>();
        for (MinecraftInstance instance : instanceManager.getInstances()) {
            instanceStrings.add(instance.getInstancePath().toString());
        }
        JultiOptions.getInstance().lastInstances = Collections.unmodifiableList(instanceStrings);
    }


}

