package xyz.duncanruns.julti.script.lua;

import com.sun.jna.platform.win32.Win32VK;
import org.apache.logging.log4j.Level;
import org.luaj.vm2.LuaValue;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.cancelrequester.CancelRequester;
import xyz.duncanruns.julti.command.CommandManager;
import xyz.duncanruns.julti.instance.InstanceState;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.management.ActiveWindowManager;
import xyz.duncanruns.julti.management.InstanceManager;
import xyz.duncanruns.julti.messages.HotkeyPressQMessage;
import xyz.duncanruns.julti.messages.OptionChangeQMessage;
import xyz.duncanruns.julti.resetting.ResetHelper;
import xyz.duncanruns.julti.script.ScriptManager;
import xyz.duncanruns.julti.util.*;

import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("unused")
class JultiLuaLibrary extends LuaLibrary {
    public JultiLuaLibrary(CancelRequester requester) {
        super(requester, "julti");
    }

    private static MinecraftInstance getInstanceFromInt(int instanceNum) {
        return InstanceManager.getInstanceManager().getInstances().get(instanceNum - 1);
    }

    public void activateInstance(int instanceNum, Boolean doSetupStyle) {
        Julti.waitForExecute(() -> Julti.getJulti().activateInstance(getInstanceFromInt(instanceNum), !(doSetupStyle == null) && doSetupStyle));
    }

    public void sendChatMessage(String message) {
        Julti.waitForExecute(() -> {
            MinecraftInstance selectedInstance = InstanceManager.getInstanceManager().getSelectedInstance();
            if (selectedInstance == null) {
                return;
            }
            selectedInstance.sendChatMessage(message, false);
        });
    }

    public void clearWorlds() {
        Julti.waitForExecute(BopperUtil::clearWorlds);
    }

    public void closeInstance(int instanceNum) {
        Julti.waitForExecute(() -> getInstanceFromInt(instanceNum).reset());
    }

    public void closeAllInstances() {
        Julti.waitForExecute(() -> DoAllFastUtil.doAllFast(MinecraftInstance::closeWindow));
    }

    public void replicateHotkey(String hotkeyCode, Integer mouseX, Integer mouseY) {
        Point mousePos = mouseX == null ? MouseUtil.getMousePos() : new Point(mouseX, mouseY);
        Julti.getJulti().queueMessageAndWait(new HotkeyPressQMessage(hotkeyCode, mousePos));
    }

    public void launchInstance(int instanceNum) {
        Julti.waitForExecute(() -> SafeInstanceLauncher.launchInstance(getInstanceFromInt(instanceNum)));
    }

    public void launchAllInstances() {
        Julti.waitForExecute(() -> SafeInstanceLauncher.launchInstances(InstanceManager.getInstanceManager().getInstances()));
    }

    public void lockInstance(int instanceNum) {
        Julti.waitForExecute(() -> ResetHelper.getManager().lockInstance(getInstanceFromInt(instanceNum)));
    }

    public void lockAllInstances() {
        Julti.waitForExecute(() -> InstanceManager.getInstanceManager().getInstances().forEach(instance -> ResetHelper.getManager().lockInstance(instance)));
    }

    public void log(String message) {
        Julti.log(Level.INFO, message);
    }

    public void openFile(String filePath) {
        LauncherUtil.openFile(filePath);
    }

    public void openInstanceToLan() {
        Julti.waitForExecute(() -> {
            MinecraftInstance selectedInstance = InstanceManager.getInstanceManager().getSelectedInstance();
            if (selectedInstance != null) {
                selectedInstance.openToLan(false);
            }
        });
    }

    public boolean trySetOption(String optionName, String optionValue) {
        AtomicBoolean out = new AtomicBoolean(false);
        Julti.getJulti().queueMessageAndWait(new OptionChangeQMessage(optionName, optionValue));
        return out.get();
    }

