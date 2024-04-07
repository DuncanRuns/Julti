package xyz.duncanruns.julti.script.lua;

import org.apache.logging.log4j.Level;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.DebugLib;
import org.luaj.vm2.lib.jse.JsePlatform;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.cancelrequester.CancelRequester;
import xyz.duncanruns.julti.util.ExceptionUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class LuaRunner {
    public static final Map<String, LuaValue> GLOBALS_MAP = Collections.synchronizedMap(new HashMap<>());

    public static void runLuaScript(String luaScript, CancelRequester cancelRequester) {
        Globals globals = JsePlatform.standardGlobals();
        globals.load(new InterruptibleDebugLib(cancelRequester));
        LuaLibraries.addLibraries(globals, cancelRequester);
        LuaValue chunk = globals.load(luaScript);
        try {
            chunk.call();
        } catch (LuaError e) {
            if (!(e.getCause() instanceof LuaScriptCancelledException)) {
                Julti.log(Level.ERROR, "Error while executing script: " + ExceptionUtil.toDetailedString(e.getCause()));
            }
        }
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
}
