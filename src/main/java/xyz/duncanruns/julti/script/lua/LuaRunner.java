package xyz.duncanruns.julti.script.lua;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Level;
import org.luaj.vm2.*;
import org.luaj.vm2.ast.Chunk;
import org.luaj.vm2.ast.Exp;
import org.luaj.vm2.ast.Visitor;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseMathLib;
import org.luaj.vm2.parser.LuaParser;
import org.luaj.vm2.parser.ParseException;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.cancelrequester.CancelRequester;
import xyz.duncanruns.julti.script.LuaScript;
import xyz.duncanruns.julti.util.ExceptionUtil;
import xyz.duncanruns.julti.util.SleepUtil;

import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class LuaRunner {
    public static final Map<String, LuaValue> GLOBALS_MAP = Collections.synchronizedMap(new HashMap<>());

    public static void runLuaScript(LuaScript script, CancelRequester cancelRequester) {
        Globals globals = getSafeGlobals();
        globals.load(new InterruptibleDebugLib(cancelRequester));
        globals.load(new JultiLuaLibrary(cancelRequester, script));
        LuaLibraries.addLibraries(globals, cancelRequester);
        LuaValue chunk = globals.load(script.getContents());
        try {
            chunk.call();
        } catch (LuaError e) {
            if (!(e.getCause() instanceof LuaScriptCancelledException)) {
                Julti.log(Level.ERROR, "Error while executing script: " + ExceptionUtil.toDetailedString(e.getCause() != null ? e.getCause() : e));
            }
        }
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

    public static Map<String, Pair<String, String>> extractCustomizables(String script) throws ParseException {
        LuaParser p = new LuaParser(new StringReader(script));
        Chunk chunk = p.Chunk();

        Map<String, Pair<String, String>> map = new HashMap<>(); // Customizable name -> <Type, Description>

        chunk.accept(new Visitor() {
            @Override
            public void visit(Exp.FuncCall exp) {
                String extracted = extractScriptSection(script, exp.beginLine, exp.endLine, exp.beginColumn, exp.endColumn);
                if (extracted.replaceAll("\\s", "").contains("julti.customizable(\"")) {
                    try {
                        CancelRequester requester = new CancelRequester();
                        Globals globals = makeCustomizableExtractorGlobals(map, requester);
                        String executableExtract = extracted.substring(extracted.indexOf("julti.customizable"));
                        Thread thread = new Thread(() -> globals.load(executableExtract).call());
                        thread.start();
                        long start = System.currentTimeMillis();
                        while (thread.isAlive()) {
                            SleepUtil.sleep(1);
                            if (Math.abs(System.currentTimeMillis() - start) > 100) {
                                requester.cancel();
                            }
                        }
                    } catch (Throwable ignored) {
                    }
                }
                super.visit(exp);
            }
        });
        return map;
    }

    private static Globals makeCustomizableExtractorGlobals(Map<String, Pair<String, String>> map, CancelRequester requester) {
        Globals globals = getSafeGlobals();
        globals.load(new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue modname, LuaValue env) {
                LuaValue library = tableOf();
                library.set("customizable", new ThreeArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
                        map.put(arg1.checkjstring(), Pair.of(arg2.checkjstring(), arg3.checkjstring()));
                        return NIL;
                    }
                });
                env.set("julti", library);
                return library;
            }
        });
        globals.load(new InterruptibleDebugLib(requester));
        return globals;
    }

    private static String extractScriptSection(String script, int beginLine, int endLine, int beginCol, int endCol) {
        String[] lines = script.split("\n");

        if (beginLine == endLine) {
            return lines[beginLine - 1].substring(beginCol - 1, endCol);
        }
        StringBuilder out = new StringBuilder(lines[beginLine - 1].substring(beginCol - 1));
        for (int i = beginLine; i <= endLine - 2; i++) {
            out.append("\n").append(lines[i]);
        }
        out.append("\n").append(lines[endLine - 1], 0, endCol);
        return out.toString();
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
