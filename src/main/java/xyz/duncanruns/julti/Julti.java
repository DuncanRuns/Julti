package xyz.duncanruns.julti;

import com.google.common.collect.ImmutableMap;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Win32VK;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.HWND;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.duncanruns.julti.affinity.AffinityManager;
import xyz.duncanruns.julti.hotkey.HotkeyManager;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.management.ActiveWindowTracker;
import xyz.duncanruns.julti.management.InstanceManager;
import xyz.duncanruns.julti.management.LogReceiver;
import xyz.duncanruns.julti.management.OBSStateManager;
import xyz.duncanruns.julti.messages.*;
import xyz.duncanruns.julti.resetting.ResetHelper;
import xyz.duncanruns.julti.script.ScriptManager;
import xyz.duncanruns.julti.util.KeyboardUtil;
import xyz.duncanruns.julti.util.ResourceUtil;
import xyz.duncanruns.julti.util.SleepBGUtil;
import xyz.duncanruns.julti.util.WindowTitleUtil;
import xyz.duncanruns.julti.win32.User32;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static xyz.duncanruns.julti.util.SleepUtil.sleep;

public final class Julti {
    private static final Julti INSTANCE = new Julti();
    public static final String VERSION = Julti.class.getPackage().getImplementationVersion() == null ? "DEV" : Julti.class.getPackage().getImplementationVersion();
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

    public static Julti getInstance() {
        return INSTANCE;
    }

    public static void waitForExecute(Runnable runnable) {
        getInstance().queueMessageAndWait(new RunnableQMessage(runnable));
    }

    public static void doLater(Runnable runnable) {
        getInstance().queueMessage(new RunnableQMessage(runnable));
    }

    public static void log(Level level, String message) {
        LOGGER.log(level, message);
        LogReceiver.receive(level, message);
    }

    private void changeProfile(QMessage message) {
        this.changeProfile(((ProfileChangeQMessage) message).getProfileName());
    }

    public void changeProfile(String profileName) {
        JultiOptions.getInstance().trySave();
        if (!JultiOptions.tryChangeProfile(profileName)) {
            Julti.log(Level.ERROR, "Profile change failed!");
            return;
        }
        this.reload();
    }

    private void reload() {
        InstanceManager.getManager().onOptionsLoad();
        HotkeyManager.getInstance().reloadHotkeys();
        ResetHelper.getManager().reload();
    }

    private void changeOption(QMessage message) {
        OptionChangeQMessage ocMessage = (OptionChangeQMessage) message;
        if (!JultiOptions.getInstance().trySetValue(ocMessage.getOptionName(), ocMessage.getValue())) {
            message.markFailed();
        }
    }

    private void onCrashMessage(QMessage message) {
        this.stop();
        Julti.log(Level.ERROR, "Shutting down due to error: " + ((CrashQMessage) message).getThrowable());
    }

    private void stop() {
        JultiOptions.getInstance().trySave();
        AffinityManager.stop();
        SleepBGUtil.disableLock();
        AffinityManager.release();
        this.running = false;
    }

    public boolean isRunning() {
        return this.running;
    }

    public void run() {
        ResourceUtil.makeResources();
        OBSStateManager.getInstance().tryOutputLSInfo();
        this.reload();
        long cycles = 0;
        log(Level.INFO, "Welcome to Julti!");
        String libraryPathThing = System.getProperty("java.library.path");
        log(Level.INFO, "You are running Julti v" + VERSION + " with java: " + libraryPathThing.substring(0, libraryPathThing.indexOf(";")));
        while (this.running) {
            sleep(1);
            this.tick(cycles++);
        }
    }

    private void tick(long cycles) {
        ActiveWindowTracker.update();
        InstanceManager.getManager().tick(cycles);
        if (cycles % 100 == 0) {
            this.ensureLocation();
        }
        this.processQMessages();
        this.processHotkeyMessages();
        InstanceManager.getManager().updateInstanceStates();
        OBSStateManager.getInstance().tryOutputState();
    }

