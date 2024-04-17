package xyz.duncanruns.julti.script.lua;

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
import java.util.*;

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

    public static List<String> extractCustomizables(String script) throws ParseException {
        LuaParser p = new LuaParser(new StringReader(script));
        Chunk chunk = p.Chunk();

        Set<String> varNames = new HashSet<>();
        List<String> out = new ArrayList<>();

        chunk.accept(new Visitor() {
            @Override
            public void visit(Exp.FuncCall exp) {
                String extracted = extractScriptSection(script, exp.beginLine, exp.endLine, exp.beginColumn, exp.endColumn);
                if (extracted.replaceAll("\\s", "").contains("julti.customizable(")) {
                    try {
                        CancelRequester requester = new CancelRequester();
                        Globals globals = makeCustomizableExtractorGlobals(out, varNames, requester);
                        LuaValue executableChunk = globals.load(extracted.substring(extracted.indexOf("julti.customizable")));
                        Thread thread = new Thread(() -> {
                            try {
                                executableChunk.call();
                            } catch (Exception ignored) {
                            }
                        });
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
        return out;
    }

    private static Globals makeCustomizableExtractorGlobals(List<String> strings, Set<String> varNames, CancelRequester requester) {
        Globals globals = getSafeGlobals();
        globals.load(new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue modname, LuaValue env) {
                LuaValue library = tableOf();
                library.set("customizable", new VarArgFunction() {
                    @Override
                    public Varargs invoke(Varargs args) {
                        if (!varNames.contains(args.arg1().checkjstring()) && args.narg() == 4) {
                            varNames.add(args.arg1().checkjstring());
                            strings.add(args.arg(1).tojstring());
                            strings.add(args.arg(2).tojstring());
                            strings.add(args.arg(3).tojstring());
                            strings.add(args.arg(4).tojstring());
                        }
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
