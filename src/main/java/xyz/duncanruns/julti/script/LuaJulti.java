package xyz.duncanruns.julti.script;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.DebugLib;
import org.luaj.vm2.lib.LibFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;
import xyz.duncanruns.julti.cancelrequester.CancelRequester;
import xyz.duncanruns.julti.command.CommandManager;
import xyz.duncanruns.julti.management.InstanceManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

@SuppressWarnings("unused")
public class LuaJulti {
    private static final String JULTI_REQUIRE = "require(\"" + JultiLib.class.getName() + "\")\n";

    public static void runLuaScript(String luaScript, CancelRequester cancelRequester) {
        Globals globals = JsePlatform.standardGlobals();
        globals.load(new InterruptibleDebugLib(cancelRequester));
        LuaValue chunk = JsePlatform.standardGlobals().load(JULTI_REQUIRE + luaScript);
        chunk.call();
    }

    private static LibFunction convertToArgFunctionObj(Method method) {
        return new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                ArrayList<LuaValue> argVals = new ArrayList<>(args.narg());
                for (int i = 1; i <= args.narg(); i++) {
                    argVals.add(args.arg(i));
                }
                try {
                    return (LuaValue) method.invoke(null, argVals.toArray());
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
        };
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
                throw new RuntimeException("Cancel Requested");
            }
            super.onInstruction(pc, v, top);
        }

    }

    public static class JultiLib extends TwoArgFunction {

        public static LuaValue reset(LuaValue arg) {
            InstanceManager.getInstanceManager().getInstances().get(arg.checkint() - 1).reset();
            return LuaValue.NIL;
        }

        public static LuaValue runcommand(LuaValue arg) {
            CommandManager.getMainManager().runCommand(arg.checkjstring()); //TODO: get a cancel requester somehow (probably have a global cancel requester for lua calls?)
            return LuaValue.NIL;
        }

        @Override
        public LuaValue call(LuaValue modname, LuaValue env) {
            LuaValue library = tableOf();
            for (Method method : JultiLib.class.getDeclaredMethods()) {
                if (method.getName().equals("call")) {
                    continue;
                }
                library.set(method.getName(), convertToArgFunctionObj(method));
            }
            env.set("julti", library);
            return library;
        }
    }
}
