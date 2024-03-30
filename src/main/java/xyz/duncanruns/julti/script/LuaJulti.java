package xyz.duncanruns.julti.script;

import org.apache.logging.log4j.Level;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.DebugLib;
import org.luaj.vm2.lib.LibFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.cancelrequester.CancelRequester;
import xyz.duncanruns.julti.command.CommandManager;
import xyz.duncanruns.julti.instance.InstanceState;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.management.InstanceManager;
import xyz.duncanruns.julti.messages.HotkeyPressQMessage;
import xyz.duncanruns.julti.resetting.ResetHelper;
import xyz.duncanruns.julti.util.*;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("unused")
public class LuaJulti {
    private static final Map<String, LuaValue> GLOBALS_MAP = Collections.synchronizedMap(new HashMap<>());

    public static void runLuaScript(String luaScript, CancelRequester cancelRequester) {
        Globals globals = JsePlatform.standardGlobals();
        globals.load(new InterruptibleDebugLib(cancelRequester));
        globals.load(new JultiLib(cancelRequester));
        LuaValue chunk = globals.load(luaScript);
        try {
            chunk.call();
        } catch (LuaError e) {
            if (!(e.getCause() instanceof LuaScriptCancelledException)) {
                Julti.log(Level.ERROR, "Error while executing script: " + ExceptionUtil.toDetailedString(e.getCause()));
            }
        }
    }

