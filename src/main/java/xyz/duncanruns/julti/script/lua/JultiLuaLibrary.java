package xyz.duncanruns.julti.script.lua;

import com.sun.jna.platform.win32.Win32VK;
import org.apache.logging.log4j.Level;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.cancelrequester.CancelRequester;
import xyz.duncanruns.julti.cancelrequester.CancelRequesters;
import xyz.duncanruns.julti.command.CommandManager;
import xyz.duncanruns.julti.gui.JultiGUI;
import xyz.duncanruns.julti.instance.InstanceState;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.management.ActiveWindowManager;
import xyz.duncanruns.julti.management.InstanceManager;
import xyz.duncanruns.julti.messages.HotkeyPressQMessage;
import xyz.duncanruns.julti.messages.OptionChangeQMessage;
import xyz.duncanruns.julti.resetting.ResetHelper;
import xyz.duncanruns.julti.script.CustomizableManager;
import xyz.duncanruns.julti.script.LuaScript;
import xyz.duncanruns.julti.script.ScriptManager;
import xyz.duncanruns.julti.util.*;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("unused")
class JultiLuaLibrary extends LuaLibrary {
    public static final Map<String, LuaValue> GLOBALS_MAP = Collections.synchronizedMap(new HashMap<>());
    protected final LuaScript script;

    public JultiLuaLibrary(CancelRequester requester, LuaScript script) {
        super(requester, "julti");
        this.script = script;
    }

    private JultiLuaLibrary() {
        super(CancelRequesters.ALWAYS_CANCEL_REQUESTER, "julti");
        this.script = null;
    }

    public static JultiLuaLibrary forLibGen() {
        return new JultiLuaLibrary();
    }

    private static MinecraftInstance getInstanceFromInt(int instanceNum) {
        synchronized (Julti.getJulti()) {
            return InstanceManager.getInstanceManager().getInstances().get(instanceNum - 1);
        }
    }

    private static Integer getInstanceNumAtPosition(Point mousePos) {
        synchronized (Julti.getJulti()) {
            return InstanceManager.getInstanceManager().getInstanceNum(ResetHelper.getManager().getHoveredWallInstance(mousePos));
        }
    }

    @AllowedWhileCustomizing
    public String getScriptName() {
        return this.script.getName();
    }

    @LuaDocumentation(description = "Brings the user to the instance specified by instanceNum. Optionally a second argument (doSetupStyle) can be specified to prevent window resizing, fullscreening, unpausing, etc.", paramTypes = {"number", "boolean|nil"})
    public void activateInstance(int instanceNum, Boolean doSetupStyle) {
        synchronized (Julti.getJulti()) {
            Julti.getJulti().activateInstance(getInstanceFromInt(instanceNum), !(doSetupStyle == null) && doSetupStyle);
        }
    }

    @LuaDocumentation(description = "Sends a chat message in the active instance. A slash needs to be given if executing a command (eg. julti.sendChatMessage(\"/kill\")).")
    public void sendChatMessage(String message) {
        synchronized (Julti.getJulti()) {
            MinecraftInstance selectedInstance = InstanceManager.getInstanceManager().getSelectedInstance();
            if (selectedInstance == null) {
                return;
            }
            selectedInstance.sendChatMessage(message, false);
        }
    }

    @LuaDocumentation(description = "Clears all but the last 5 worlds in each instance.")
    public void clearWorlds() {
        synchronized (Julti.getJulti()) {
            BopperUtil.clearWorlds();
        }
    }

    @LuaDocumentation(description = "Clears all but the last 5 worlds in each instance and waits for it to complete.")
    public void clearWorldsAndWait() {
        synchronized (Julti.getJulti()) {
            BopperUtil.clearWorlds();
        }
        BopperUtil.waitForFinish();
    }

    @LuaDocumentation(description = "Closes a specific instance.")
    public void closeInstance(int instanceNum) {
        synchronized (Julti.getJulti()) {
            getInstanceFromInt(instanceNum).reset();
        }
    }

    @LuaDocumentation(description = "Closes all instances.")
    public void closeAllInstances() {
        synchronized (Julti.getJulti()) {
            DoAllFastUtil.doAllFast(MinecraftInstance::closeWindow);
        }
    }

