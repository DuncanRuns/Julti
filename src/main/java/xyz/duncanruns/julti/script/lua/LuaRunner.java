package xyz.duncanruns.julti.script.lua;

import org.apache.logging.log4j.Level;
import org.luaj.vm2.*;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseMathLib;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.cancelrequester.CancelRequester;
import xyz.duncanruns.julti.script.LuaScript;
import xyz.duncanruns.julti.util.ExceptionUtil;

@SuppressWarnings("unused")
public class LuaRunner {
    public static void runLuaScript(LuaScript script, CancelRequester cancelRequester) {
        Globals globals = getSafeGlobals();
        globals.load(new InterruptibleDebugLib(cancelRequester));
        globals.load(new JultiLuaLibrary(cancelRequester, script));
        LuaLibraries.addLibraries(globals, cancelRequester);
        LuaValue chunk = globals.load(script.getContents());
        try {
            chunk.call();
        } catch (LuaError e) {
            // Get Root Cause
            Throwable cause = ExceptionUtil.getRootCause(e);
            if (cause instanceof CustomizingException) {
                Julti.log(Level.ERROR, cause.getMessage());
            } else if (!(cause instanceof LuaScriptCancelledException)) {
                Julti.log(Level.ERROR, "Error while executing script: " + ExceptionUtil.toDetailedString(cause != null ? cause : e));
            }
        }
    }

    /**
     * Runs the script in customization mode, where libraries have their functions (ones not marked with the @AllowedWhileCustomizing annotation)
     * transformed into error-throwing functions that self report the invalid usage.
     *
     * @return true if julti.isCustomizing() was used at any point, otherwise
     */
    public static boolean customizeLuaScript(LuaScript script, CancelRequester cancelRequester) {
        Globals globals = getSafeGlobals();
        globals.load(new InterruptibleDebugLib(cancelRequester));
        CustomizingJultiLuaLibrary customLib = new CustomizingJultiLuaLibrary(cancelRequester, script);
        globals.load(customLib);
        LuaLibraries.addMockLibraries(globals, cancelRequester);
        LuaValue chunk = globals.load(script.getContents());
        try {
            chunk.call();
        } catch (LuaError e) {
            // Get Root Cause
            Throwable cause = ExceptionUtil.getRootCause(e);
            if (cause instanceof CustomizingException) {
                // Script almost certainly doesn't do customization
                if (customLib.hasCheckedCustomizing()) {
                    Julti.log(Level.ERROR, cause.getMessage());
                }
            } else if (!(cause instanceof LuaScriptCancelledException)) {
                Julti.log(Level.ERROR, "Error while customizing script: " + ExceptionUtil.toDetailedString(cause != null ? cause : e));
            }
        }
        return customLib.hasCheckedCustomizing();
    }

    private static Globals getSafeGlobals() {
        Globals globals = new Globals();
        globals.load(new JseBaseLib());
        globals.load(new PackageLib());
        globals.load(new Bit32Lib());
        globals.load(new TableLib());
        globals.load(new StringLib());
        globals.load(new JseMathLib());
        LoadState.install(globals);
        LuaC.install(globals);
        return globals;
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