    public String getOptionAsString(String optionName) {
        AtomicReference<String> out = new AtomicReference<>("");
        Julti.waitForExecute(() -> {
            String val = JultiOptions.getJultiOptions().getValueString(optionName);
            if (val != null) {
                out.set(val);
            }
        });
        return out.get();
    }

    public boolean tryPlaySound(String soundLocation, float volume) {
        Path pathFromSoundsFolder = JultiOptions.getJultiDir().resolve("sounds").resolve(soundLocation);
        if (Files.isRegularFile(pathFromSoundsFolder)) {
            SoundUtil.playSound(pathFromSoundsFolder.toFile(), volume);
            return true;
        }
        Path wholePath = Paths.get(soundLocation);
        if (Files.isRegularFile(wholePath)) {
            SoundUtil.playSound(wholePath.toFile(), volume);
            return true;
        }
        return false;
    }

    public void resetInstance(int instanceNum) {
        Julti.waitForExecute(() -> getInstanceFromInt(instanceNum).reset());
    }

    public void resetAllInstances() {
        Julti.waitForExecute(() -> DoAllFastUtil.doAllFast(MinecraftInstance::reset));
    }

    public void setSessionResets(int sessionResets) {
        Julti.waitForExecute(() -> {
            ResetCounter.sessionCounter = sessionResets;
            ResetCounter.updateFiles();
            Julti.log(Level.INFO, "Updated session reset counter to " + ResetCounter.sessionCounter + ".");
        });
    }

    public void sleep(long millis) {
        SleepUtil.sleep(millis);
    }

    public void waitForInstanceLaunch(int instanceNum) {
        MinecraftInstance instance = getInstanceFromInt(instanceNum);
        while ((!this.cancelRequester.isCancelRequested())) {
            AtomicBoolean b = new AtomicBoolean(false);
            Julti.waitForExecute(() -> b.set(InstanceManager.getInstanceManager().getMatchingInstance(instance).hasWindow()));
            if (b.get()) {
                break;
            }
            SleepUtil.sleep(50);
        }
    }

    public void waitForInstancePreviewLoad(int instanceNum) {
        MinecraftInstance instance = getInstanceFromInt(instanceNum);
        while ((!this.cancelRequester.isCancelRequested())) {
            AtomicBoolean b = new AtomicBoolean(false);
            Julti.waitForExecute(() -> b.set(instance.getStateTracker().isCurrentState(InstanceState.PREVIEWING)));
            if (b.get()) {
                break;
            }
            SleepUtil.sleep(50);
        }
    }

    public void waitForInstanceLoad(int instanceNum) {
        MinecraftInstance instance = getInstanceFromInt(instanceNum);
        while ((!this.cancelRequester.isCancelRequested())) {
            AtomicBoolean b = new AtomicBoolean(false);
            Julti.waitForExecute(() -> b.set(instance.getStateTracker().isCurrentState(InstanceState.INWORLD)));
            if (b.get()) {
                break;
            }
            SleepUtil.sleep(50);
        }
    }

    public void pressEscOnInstance(int instanceNum) {
        Julti.waitForExecute(() -> getInstanceFromInt(instanceNum).getKeyPresser().pressEsc());
    }

    public int getInstanceCount() {
        return InstanceManager.getInstanceManager().getSize();
    }

    public int getSelectedInstanceNum() {
        InstanceManager instanceManager = InstanceManager.getInstanceManager();
        return instanceManager.getInstanceNum(instanceManager.getSelectedInstance());
    }

    public void runCommand(String command) {
        CommandManager.getMainManager().runCommand(command, this.cancelRequester);
    }

    public void focusWall() {
        Julti.waitForExecute(() -> Julti.getJulti().focusWall());
    }

    public void runScript(String scriptName) {
        ScriptManager.runScriptAndWait(scriptName);
    }

    public void setGlobal(String key, LuaValue val) {
        LuaRunner.GLOBALS_MAP.put(key, val);
    }

    public LuaValue getGlobal(String key, LuaValue def) {
        return LuaRunner.GLOBALS_MAP.getOrDefault(key, def == null ? NIL : def);
    }

