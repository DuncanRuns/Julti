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
    private final HashMap<String, Consumer<String[]>> commandMap = getCommandMap();

    public Julti() {
        instanceManager = null;
        tickExecutor = null;
        logCheckExecutor = null;

        last2SecCycle = 0;
        lastWorldClear = 0;
        lastAction = 0;

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
        map.put("remove", this::runCommandRemove);
        map.put("hotkey", this::runCommandHotkey);
        return map;
    }

    private void runCommandHotkey(final String[] args) {
        List<String> setHotkeyArgs = Arrays.asList("reset", "hide", "bgreset", "custom");
        JultiOptions options = JultiOptions.getInstance();
        if (args.length == 0) {
            log(Level.ERROR, "No args given to hotkey command!");
        } else if ("list".equals(args[0])) {
            StringBuilder out = new StringBuilder("Hotkeys:\n" +
                    "Reset: " + HotkeyUtil.formatKeys(options.resetHotkey) + "\n" +
                    "Hide: " + HotkeyUtil.formatKeys(options.hideHotkey) + "\n" +
                    "Background Reset: " + HotkeyUtil.formatKeys(options.bgResetHotkey));
            for (Map.Entry<String, List<Integer>> entry : options.extraHotkeys.entrySet()) {
                out.append("\nCommand \"").append(entry.getKey()).append("\": ").append(HotkeyUtil.formatKeys(entry.getValue()));
            }
            log(Level.INFO, out.toString());
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
                    case "hide":
                        jultiOptions.hideHotkey = hotkey.getKeys();
                        break;
                    case "custom":
                        StringBuilder commandBuilder = new StringBuilder();
                        for (String a : Arrays.copyOfRange(args, 1, args.length)) {
                            commandBuilder.append(a).append(" ");
                        }
                        String command = commandBuilder.toString().trim();
                        jultiOptions.extraHotkeys.put(command, hotkey.getKeys());
                        break;
                }
                log(Level.INFO, "Hotkey Set");
                hotkey.wasPressed();
                setupHotkeys();
            });
        } else if ("remove".equals(args[0])) {
            StringBuilder commandBuilder = new StringBuilder();
            for (String a : Arrays.copyOfRange(args, 1, args.length)) {
                commandBuilder.append(a).append(" ");
            }
            String command = commandBuilder.toString().trim();
            if (options.extraHotkeys.remove(command) == null) {
                log(Level.ERROR, "Command \"" + command + "\" is not bound to any key.");
            } else {
                log(Level.INFO, "Removed binding for command \"" + command + "\".");
            }
            setupHotkeys();
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
        updateLastActionTime();
        List<MinecraftInstance> instances = getInstanceManager().getInstances();
        if (args.length == 0) {
            if (!reset()) {
                log(Level.ERROR, "No instances to reset / No instance selected.");
            }
        } else {
            final String input = args[0];
            if ("all".equals(input)) {
                for (MinecraftInstance instance : instances) {
                    instance.reset();
                }
            } else if ("random".equals(input)) {
                instances.get(new Random().nextInt(instances.size())).reset();
            } else if ("background".equals(input)) {
                backgroundReset();
            } else if ("unselected".equals(input)) {
                backgroundReset();
            } else {
                int index = indexFromArg(input);
                if (index != -1)
                    instances.get(index).reset();
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
        updateLastActionTime();
        List<MinecraftInstance> instances = getInstanceManager().getInstances();
        if (args.length == 0) {
            log(Level.ERROR, "No args given to activate command!");
        } else {
            final String input = args[0];
            if ("random".equals(input)) {
                instances.get(new Random().nextInt(instances.size())).activate();
            } else {
                int index = indexFromArg(input);
                if (index != -1) {
                    instances.get(index).activate();
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
        log(Level.INFO, "Commands:\n" +
                "redetect -> Sets current instances to the opened Minecraft instances\n" +
                "reset -> Functions the same as the reset hotkey\n" +
                "reset [all/random/unselected/background/#] -> Resets specified instance(s); background functions the same as the background reset hotkey\n" +
                "close <all/random/#> -> Closes a specific / all instance(s)\n" +
                "activate <random/#> -> Activates a specific instance\n" +
                "list -> Lists all opened instances\n" +
                "help -> Shows all commands\n" +
                "hide -> Functions the same as the hide hotkey\n" +
                "hide [all/random/unselected/#] -> Hides instance(s)\n" +
                "option -> Lists all options\n" +
                "option [option] -> Gets the current value of an option and gives an example to set it\n" +
                "option [option] [value] -> Sets the value of the option to the specified value\n" +
                "remove <#> -> Removes a single instance from the current instances\n" +
                "hotkey list -> List all hotkeys.\n" +
                "hotkey <reset/bgreset/hide> -> Rebinds a hotkey. After running the command, press the wanted hotkey for the chosen function.\n" +
                "hotkey custom <custom command> -> Bind a hotkey to a command. After running the command, press the wanted hotkey for the chosen command.\n" +
                "hotkey remove <custom command> -> Removes a hotkey."
        );
    }

    private void runCommandHide(String[] args) {
        updateLastActionTime();
        List<MinecraftInstance> instances = getInstanceManager().getInstances();
        if (args.length == 0) {
            MinecraftInstance selectedInstance = getSelectedInstance();
            if (selectedInstance == null) {
                log(Level.ERROR, "No instances to hide / No instance selected.");
            } else {
                for (MinecraftInstance instanceToHide : instances) {
                    if (!Objects.equals(selectedInstance, instanceToHide)) {
                        instanceToHide.hide();
                    }
                }
            }
        } else {
            final String input = args[0];
            if ("all".equals(input)) {
                for (MinecraftInstance instance : instances) {
                    instance.hide();
                }
            } else if ("random".equals(input)) {
                instances.get(new Random().nextInt(instances.size())).hide();
            } else if ("unselected".equals(input)) {
                MinecraftInstance selectedInstance = getSelectedInstance();
                for (MinecraftInstance instanceToHide : instances) {
                    if (!Objects.equals(selectedInstance, instanceToHide)) {
                        instanceToHide.hide();
                    }
                }
            } else {
                int index = indexFromArg(input);
                if (index != -1)
                    instances.get(index).hide();
            }
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

    private int indexFromArg(final String num) {
        List<MinecraftInstance> instances = getInstanceManager().getInstances();
        if (num.startsWith("~")) {
            // parseInt can handle "1", "-1", and "+1"
            int offset = Integer.parseInt(num.substring(1));

            MinecraftInstance selectedInstance = getSelectedInstance();
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

    public InstanceManager getInstanceManager() {
        return instanceManager;
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

    public void redetectInstances() {
        log(Level.INFO, "Redetecting Instances...");
        instanceManager.redetectInstances();
        for (MinecraftInstance instance : instanceManager.getInstances()) {
            onInstanceLoad(instance);
        }
        log(Level.INFO, instanceManager.getInstances().size() + " instances found.");
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

        int nextInstInd = (instances.indexOf(selectedInstance) + 1) % instances.size();
        MinecraftInstance nextInstance = instances.get(nextInstInd);

        nextInstance.activate();
        switchScene(nextInstInd + 1);

        selectedInstance.reset();

        String toCopy = JultiOptions.getInstance().clipboardOnReset;
        if (!toCopy.isEmpty()) {
            KeyboardUtil.copyToClipboard(toCopy);
        }
        return true;
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

    public void runCommand(final String commands) {
        for (String command : commands.split(";")) {
            command = commands.trim();
            String[] args = command.split(" ");
            if (args.length == 0 || "".equals(args[0].trim())) {
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
            instance.hide();
        }
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