    private static LibFunction convertToArgFunctionObj(JultiLib obj, Method method) {
        return new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                ArrayList<LuaValue> argVals = new ArrayList<>(args.narg());
                for (int i = 1; i <= args.narg(); i++) {
                    argVals.add(args.arg(i));
                }
                try {
                    return (LuaValue) method.invoke(obj, argVals.toArray());
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    private static MinecraftInstance getInstanceFromInt(LuaValue instanceNum) {
        return InstanceManager.getInstanceManager().getInstances().get(instanceNum.checkint() - 1);
    }

    private static class LuaScriptCancelledException extends RuntimeException {
    }

    private static class InterruptibleDebugLib extends DebugLib {
        private final CancelRequester cancelRequester;

        public InterruptibleDebugLib(CancelRequester cancelRequester) {
            super();
            this.cancelRequester = cancelRequester;
        }

        @Override
        public void onInstruction(int pc, Varargs v, int top) {
            if (this.cancelRequester.isCancelRequested()) {
                throw new LuaScriptCancelledException();
            }
            super.onInstruction(pc, v, top);
        }

    }

    public static class JultiLib extends TwoArgFunction {
        private final CancelRequester cancelRequester;

        public JultiLib(CancelRequester cancelRequester) {
            this.cancelRequester = cancelRequester;
        }

        // Julti Lua Library Functions Start Here //

        public LuaValue activateInstance(LuaValue /*int*/ instanceNum, LuaValue /*boolean*/ doSetupStyle) {
            Julti.waitForExecute(() -> Julti.getJulti().activateInstance(getInstanceFromInt(instanceNum), !doSetupStyle.isnil() && doSetupStyle.checkboolean()));
            return NIL;
        }

        public LuaValue sendChatMessage(LuaValue /*String*/ message) {
            Julti.waitForExecute(() -> {
                MinecraftInstance selectedInstance = InstanceManager.getInstanceManager().getSelectedInstance();
                if (selectedInstance == null) {
                    return;
                }
                selectedInstance.sendChatMessage(message.checkjstring(), false);
            });
            return NIL;
        }

        public LuaValue clearWorlds() {
            Julti.waitForExecute(BopperUtil::clearWorlds);
            return NIL;
        }

        public LuaValue closeInstance(LuaValue /*int*/ instanceNum) {
            Julti.waitForExecute(() -> getInstanceFromInt(instanceNum).reset());
            return NIL;
        }

        public LuaValue closeAllInstances() {
            Julti.waitForExecute(() -> DoAllFastUtil.doAllFast(MinecraftInstance::closeWindow));
            return NIL;
        }

        public LuaValue replicateHotkey(LuaValue /*String*/ hotkeyCode, LuaValue /*int*/ mouseX, LuaValue /*int*/ mouseY) {
            Point mousePos = mouseX.isnil() ? MouseUtil.getMousePos() : new Point(mouseX.checkint(), mouseY.checkint());
            Julti.getJulti().queueMessageAndWait(new HotkeyPressQMessage(hotkeyCode.checkjstring(), mousePos));
            return NIL;
        }

        public LuaValue launchInstance(LuaValue /*int*/ instanceNum) {
            Julti.waitForExecute(() -> SafeInstanceLauncher.launchInstance(getInstanceFromInt(instanceNum)));
            return NIL;
        }

        public LuaValue launchAllInstances() {
            Julti.waitForExecute(() -> SafeInstanceLauncher.launchInstances(InstanceManager.getInstanceManager().getInstances()));
            return NIL;
        }

        public LuaValue lockInstance(LuaValue /*int*/ instanceNum) {
            Julti.waitForExecute(() -> ResetHelper.getManager().lockInstance(getInstanceFromInt(instanceNum)));
            return NIL;
        }

        public LuaValue lockAllInstances() {
            Julti.waitForExecute(() -> InstanceManager.getInstanceManager().getInstances().forEach(instance -> ResetHelper.getManager().lockInstance(instance)));
            return NIL;
        }

        public LuaValue log(LuaValue /*String*/ message) {
            Julti.log(Level.INFO, message.checkjstring());
            return NIL;
        }

        public LuaValue openFile(LuaValue /*String*/ filePath) {
            LauncherUtil.openFile(filePath.checkjstring());
            return NIL;
        }

        public LuaValue openInstanceToLan() {
            Julti.waitForExecute(() -> {
                MinecraftInstance selectedInstance = InstanceManager.getInstanceManager().getSelectedInstance();
                if (selectedInstance != null) {
                    selectedInstance.openToLan(false);
                }
            });
            return NIL;
        }

        public LuaValue trySetOption(LuaValue /*String*/ optionName, LuaValue /*anything*/ optionValue) {
            AtomicBoolean out = new AtomicBoolean(false);
            Julti.waitForExecute(() -> out.set(JultiOptions.getJultiOptions().trySetValue(optionName.checkjstring(), optionValue.checkjstring())));
            return valueOf(out.get());
        }

        public LuaValue getOptionAsString(LuaValue /*String*/ optionName) {
            AtomicReference<String> out = new AtomicReference<>("");
            Julti.waitForExecute(() -> {
                String val = JultiOptions.getJultiOptions().getValueString(optionName.checkjstring());
                if (val != null) {
                    out.set(val);
                }
            });
            return valueOf(out.get());
        }

        public LuaValue tryPlaySound(LuaValue /*String*/ soundLocation, LuaValue /*float*/ volume) {
            float volumeFloat = volume.tofloat();

            String soundLocationString = soundLocation.checkjstring();
            Path pathFromSoundsFolder = JultiOptions.getJultiDir().resolve("sounds").resolve(soundLocationString);
            if (Files.isRegularFile(pathFromSoundsFolder)) {
                SoundUtil.playSound(pathFromSoundsFolder.toFile(), volumeFloat);
                return TRUE;
            }
            Path wholePath = Paths.get(soundLocationString);
            if (Files.isRegularFile(wholePath)) {
                SoundUtil.playSound(wholePath.toFile(), volumeFloat);
                return TRUE;
            }
            return FALSE;
        }

        public LuaValue resetInstance(LuaValue /*int*/ instanceNum) {
            Julti.waitForExecute(() -> getInstanceFromInt(instanceNum).reset());
            return NIL;
        }

        public LuaValue resetAllInstances() {
            Julti.waitForExecute(() -> DoAllFastUtil.doAllFast(MinecraftInstance::reset));
            return NIL;
        }

        public LuaValue setSessionResets(LuaValue /*int*/ sessionResets) {
            Julti.waitForExecute(() -> {
                ResetCounter.sessionCounter = sessionResets.checkint();
                ResetCounter.updateFiles();
                Julti.log(Level.INFO, "Updated session reset counter to " + ResetCounter.sessionCounter + ".");
            });
            return NIL;
        }

        public LuaValue sleep(LuaValue /*long*/ millis) {
            SleepUtil.sleep(millis.checklong());
            return NIL;
        }

        public LuaValue waitForInstanceLaunch(LuaValue /*int*/ instanceNum) {
            MinecraftInstance instance = getInstanceFromInt(instanceNum);
            while ((!this.cancelRequester.isCancelRequested())) {
                AtomicBoolean b = new AtomicBoolean(false);
                Julti.waitForExecute(() -> b.set(InstanceManager.getInstanceManager().getMatchingInstance(instance).hasWindow()));
                if (b.get()) {
                    break;
                }
                SleepUtil.sleep(50);
            }
            return NIL;
        }

        public LuaValue waitForInstancePreviewLoad(LuaValue /*int*/ instanceNum) {
            MinecraftInstance instance = getInstanceFromInt(instanceNum);
            while ((!this.cancelRequester.isCancelRequested())) {
                AtomicBoolean b = new AtomicBoolean(false);
                Julti.waitForExecute(() -> b.set(instance.getStateTracker().isCurrentState(InstanceState.PREVIEWING)));
                if (b.get()) {
                    break;
                }
                SleepUtil.sleep(50);
            }
            return NIL;
        }

        public LuaValue waitForInstanceLoad(LuaValue /*int*/ instanceNum) {
            MinecraftInstance instance = getInstanceFromInt(instanceNum);
            while ((!this.cancelRequester.isCancelRequested())) {
                AtomicBoolean b = new AtomicBoolean(false);
                Julti.waitForExecute(() -> b.set(instance.getStateTracker().isCurrentState(InstanceState.INWORLD)));
                if (b.get()) {
                    break;
                }
                SleepUtil.sleep(50);
            }
            return NIL;
        }

        public LuaValue pressEscOnInstance(LuaValue /*int*/ instanceNum) {
            Julti.waitForExecute(() -> {
                getInstanceFromInt(instanceNum).getKeyPresser().pressEsc();
            });
            return NIL;
        }

        public LuaValue getInstanceCount() {
            return valueOf(InstanceManager.getInstanceManager().getSize());
        }

        public LuaValue getSelectedInstanceNum() {
            InstanceManager instanceManager = InstanceManager.getInstanceManager();
            return valueOf(instanceManager.getInstanceNum(instanceManager.getSelectedInstance()));
        }

        public LuaValue runCommand(LuaValue /*String*/ command) {
            CommandManager.getMainManager().runCommand(command.checkjstring(), this.cancelRequester);
            return NIL;
        }

        public LuaValue focusWall() {
            Julti.waitForExecute(() -> Julti.getJulti().focusWall());
            return NIL;
        }

        public LuaValue runScript(LuaValue /*String*/ scriptName) {
            ScriptManager.runScriptAndWait(scriptName.checkjstring());
            return NIL;
        }

        public LuaValue setGlobal(LuaValue /*String*/ key, LuaValue /*anything*/ val) {
            GLOBALS_MAP.put(key.checkjstring(), val);
            return NIL;
        }

        public LuaValue getGlobal(LuaValue /*String*/ key, LuaValue /*anything*/ def) {
            return GLOBALS_MAP.getOrDefault(key.checkjstring(), def);
        }

        public LuaValue getInstanceState(LuaValue /*int*/ instanceNum){
            MinecraftInstance instance = getInstanceFromInt(instanceNum);
            return valueOf(instance.getStateTracker().getInstanceState().name());
        }

        public LuaValue getInstanceInWorldState(LuaValue /*int*/ instanceNum){
            MinecraftInstance instance = getInstanceFromInt(instanceNum);
            return valueOf(instance.getStateTracker().getInWorldType().name());
        }

        // Julti Lua Library Functions End Here //

        @Override
        public LuaValue call(LuaValue modname, LuaValue env) {
            LuaValue library = tableOf();
            for (Method method : JultiLib.class.getDeclaredMethods()) {
                if (method.getName().equals("call")) {
                    continue;
                }
                library.set(method.getName(), convertToArgFunctionObj(this, method));
            }
            env.set("julti", library);
            return library;
        }
    }
}
