package xyz.duncanruns.julti;

import com.google.common.collect.ImmutableMap;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.HWND;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import xyz.duncanruns.julti.affinity.AffinityManager;
import xyz.duncanruns.julti.gui.JultiGUI;
import xyz.duncanruns.julti.hotkey.HotkeyManager;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.management.ActiveWindowManager;
import xyz.duncanruns.julti.management.InstanceManager;
import xyz.duncanruns.julti.management.LogReceiver;
import xyz.duncanruns.julti.management.OBSStateManager;
import xyz.duncanruns.julti.messages.*;
import xyz.duncanruns.julti.plugin.PluginEvents;
import xyz.duncanruns.julti.resetting.ResetHelper;
import xyz.duncanruns.julti.script.ScriptManager;
import xyz.duncanruns.julti.util.*;
import xyz.duncanruns.julti.win32.User32;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static xyz.duncanruns.julti.util.SleepUtil.sleep;

public final class Julti {
    public static final String VERSION = Julti.class.getPackage().getImplementationVersion() == null ? "DEV" : Julti.class.getPackage().getImplementationVersion();

    private static final Julti INSTANCE = new Julti();
    private static final Logger LOGGER = LogManager.getLogger("Julti");

    private boolean running = true;

    private final Queue<HotkeyPressQMessage> hotkeyQueue = new ConcurrentLinkedQueue<>();
    private final Queue<QMessage> messageQueue = new ConcurrentLinkedQueue<>();
    private final Map<Class<? extends QMessage>, Consumer<QMessage>> messageConsumerMap = (ImmutableMap.<Class<? extends QMessage>, Consumer<QMessage>>builder()
            .put(CrashQMessage.class, this::onCrashMessage)
            .put(OptionChangeQMessage.class, this::changeOption)
            .put(ProfileChangeQMessage.class, this::changeProfile)
            .put(ShutdownQMessage.class, m -> this.stop())
            .put(RunnableQMessage.class, m -> ((RunnableQMessage) m).getRunnable().run())
    ).build();

    private Julti() {
    }

    /**
     * Returns the main Julti object which gives access to some useful synchronization methods and miscellaneous wrapper methods.
     * Alternatively synchronize over the Julti object itself to ensure thread safety.
     */
    public static Julti getJulti() {
        return INSTANCE;
    }

    public static void waitForExecute(Runnable runnable) {
        getJulti().queueMessageAndWait(new RunnableQMessage(runnable));
    }

    public static void doLater(Runnable runnable) {
        getJulti().queueMessage(new RunnableQMessage(runnable));
    }

    public static void log(Level level, String message) {
        Configurator.setRootLevel(JultiOptions.getJultiOptions().showDebug ? Level.DEBUG : Level.INFO); // whether to write debug logs to latest.log (info is default)
        LOGGER.log(level, message);
        LogReceiver.receive(level, message);
    }

    private static void checkDeleteOldJar() {
        List<String> argList = Arrays.asList(JultiAppLaunch.args);
        if (!argList.contains("-deleteOldJar")) {
            return;
        }

        File toDelete = new File(argList.get(argList.indexOf("-deleteOldJar") + 1));

        log(Level.INFO, "Deleting old jar " + toDelete.getName());

        for (int i = 0; i < 200 && !toDelete.delete(); i++) {
            sleep(10);
        }

        if (toDelete.exists()) {
            log(Level.ERROR, "Failed to delete " + toDelete.getName());
        } else {
            log(Level.INFO, "Deleted " + toDelete.getName());
        }

    }

