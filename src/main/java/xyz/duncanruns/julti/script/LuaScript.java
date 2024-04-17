package xyz.duncanruns.julti.script;

import org.luaj.vm2.parser.ParseException;
import xyz.duncanruns.julti.cancelrequester.CancelRequester;
import xyz.duncanruns.julti.script.lua.LuaRunner;
import xyz.duncanruns.julti.util.FileUtil;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class LuaScript extends Script {
    private final String name;
    private final Path path;
    private final String contents;
    private final byte hotkeyContext;
    private final boolean allowParallel;

    private LuaScript(Path path, String contents) {
        this.path = path;
        this.contents = contents;
        this.name = this.path.getFileName().toString().split("\\.")[0];
        this.hotkeyContext = extractHotkeyContext(contents);
        this.allowParallel = extractAllowParallel(contents);
    }

    private static byte extractHotkeyContext(String contents) {
        for (String s : contents.split("\n")) {
            s = s.trim().replace("-- ", "--");
            if (s.startsWith("--hotkey-context=")) {
                switch (s.substring(17)) {
                    case "game":
                        return 1;
                    case "wall":
                        return 2;
                    case "anywhere":
                        return 3;
                }
            }
        }
        return 0;
    }

    private static boolean extractAllowParallel(String contents) {
        for (String s : contents.split("\n")) {
            s = s.trim().replace("-- ", "--");
            if (s.startsWith("--allow-parallel=")) {
                switch (s.substring(17)) {
                    case "true":
                        return true;
                    case "false":
                        return false;
                }
            }
        }
        return false;
    }

    public static LuaScript load(Path path) throws IOException {
        String contents = FileUtil.readString(path);
        return new LuaScript(path, contents);
    }

    @Override
    public byte getHotkeyContext() {
        return this.hotkeyContext;
    }

    @Override
    public void run(CancelRequester cancelRequester) {
        LuaRunner.runLuaScript(this, cancelRequester);
    }

    @Override
    public Path getPath() {
        return this.path;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public boolean allowsParallelRunning() {
        return this.allowParallel;
    }

    @Override
    public List<String> getCustomizables() {
        try {
            return LuaRunner.extractCustomizables(this.contents);
        } catch (ParseException e) {
            return Collections.emptyList();
        }
    }

    public String getContents() {
        return this.contents;
    }
}