    @LuaDocumentation(description = "Replicates a hotkey action exactly. Possible codes are: \"reset\", \"bgReset\", \"wallReset\", \"wallSingleReset\", \"wallLock\", \"wallPlay\", \"wallFocusReset\", \"wallPlayLock\", \"debugHover\", or \"script:<script name>\"\nA mouse position can also be specified with 2 extra arguments, or if left blank will use the user's mouse position.", paramTypes = {"string", "number|nil", "number|nil"})
    public void replicateHotkey(String hotkeyCode, Integer mouseX, Integer mouseY) {
        Point mousePos = mouseX == null ? MouseUtil.getMousePos() : new Point(mouseX, mouseY);
        Julti.getJulti().queueMessageAndWait(new HotkeyPressQMessage(hotkeyCode, mousePos));
    }

    @LuaDocumentation(description = "Launches a specific instance. Does nothing if the instance is already opened. Requires a multimc executable to be specified in Julti options.")
    public void launchInstance(int instanceNum) {
        Julti.waitForExecute(() -> SafeInstanceLauncher.launchInstance(getInstanceFromInt(instanceNum)));
    }

    @LuaDocumentation(description = "Launches all unopened instances. Requires a multimc executable to be specified in Julti options.")
    public void launchAllInstances() {
        Julti.waitForExecute(() -> SafeInstanceLauncher.launchInstances(InstanceManager.getInstanceManager().getInstances()));
    }

    @LuaDocumentation(description = "Locks a specific instance.")
    public void lockInstance(int instanceNum) {
        Julti.waitForExecute(() -> ResetHelper.getManager().lockInstance(getInstanceFromInt(instanceNum)));
    }

    @LuaDocumentation(description = "Locks all instances.")
    public void lockAllInstances() {
        Julti.waitForExecute(() -> InstanceManager.getInstanceManager().getInstances().forEach(instance -> ResetHelper.getManager().lockInstance(instance)));
    }

    @LuaDocumentation(description = "Logs a message into the Julti log. If a script is ran in customization mode, this won't log anything until julti.isCustomization() is checked.")
    @AllowedWhileCustomizing
    public void log(String message) {
        Julti.log(Level.INFO, message);
    }

    @LuaDocumentation(description = "Opens the specified file as if it were double-clicked in file explorer.")
    @AllowedWhileCustomizing
    public void openFile(String filePath) {
        LauncherUtil.openFile(filePath);
    }

    @LuaDocumentation(description = "Opens the currently active instance's world to lan.", paramTypes = "boolean|nil")
    public void openInstanceToLan(Boolean enableCheats) {
        Julti.waitForExecute(() -> {
            MinecraftInstance selectedInstance = InstanceManager.getInstanceManager().getSelectedInstance();
            if (selectedInstance != null) {
                selectedInstance.openToLan(false, enableCheats != null && enableCheats);
            }
        });
    }

    @LuaDocumentation(description = "Sets the Julti option to the given value after attempting to convert it.", paramTypes = {"string", "any"})
    @AllowedWhileCustomizing
    public boolean trySetOption(String optionName, String optionValue) {
        return Julti.getJulti().queueMessageAndWait(new OptionChangeQMessage(optionName, optionValue));
    }

    @LuaDocumentation(description = "Gets a Julti option and returns it as a string.")
    @AllowedWhileCustomizing
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

    @LuaDocumentation(description = "Plays a sound file located in the .Julti/sounds folder. A full path to a sound can also be specified.")
    @AllowedWhileCustomizing
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

    @LuaDocumentation(description = "Resets the specified instance.")
    public void resetInstance(int instanceNum) {
        Julti.waitForExecute(() -> getInstanceFromInt(instanceNum).reset());
    }

    @LuaDocumentation(description = "Resets all instances, regardless of locks.")
    public void resetAllInstances() {
        Julti.waitForExecute(() -> DoAllFastUtil.doAllFast(MinecraftInstance::reset));
    }

    @LuaDocumentation(description = "Sets the session resets to the given integer.")
    public void setSessionResets(int sessionResets) {
        Julti.waitForExecute(() -> {
            ResetCounter.sessionCounter = sessionResets;
            ResetCounter.updateFiles();
            Julti.log(Level.INFO, "Updated session reset counter to " + ResetCounter.sessionCounter + ".");
        });
    }