    /**
     * Returns the path of the "code source".
     * <p>
     * This will be a path to the jar file when running as a jar, and a root directory of the compiled classes when ran in a development environment.
     */
    public static Path getSourcePath() {
        try {
            return Paths.get(UpdateUtil.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns true if the code source's parent is not equal to the current working directory.
     */
    public static boolean isRanFromAlternateLocation() {
        return !Paths.get("").toAbsolutePath().equals(getSourcePath().getParent());
    }

    private static void runUtilityModeReset() {
        MinecraftInstance instance = InstanceManager.getInstanceManager().getSelectedInstance();
        if (instance != null) {
            instance.getKeyPresser().pressKey(instance.getGameOptions().createWorldKey);
        }
    }

    public static void resetInstancePositions() {
        InstanceManager.getInstanceManager().getInstances().stream().filter(MinecraftInstance::hasWindow).forEach(JultiOptions.getJultiOptions().utilityMode ? MinecraftInstance::ensurePlayingWindowState : MinecraftInstance::ensureInitialWindowState);
    }

    private void changeProfile(QMessage message) {
        this.changeProfile(((ProfileChangeQMessage) message).getProfileName());
    }

    public void changeProfile(String profileName) {
        if (!JultiOptions.getJultiOptions().trySave()) {
            return;
        }
        if (!JultiOptions.tryChangeProfile(profileName)) {
            return;
        }
        this.reload();
    }

    private void reload() {
        InstanceManager.getInstanceManager().onOptionsLoad();
        HotkeyManager.getHotkeyManager().reloadHotkeys();
        ResetHelper.getManager().reload();
        ResetCounter.updateFiles();
        PluginEvents.RunnableEventType.RELOAD.runAll();
        // Trigger plugin data loaders
        JultiOptions.getJultiOptions().triggerPluginDataLoaders();
    }

    private void changeOption(QMessage message) {
        OptionChangeQMessage ocMessage = (OptionChangeQMessage) message;
        if (!JultiOptions.getJultiOptions().trySetValue(ocMessage.getOptionName(), ocMessage.getValue())) {
            message.markFailed();
        }
    }

    private void onCrashMessage(QMessage message) {
        throw new RuntimeException(((CrashQMessage) message).getThrowable());
    }

    private void stop() {
        JultiOptions.getJultiOptions().trySave();
        AffinityManager.release();
        SleepBGUtil.disableLock();
        AffinityManager.release();
        PluginEvents.RunnableEventType.STOP.runAll();
        this.running = false;
    }

    public boolean isRunning() {
        return this.running;
    }

    public void run() throws IOException {
        ResourceUtil.makeResources();
        OBSStateManager.getOBSStateManager().tryOutputLSInfo();
        checkDeleteOldJar();
        PluginEvents.RunnableEventType.LAUNCH.runAll();

        this.reload();
        long cycles = 0;

        log(Level.INFO, "Welcome to Julti!");
        String usedJava = System.getProperty("java.home");
        log(Level.INFO, "You are running Julti v" + VERSION + " with java: " + usedJava);
        if (isRanFromAlternateLocation()) {
            log(Level.INFO, "Julti is being ran from another location");
        }
        new Thread(LegalModsUtil::updateLegalMods, "legal-mods-updater").start();

        // Schedule stuff for after Julti startup processes
        Julti.doLater(() -> {
            MistakesUtil.checkStartupMistakes();
            new Thread(() -> UpdateUtil.tryCheckForUpdates(JultiGUI.getJultiGUI()), "update-checker").start();
        });

        while (this.running) {
            sleep(1);
            synchronized (this) {
                this.tick(cycles++);
            }
        }
    }

    private void tick(long cycles) {
        PluginEvents.RunnableEventType.START_TICK.runAll();
        ActiveWindowManager.update();
        InstanceManager.getInstanceManager().tick(cycles);
        ResetHelper.getManager().tick(cycles);
        if (cycles % 100 == 0) {
            this.ensureLocation();
        }
        this.processQMessages();
        this.processHotkeyMessages();
        if (cycles % 100 == 0) {
            AffinityManager.tick();
        }
        InstanceManager.getInstanceManager().tickInstances();
        OBSStateManager.getOBSStateManager().tryOutputState();
        PluginEvents.RunnableEventType.END_TICK.runAll();
    }

    private void ensureLocation() {
        MinecraftInstance selectedInstance = InstanceManager.getInstanceManager().getSelectedInstance();
        boolean instanceActive = selectedInstance != null;
        boolean wallActive = !instanceActive && ActiveWindowManager.isWallActive();
        if (wallActive) {
            OBSStateManager.getOBSStateManager().setLocationToWall();
        } else if (instanceActive) {
            OBSStateManager.getOBSStateManager().setLocation(InstanceManager.getInstanceManager().getInstanceNum(selectedInstance));
        }
    }

    private void processQMessages() {
        while (!this.messageQueue.isEmpty()) {
            QMessage message = this.messageQueue.poll();
            Consumer<QMessage> consumer = this.messageConsumerMap.getOrDefault(message.getClass(), this::reportUnknownMessageType);
            try {
                consumer.accept(message);
            } catch (Exception e) {
                message.markFailed();
                message.markProcessed();
                throw e;
            }
            message.markProcessed();
        }
    }

    private void processHotkeyMessages() {
        // Cancel all hotkeys if instances are missing
        boolean instancesMissing = InstanceManager.getInstanceManager().areInstancesMissing();

        Set<String> hotkeyables = ActiveWindowManager.isWallActive() ? HotkeyManager.WALL_HOTKEYABLE_CODES : HotkeyManager.INGAME_HOTKEYABLE_CODES;
        Predicate<String> scriptHotkeyablePredicate = ActiveWindowManager.isWallActive() ? ScriptManager::isWallHotkeyable : ScriptManager::isInGameHotkeyable;

        while (!this.hotkeyQueue.isEmpty()) {
            boolean shouldRun;

            HotkeyPressQMessage m = this.hotkeyQueue.poll();
            String hotkeyCode = m.getHotkeyCode();

            String[] parts;
            if (hotkeyables.contains(hotkeyCode) || HotkeyManager.ANYWHERE_HOTKEYABLE_CODES.contains(hotkeyCode)) {
                shouldRun = true;
            } else if (!hotkeyCode.startsWith("script:") || !((parts = hotkeyCode.split(":")).length > 1 && scriptHotkeyablePredicate.test(parts[1]))) {
                shouldRun = false;
            } else {
                shouldRun = (parts = hotkeyCode.split(":")).length > 1 && scriptHotkeyablePredicate.test(parts[1]);
            }

            if (shouldRun) {
                if (hotkeyCode.equals("cancelScript") || hotkeyCode.startsWith("script:") || !instancesMissing) {
                    this.tryProcessHotkeyMessage(m);
                }
            } else {
                m.markFailed();
            }
            m.markProcessed();
        }
    }

    private void tryProcessHotkeyMessage(HotkeyPressQMessage message) {
        try {
            this.runHotkeyAction(message.getHotkeyCode(), message.getMousePosition());
        } catch (Exception e) {
            message.markFailed();
            throw new RuntimeException(e);
        }
    }

    private void runHotkeyAction(String hotkeyCode, Point mousePosition) {
        PluginEvents.MiscEventType.HOTKEY_PRESS.runAll(Pair.of(hotkeyCode, mousePosition));
        if (hotkeyCode.startsWith("script:")) {
            String[] parts = hotkeyCode.split(":");
            if (parts.length < 2) {
                log(Level.ERROR, "Hotkey with empty script name was ran!");
            }
            String scriptName = parts[1];
            ScriptManager.runScriptHotkey(scriptName);
        } else if (hotkeyCode.equals("cancelScript")) {
            ScriptManager.cancelAllScripts();
        } else {
            JultiOptions options = JultiOptions.getJultiOptions();
            if (!options.utilityMode) {
                ResetHelper.run(hotkeyCode, mousePosition);
            } else if (options.utilityModeAllowResets && hotkeyCode.equals("reset")) {
                runUtilityModeReset();
            }
        }
    }

    private void reportUnknownMessageType(QMessage message) {
        Julti.log(Level.WARN, "Julti received an unknown message type: " + message.getClass().getSimpleName());
    }

    public QMessage queueMessage(QMessage message) {
        if (message instanceof HotkeyPressQMessage) {
            this.hotkeyQueue.add((HotkeyPressQMessage) message);
        } else {
            this.messageQueue.add(message);
        }
        return message;
    }

    /**
     * Queue a message for the main loop to process and wait until its completion.
     *
     * @param message the QMessage object
     *
     * @return true if the message was successful, otherwise false
     */
    public boolean queueMessageAndWait(QMessage message) {
        this.queueMessage(message);
        while (!message.isProcessed()) {
            sleep(5);
            if (!this.isRunning()) {
                return false;
            }
        }
        return !message.hasFailed();
    }

    public void activateInstance(MinecraftInstance instance) {
        this.activateInstance(instance, false);
    }

    public void activateInstance(MinecraftInstance instance, boolean doingSetup) {
        JultiOptions options = JultiOptions.getJultiOptions();
        instance.activate(doingSetup);
        if ((options.alwaysOnTopProjector || options.minimizeProjectorWhenPlaying) && ActiveWindowManager.isWallActive()) {
            User32.INSTANCE.ShowWindow(ActiveWindowManager.getActiveHwnd(), User32.SW_MINIMIZE);
        }
        OBSStateManager.getOBSStateManager().setLocation(InstanceManager.getInstanceManager().getInstanceNum(instance));
    }

    public void focusWall() {
        this.focusWall(true);
    }

    public void focusWall(boolean disableLock) {
        if (disableLock) {
            SleepBGUtil.disableLock();
        }
        AtomicReference<HWND> wallHwnd = new AtomicReference<>(ActiveWindowManager.getLastWallHwnd());
        if (wallHwnd.get() == null) {
            User32.INSTANCE.EnumWindows((hwnd, data) -> {
                if (ActiveWindowManager.isWallHwnd(hwnd)) {
                    wallHwnd.set(hwnd);
                    return false;
                }
                return true;
            }, null);
            if (wallHwnd.get() == null) {
                log(Level.ERROR, "No Wall Window found!");
                return;
            }
        }
        HWND hwnd = wallHwnd.get();
        if (JultiOptions.getJultiOptions().alwaysOnTopProjector) {
            // Set always on top
            User32.INSTANCE.SetWindowPos(hwnd, new HWND(new Pointer(-1)), 0, 0, 0, 0, new WinDef.UINT(0x0002 | 0x0001));
        }
        ActiveWindowManager.activateHwnd(hwnd);
        User32.INSTANCE.ShowWindow(hwnd, User32.SW_SHOWMAXIMIZED);
        OBSStateManager.getOBSStateManager().setLocationToWall();
        PluginEvents.RunnableEventType.WALL_ACTIVATE.runAll();
    }
}
