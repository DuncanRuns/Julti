package xyz.duncanruns.julti.script;

import xyz.duncanruns.julti.cancelrequester.CancelRequester;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public abstract class Script {
    public static Optional<Script> tryLoad(Path path) {
        try {
            return Optional.of(load(path));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private static Script load(Path path) throws IOException {
        if (path.getFileName().toString().endsWith(".lua")) {
            return LuaScript.load(path);
        }
        return LegacyScript.load(path);
    }

    public abstract byte getHotkeyContext();

    public abstract void run(CancelRequester cancelRequester);

    public abstract Path getPath();

    public abstract String getName();

    @Override
    public int hashCode() {
        return this.getName().hashCode();
    }
}