    @LuaDocumentation(description = "Sleeps for the specified amount of milliseconds.")
    public void sleep(long millis) {
        SleepUtil.sleep(millis);
    }

    @LuaDocumentation(description = "Waits for an instance to launch.")
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

    @LuaDocumentation(description = "Waits for an instance's preview to load.")
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

    @LuaDocumentation(description = "Waits for an instance's world to load.")
    public void waitForInstanceLoad(int instanceNum) {
        MinecraftInstance instance = getInstanceFromInt(instanceNum);
        while ((!this.cancelRequester.isCancelRequested())) {
            synchronized (Julti.getJulti()) {
                if (instance.getStateTracker().isCurrentState(InstanceState.INWORLD)) {
                    break;
                }
            }
            SleepUtil.sleep(50);
        }
    }

    @LuaDocumentation(description = "Presses the escape key on the specified instance.")
    public void pressEscOnInstance(int instanceNum) {
        Julti.waitForExecute(() -> getInstanceFromInt(instanceNum).getKeyPresser().pressEsc());
    }

    @LuaDocumentation(description = "Gets the amount of instances in the instance list.")
    @AllowedWhileCustomizing
    public int getInstanceCount() {
        synchronized (Julti.getJulti()) {
            return InstanceManager.getInstanceManager().getSize();
        }
    }

    @LuaDocumentation(description = "Gets the instance number of the currently active instance. Returns 0 if no instance is active.")
    public int getSelectedInstanceNum() {
        synchronized (Julti.getJulti()) {
            InstanceManager instanceManager = InstanceManager.getInstanceManager();
            return instanceManager.getInstanceNum(instanceManager.getSelectedInstance());
        }
    }

    @LuaDocumentation(description = "Runs a Julti command.")
    public void runCommand(String command) {
        CommandManager.getMainManager().runCommand(command, this.cancelRequester);
    }

    @LuaDocumentation(description = "Brings the user to the wall window if it exists.")
    public void focusWall() {
        Julti.waitForExecute(() -> Julti.getJulti().focusWall());
    }

    @LuaDocumentation(description = "Runs a script of the specified name and waits for it to end.")
    public void runScript(String scriptName) {
        ScriptManager.runScriptAndWait(scriptName);
    }

    @LuaDocumentation(description = "Sets a value in global storage. The value can be accessed on other runs of this script or any other script.\nGlobal storage is not persistent through restarts of Julti.\nFor string storage that is private to this specific script and persists through restarts, see julti.getCustomizable and julti.setCustomizable.")
    @AllowedWhileCustomizing
    public void setGlobal(String key, LuaValue val) {
        GLOBALS_MAP.put(key, val);
    }

    @LuaDocumentation(description = "Retrieves a value from global storage set by julti.setGlobal(). A default value can optionally be provided in the case that no value is found in the globals storage.", returnTypes = "any|nil", paramTypes = {"string", "any|nil"})
    @AllowedWhileCustomizing
    public LuaValue getGlobal(String key, LuaValue def) {
        return GLOBALS_MAP.getOrDefault(key, def == null ? NIL : def);
    }

    @LuaDocumentation(description = "Gets the current state of the instance. Returns \"WAITING\", \"INWORLD\", \"TITLE\", \"GENERATING\", or \"PREVIEWING\".")
    public String getInstanceState(int instanceNum) {
        synchronized (Julti.getJulti()) {
            return getInstanceFromInt(instanceNum).getStateTracker().getInstanceState().name();
        }
    }

    @LuaDocumentation(description = "Gets a more detailed state of the \"INWORLD\" state. Returns \"UNPAUSED\", \"PAUSED\", or \"GAMESCREENOPEN\".")
    public String getInstanceInWorldState(int instanceNum) {
        synchronized (Julti.getJulti()) {
            return getInstanceFromInt(instanceNum).getStateTracker().getInWorldType().name();
        }
    }

    @LuaDocumentation(description = "Returns true if any Minecraft window is active.")
    public boolean isOnMinecraftWindow() {
        synchronized (Julti.getJulti()) {
            return ActiveWindowManager.isMinecraftActive();
        }
    }

