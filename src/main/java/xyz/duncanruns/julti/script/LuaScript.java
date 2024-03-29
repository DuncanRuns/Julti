package xyz.duncanruns.julti.script;

import xyz.duncanruns.julti.cancelrequester.CancelRequester;
import xyz.duncanruns.julti.util.FileUtil;

import java.io.IOException;
import java.nio.file.Path;

public class LuaScript extends Script {
    private final String name;
    private final Path path;
    private final String contents;
    private final byte hotkeyContext;

    private LuaScript(Path path, String contents) {
        this.path = path;
        this.contents = contents;
        this.name = this.path.getFileName().toString().split("\\.")[0];
        this.hotkeyContext = extractHotkeyContext(contents);
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
        LuaJulti.runLuaScript(this.contents, cancelRequester);
    }

    @Override
    public Path getPath() {
        return this.path;
    }

    @Override
    public String getName() {
        return this.name;
    }
}
