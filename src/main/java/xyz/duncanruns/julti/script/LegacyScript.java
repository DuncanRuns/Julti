package xyz.duncanruns.julti.script;

import xyz.duncanruns.julti.cancelrequester.CancelRequester;
import xyz.duncanruns.julti.command.CommandManager;
import xyz.duncanruns.julti.util.FileUtil;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class LegacyScript extends Script {
    private final List<String> commands;
    private final String name;
    private final Path path;
    private byte hotkeyContext = 0;

    public LegacyScript(Path path, String contents) {
        this.path = path;
        this.commands = new ArrayList<>();
        this.name = this.path.getFileName().toString().split("\\.")[0];

        for (String s : contents.split("\n")) {
            s = s.trim();
            if (s.startsWith("#")) {
                if (this.hotkeyContext == 0) {
                    this.tryExtractHotkeyContext(s);
                }
                continue;
            }
            this.commands.add(s);
        }
    }

    public static LegacyScript load(Path path) throws IOException {
        String contents = FileUtil.readString(path);
        return new LegacyScript(path, contents);
    }

    private void tryExtractHotkeyContext(String s) {
        s = s.replace("# ", "#");
        if (s.startsWith("#hotkey-context=")) {
            switch (s.substring(16)) {
                case "game":
                    this.hotkeyContext = 1;
                    break;
                case "wall":
                    this.hotkeyContext = 2;
                    break;
                case "anywhere":
                    this.hotkeyContext = 3;
                    break;
            }
        }
    }

    @Override
    public byte getHotkeyContext() {
        return this.hotkeyContext;
    }

    @Override
    public void run(CancelRequester cancelRequester) {
        for (String command : this.commands) {
            if (cancelRequester.isCancelRequested()) {
                return;
            }
            CommandManager.getMainManager().runCommand(command, cancelRequester);
        }
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