    @LuaDocumentation(description = "Simulates holding a key on the keyboard.\nTakes a single character (\"a\", \"b\", \"1\", \"2\") or a VK constant such as \"VK_RETURN\" (https://learn.microsoft.com/en-us/windows/win32/inputdev/virtual-key-codes)")
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

    @LuaDocumentation(description = "Simulates releasing a key on the keyboard.\nTakes a single character (\"a\", \"b\", \"1\", \"2\") or a VK constant such as \"VK_RETURN\" (https://learn.microsoft.com/en-us/windows/win32/inputdev/virtual-key-codes)")
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

    @LuaDocumentation(description = "Simulates pressing and releasing a key instantly on the keyboard.\nTakes a single character (\"a\", \"b\", \"1\", \"2\") or a VK constant such as \"VK_RETURN\" (https://learn.microsoft.com/en-us/windows/win32/inputdev/virtual-key-codes)")
    public void pressKey(String key) {
        this.keyDown(key);
        this.keyUp(key);
    }

    @LuaDocumentation(description = "Simulates holding a key on the keyboard and releases it after the specified amount of milliseconds.\nTakes a single character (\"a\", \"b\", \"1\", \"2\") or a VK constant such as \"VK_RETURN\" (https://learn.microsoft.com/en-us/windows/win32/inputdev/virtual-key-codes)")
    public void holdKey(String key, long millis) {
        this.keyDown(key);
        this.sleep(millis);
        this.keyUp(key);
    }

    @LuaDocumentation(description = "Returns true if an instance from the instances list is active.")
    public boolean isInstanceActive() {
        synchronized (Julti.getJulti()) {
            return InstanceManager.getInstanceManager().getSelectedInstance() != null;
        }
    }

    @LuaDocumentation(description = "Returns true if the wall window is active.")
    public boolean isWallActive() {
        synchronized (Julti.getJulti()) {
            return ActiveWindowManager.isWallActive();
        }
    }

    @LuaDocumentation(description = "Gets the last activation time of the specified instance in milliseconds. Useful in combination with julti.getCurrentTime().")
    public long getLastActivation(int instanceNum) {
        return getInstanceFromInt(instanceNum).getLastActivation();
    }

    @LuaDocumentation(description = "Gets the current time in milliseconds.")
    @AllowedWhileCustomizing
    public long getCurrentTime() {
        return System.currentTimeMillis();
    }

    @LuaDocumentation(description = "Cancels this running script. A simple return statement can also be used to stop the lua script.")
    @AllowedWhileCustomizing
    public void cancelScript() {
        this.cancelRequester.cancel();
    }

    @LuaDocumentation(description = "Cancels all running scripts.")
    public void cancelAllScripts() {
        ScriptManager.cancelAllScripts();
    }

    @LuaDocumentation(description = "Waits for all instances to launch.")
    public void waitForAllInstancesLaunch() {
        for (MinecraftInstance instance : InstanceManager.getInstanceManager().getInstances()) {
            while ((!this.cancelRequester.isCancelRequested())) {
                synchronized (Julti.getJulti()) {
                    if (InstanceManager.getInstanceManager().getMatchingInstance(instance).hasWindow()) {
                        break;
                    }
                }
                SleepUtil.sleep(50);
            }
        }
    }

    @LuaDocumentation(description = "Waits for all instances' previews to load.")
    public void waitForAllInstancesPreviewLoad() {
        for (MinecraftInstance instance : InstanceManager.getInstanceManager().getInstances()) {
            while ((!this.cancelRequester.isCancelRequested())) {
                synchronized (Julti.getJulti()) {
                    if (instance.getStateTracker().isCurrentState(InstanceState.PREVIEWING)) {
                        break;
                    }
                }
                SleepUtil.sleep(50);
            }
        }
    }

    @LuaDocumentation(description = "Waits for all instances' worlds to load.")
    public void waitForAllInstancesLoad() {
        for (MinecraftInstance instance : InstanceManager.getInstanceManager().getInstances()) {
            while ((!this.cancelRequester.isCancelRequested())) {
                synchronized (Julti.getJulti()) {
                    if (instance.getStateTracker().isCurrentState(InstanceState.INWORLD)) {
                        break;
                    }
                }
                SleepUtil.sleep(50);
            }
        }
    }

