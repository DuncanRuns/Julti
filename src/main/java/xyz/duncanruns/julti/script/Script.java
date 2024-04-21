package xyz.duncanruns.julti.script;

import xyz.duncanruns.julti.cancelrequester.CancelRequester;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public abstract class Script {
    public static Optional<Script> tryLoad(Path path) {
        try {
            return Optional.ofNullable(load(path));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private static Script load(Path path) throws IOException {
        String fileName = path.getFileName().toString();
        if (fileName.endsWith(".lua")) {
            return LuaScript.load(path);
        } else if (fileName.endsWith(".txt")) {
            return LegacyScript.load(path);
        }
        return null;
    }

    public abstract byte getHotkeyContext();

    public abstract void run(CancelRequester cancelRequester);

    public boolean customize(CancelRequester cancelRequester) {
        return false;
    }

    public abstract Path getPath();

    public abstract String getName();

    public abstract boolean allowsParallelRunning();

    @Override
    public int hashCode() {
        return this.getName().hashCode();
    }
}
