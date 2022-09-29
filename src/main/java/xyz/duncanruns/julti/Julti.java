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
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Julti {

    private static final Logger LOGGER = LogManager.getLogger("InstanceManager");

    private InstanceManager instanceManager;

    private ScheduledExecutorService tickExecutor;
    private ScheduledExecutorService logCheckExecutor;
    private final Timer timer;

    private long last2SecCycle;
    private long lastWorldClear;
    private long lastAction;

    private Pointer selectedHwnd;

    private boolean hasHidden;

    public Julti() {
        instanceManager = null;
        tickExecutor = null;
        logCheckExecutor = null;
        last2SecCycle = 0;

        timer = new Timer();
        hasHidden = true;

        selectedHwnd = null;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }

    public void start() {
        stopExecutors();
        reloadInstanceManager();
        setupHotkeys();
        tickExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("julti").build());
        tickExecutor.scheduleWithFixedDelay(this::tick, 25, 50, TimeUnit.MILLISECONDS);
        logCheckExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("julti").build());
        logCheckExecutor.scheduleWithFixedDelay(this::logCheckTick, 12, 25, TimeUnit.MILLISECONDS);
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
            HotkeyUtil.addGlobalHotkey(new HotkeyUtil.Hotkey(entry.getValue()), () -> this.runHotkey(entry.getKey()));
        }

        HotkeyUtil.startGlobalHotkeyChecker();
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
        for (MinecraftInstance instance : instanceManager.getInstances()) {
            instance.checkLog();
        }
    }

    private void onResetKey() {
        updateLastActionTime();
        JultiOptions options = JultiOptions.getInstance();
        List<MinecraftInstance> instances = instanceManager.getInstances();

        // Return if no instances
        if (instances.size() == 0) {
            return;
        }

        // Get selected instance and next instance, return if no selected instance,
        // if there is only a single instance, reset it and return.
        MinecraftInstance selectedInstance = getSelectedInstance();
        if (selectedInstance == null) {
            return;
        }
        if (instances.size() == 1) {
            selectedInstance.reset(true);
            return;
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
    }

    private void unHideAndWait() {
        List<MinecraftInstance> instances = instanceManager.getInstances();
        updateLastActionTime();
        JultiOptions options = JultiOptions.getInstance();
        hasHidden = false;
        for (MinecraftInstance instance : instances) {
            if (options.useBorderless) {
                instance.borderlessAndMove(options.screenLocation[0], options.screenLocation[1], options.screenSize[0], options.screenSize[1]);
            } else {
                instance.maximize();
            }
        }
        sleep(100);
    }

    private void updateLastActionTime() {
        lastAction = System.currentTimeMillis();
    }

    private void onHideKey() {
        hideNonSelectedInstances();
    }

    private void onBGResetKey() {
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

    private void runHotkey(String bindingName) {
        System.out.println("Running " + bindingName);
    }

    private void onInstanceLoad(MinecraftInstance minecraftInstance) {
        JultiOptions options = JultiOptions.getInstance();
        if (options.useBorderless) {
            minecraftInstance.borderlessAndMove(options.screenLocation[0], options.screenLocation[1], options.screenSize[0], options.screenSize[1]);
        }
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
            log(Level.WARN, "Too many instances! Could not switch to a scene past 9.");
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
            instance.move(0, 0, 0, 0);
        }

        hasHidden = true;
    }

    public InstanceManager getInstanceManager() {
        return instanceManager;
    }

    public void redetectInstances() {
        log(Level.INFO, "Redetecting Instances...");
        instanceManager.redetectInstances();
        log(Level.INFO, instanceManager.getInstances().size() + " instances found.");
    }

    public static void log(Level level, String message) {
        LOGGER.log(level, message);
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