    @LuaDocumentation(description = "Returns true if the script is being run in customization mode, otherwise false.")
    @AllowedWhileCustomizing
    public boolean isCustomizing() {
        return false;
    }

    @LuaDocumentation(description = "Sets and stores a customizable string. Usually should be done inside of an if julti.isCustomizing() block.\nValues stored are only accessible to runs of this script and are persistent through Julti restarts.")
    @AllowedWhileCustomizing
    public void setCustomizable(String key, String value) {
        CustomizableManager.set(this.script.getName(), key, value);
    }

    @LuaDocumentation(description = "Gets a stored customizable string. A default value can optionally be provided in the case that no value is found in the customizables storage.", paramTypes = {"string", "string|nil"})
    @AllowedWhileCustomizing
    @Nullable
    public String getCustomizable(String key, String def) {
        String out = CustomizableManager.get(this.script.getName(), key);
        return out == null ? def : out;
    }

    @LuaDocumentation(description = "Presents the user with a text input box and returns the string entered, or nil if they cancel/close the prompt without pressing Ok.", returnTypes = "string|nil", paramTypes = {"string", "string|nil", "(fun(input: string): boolean)|nil"})
    @AllowedWhileCustomizing
    @Nullable
    public String askTextBox(String message, String startingVal, LuaFunction validator) {
        boolean invalidInput = false;
        while (true) {
            Object o = JOptionPane.showInputDialog(JultiGUI.getJultiGUI().getControlPanel().openScriptsGUI(), invalidInput ? "Your input was invalid!\n" + message : message, "Julti Script: " + this.script.getName(), JOptionPane.PLAIN_MESSAGE, null, null, Optional.ofNullable(startingVal).orElse(""));
            if (o == null) {
                return null;
            }
            String string = o.toString();
            if (validator == null || validator.call(valueOf(string)).checkboolean()) {
                return string;
            }
            invalidInput = true;
        }
    }

    @LuaDocumentation(description = "Presents the user with a message and Yes/No/Cancel buttons. Returns true for yes, false for no, and nil for cancel or if the user closes the window.", returnTypes = "boolean|nil")
    @AllowedWhileCustomizing
    @Nullable
    public Boolean askYesNo(String message) {
        int ans = JOptionPane.showConfirmDialog(JultiGUI.getJultiGUI().getControlPanel().openScriptsGUI(), message, "Julti Script: " + this.script.getName(), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null);
        switch (ans) {
            case 0:
                return true;
            case 1:
                return false;
            default:
                return null;
        }
    }

    @LuaDocumentation(description = "Shows a message in a message box to the user.")
    @AllowedWhileCustomizing
    public void showMessageBox(String message) {
        JOptionPane.showMessageDialog(JultiGUI.getJultiGUI(), message, "Julti Script: " + this.script.getName(), JOptionPane.PLAIN_MESSAGE, null);
    }

    @LuaDocumentation(description = "Gets the position of the mouse.", returnTypes = {"number", "number"})
    public Varargs getMousePosition() {
        Point mousePos = MouseUtil.getMousePos();
        return varargsOf(new LuaValue[]{valueOf(mousePos.x), valueOf(mousePos.y)});
    }

    @LuaDocumentation(description = "Gets the instance number of the instance at the specified mouse position, or 0 if no instance at position.")
    public Integer getInstanceNumAtPosition(int mouseX, int mouseY) {
        return getInstanceNumAtPosition(new Point(mouseX, mouseY));
    }

    @LuaDocumentation(description = "Gets the instance number of the hovered instance, or 0 if no hovered instance.")
    public Integer getHoveredInstanceNum() {
        return getInstanceNumAtPosition(MouseUtil.getMousePos());
    }

    @LuaDocumentation(description = "Checks if an instance is locked.")
    public boolean isInstanceLocked(int instanceNum) {
        return ResetHelper.getManager().getLockedInstances().contains(getInstanceFromInt(instanceNum));
    }

    @LuaDocumentation(description = "Checks if a script of the specified name exists.")
    @AllowedWhileCustomizing
    public boolean scriptExists(String scriptName) {
        synchronized (Julti.getJulti()) {
            return ScriptManager.getScriptNames().contains(scriptName);
        }
    }
}
