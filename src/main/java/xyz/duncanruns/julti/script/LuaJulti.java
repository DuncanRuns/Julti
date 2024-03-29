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
import xyz.duncanruns.julti.cancelrequester.CancelRequester;
import xyz.duncanruns.julti.command.CommandManager;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.management.InstanceManager;
import xyz.duncanruns.julti.util.DoAllFastUtil;
import xyz.duncanruns.julti.util.ExceptionUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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

        public LuaValue reset(LuaValue arg) {
            Julti.waitForExecute(() -> InstanceManager.getInstanceManager().getInstances().get(arg.checkint() - 1).reset());
            return LuaValue.NIL;
        }

        public LuaValue resetAll() {
            Julti.waitForExecute(() -> DoAllFastUtil.doAllFast(MinecraftInstance::reset));
            return LuaValue.NIL;
        }

        public LuaValue runCommand(LuaValue arg) {
            CommandManager.getMainManager().runCommand(arg.checkjstring(), this.cancelRequester);
            return LuaValue.NIL;
        }

        public LuaValue setGlobal(LuaValue key, LuaValue val) {
            GLOBALS_MAP.put(key.checkjstring(), val);
            return LuaValue.NIL;
        }

        public LuaValue getGlobal(LuaValue key, LuaValue def) {
            return GLOBALS_MAP.getOrDefault(key.checkjstring(), def);
        }

        public LuaValue log(LuaValue message) {
            Julti.log(Level.INFO, message.checkjstring());
            return LuaValue.NIL;
        }

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