    private void ensureLocation() {
        MinecraftInstance selectedInstance = InstanceManager.getManager().getSelectedInstance();
        boolean instanceActive = selectedInstance != null;
        boolean wallActive = !instanceActive && ActiveWindowTracker.isWallActive();
        if (wallActive) {
            OBSStateManager.getInstance().setLocationToWall();
        } else if (instanceActive) {
            OBSStateManager.getInstance().setLocation(InstanceManager.getManager().getInstanceNum(selectedInstance));
        }
    }

    private void processQMessages() {
        while (!this.messageQueue.isEmpty()) {
            QMessage message = this.messageQueue.poll();
            Consumer<QMessage> consumer = this.messageConsumerMap.getOrDefault(message.getClass(), this::reportUnknownMessageType);
            consumer.accept(message);
            message.markProcessed();
        }
    }

    private void processHotkeyMessages() {
        // Cancel all hotkeys if instances are missing
        if (InstanceManager.getManager().areInstancesMissing()) {
            this.hotkeyQueue.forEach(QMessage::markProcessed);
            this.hotkeyQueue.clear();
            return;
        }

        while (!this.hotkeyQueue.isEmpty()) {
            HotkeyPressQMessage message = this.hotkeyQueue.poll();
            try {
                this.runHotkeyAction(message.getHotkeyCode());
            } catch (Exception e) {
                message.markFailed();
                this.onCrashMessage(new CrashQMessage(e));
            }
            message.markProcessed();
        }
    }

    private void runHotkeyAction(String hotkeyCode) {
        if (hotkeyCode.startsWith("script:")) {
            String scriptName = hotkeyCode.split(":")[1];
            boolean instanceActive = InstanceManager.getManager().getSelectedInstance() != null;
            boolean wallActive = !instanceActive && ActiveWindowTracker.isWallActive();
            if ((!instanceActive) && (!wallActive)) {
                return;
            }
            ScriptManager.runScript(scriptName, true, (byte) (instanceActive ? 1 : 2));
        } else if (hotkeyCode.equals("cancelScript")) {
            ScriptManager.requestCancel();
        } else {
            ResetHelper.run(hotkeyCode);
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
        instance.activate(doingSetup);
        if (JultiOptions.getInstance().alwaysOnTopProjector && ActiveWindowTracker.isWallActive()) {
            User32.INSTANCE.ShowWindow(ActiveWindowTracker.getActiveHwnd(), User32.SW_MINIMIZE);
        }
        OBSStateManager.getInstance().setLocation(InstanceManager.getManager().getInstanceNum(instance));
    }

    public void focusWall() {
        SleepBGUtil.disableLock();
        AtomicReference<HWND> wallHwnd = new AtomicReference<>(ActiveWindowTracker.getLastWallHwnd());
        if (wallHwnd.get() == null) {
            User32.INSTANCE.EnumWindows((hwnd, data) -> {
                if (WindowTitleUtil.isOBSTitle(WindowTitleUtil.getHwndTitle(hwnd))) {
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
        if (JultiOptions.getInstance().alwaysOnTopProjector) {
            // Set always on top
            User32.INSTANCE.SetWindowPos(hwnd, new HWND(new Pointer(-1)), 0, 0, 0, 0, new WinDef.UINT(0x0002 | 0x0001));
        }
        KeyboardUtil.keyDown(Win32VK.VK_LMENU);
        KeyboardUtil.keyUp(Win32VK.VK_LMENU);
        User32.INSTANCE.SetForegroundWindow(hwnd);
        User32.INSTANCE.BringWindowToTop(hwnd);
        User32.INSTANCE.ShowWindow(hwnd, User32.SW_SHOWMAXIMIZED);
        OBSStateManager.getInstance().setLocationToWall();
    }
}