    public String getInstanceState(int instanceNum) {
        MinecraftInstance instance = getInstanceFromInt(instanceNum);
        return instance.getStateTracker().getInstanceState().name();
    }

    public String getInstanceInWorldState(int instanceNum) {
        MinecraftInstance instance = getInstanceFromInt(instanceNum);
        return instance.getStateTracker().getInWorldType().name();
    }

    public boolean isOnMinecraftWindow() {
        AtomicBoolean out = new AtomicBoolean();
        Julti.waitForExecute(() -> out.set(ActiveWindowManager.isMinecraftActive()));
        return out.get();
    }

    public void keyDown(String key) {
        if (key.startsWith("VK_")) {
            KeyboardUtil.keyDown(Win32VK.valueOf(key));
            return;
        }
        byte[] bytes = key.getBytes();
        if (bytes.length != 1) {
            return;
        }
        KeyboardUtil.keyDown(bytes[0]);
    }

    public void keyUp(String key) {
        if (key.startsWith("VK_")) {
            KeyboardUtil.keyUp(Win32VK.valueOf(key));
            return;
        }
        byte[] bytes = key.getBytes();
        if (bytes.length != 1) {
            return;
        }
        KeyboardUtil.keyUp(bytes[0]);
    }

    public void pressKey(String key) {
        this.keyDown(key);
        this.keyUp(key);
    }

    public void holdKey(String key, long millis) {
        this.keyDown(key);
        this.sleep(millis);
        this.keyUp(key);
    }

    public boolean isInstanceActive() {
        AtomicBoolean out = new AtomicBoolean();
        Julti.waitForExecute(() -> out.set(InstanceManager.getInstanceManager().getSelectedInstance() != null));
        return out.get();
    }

    public boolean isWallActive() {
        AtomicBoolean out = new AtomicBoolean();
        Julti.waitForExecute(() -> out.set(ActiveWindowManager.isWallActive()));
        return out.get();
    }

    public long getLastActivation(int instanceNum) {
        AtomicLong out = new AtomicLong();
        Julti.waitForExecute(() -> {
            out.set(getInstanceFromInt(instanceNum).getLastActivation());
        });
        return out.get();
    }

    public long getCurrentTime() {
        return System.currentTimeMillis();
    }

    public void cancelScript() {
        this.cancelRequester.cancel();
    }

    public void cancelAllScripts() {
        ScriptManager.cancelAllScripts();
    }

    public void waitForAllInstancesLaunch() {
        for (MinecraftInstance instance : InstanceManager.getInstanceManager().getInstances()) {
            while ((!this.cancelRequester.isCancelRequested())) {
                AtomicBoolean b = new AtomicBoolean(false);
                Julti.waitForExecute(() -> b.set(InstanceManager.getInstanceManager().getMatchingInstance(instance).hasWindow()));
                if (b.get()) {
                    break;
                }
                SleepUtil.sleep(50);
            }
        }
    }

    public void waitForAllInstanceSPreviewLoad() {
        for (MinecraftInstance instance : InstanceManager.getInstanceManager().getInstances()) {
            while ((!this.cancelRequester.isCancelRequested())) {
                AtomicBoolean b = new AtomicBoolean(false);
                Julti.waitForExecute(() -> b.set(instance.getStateTracker().isCurrentState(InstanceState.PREVIEWING)));
                if (b.get()) {
                    break;
                }
                SleepUtil.sleep(50);
            }
        }
    }

    public void waitForAllInstancesLoad() {
        for (MinecraftInstance instance : InstanceManager.getInstanceManager().getInstances()) {
            while ((!this.cancelRequester.isCancelRequested())) {
                AtomicBoolean b = new AtomicBoolean(false);
                Julti.waitForExecute(() -> b.set(instance.getStateTracker().isCurrentState(InstanceState.INWORLD)));
                if (b.get()) {
                    break;
                }
                SleepUtil.sleep(50);
            }
        }
    }
}
