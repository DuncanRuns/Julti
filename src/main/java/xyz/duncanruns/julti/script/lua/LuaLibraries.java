package xyz.duncanruns.julti.script.lua;

import org.apache.logging.log4j.Level;
import org.luaj.vm2.Globals;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.cancelrequester.CancelRequester;
import xyz.duncanruns.julti.cancelrequester.CancelRequesters;
import xyz.duncanruns.julti.util.ExceptionUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public class LuaLibraries {
    private static final List<Function<CancelRequester, LuaLibrary>> LIBRARY_PROVIDERS = new ArrayList<>();

    public static void registerLuaLibrary(Function<CancelRequester, LuaLibrary> libraryProvider) {
        LIBRARY_PROVIDERS.add(libraryProvider);
    }

    public static List<Function<CancelRequester, LuaLibrary>> getLibraryProviders() {
        return LIBRARY_PROVIDERS;
    }

    static void addLibraries(Globals globals, CancelRequester cr) {
        LIBRARY_PROVIDERS.forEach(provider -> globals.load(provider.apply(cr)));
    }

    static void addMockLibraries(Globals globals, CancelRequester cancelRequester) {
        LIBRARY_PROVIDERS.forEach(f -> globals.load(f.apply(cancelRequester).asCustomizable()));
    }

    public static void generateDocs(Path folder) {
        Stream.concat(LIBRARY_PROVIDERS.stream(), Stream.of((cr) -> JultiLuaLibrary.forLibGen())).forEach(f -> {
            LuaLibrary library = f.apply(CancelRequesters.ALWAYS_CANCEL_REQUESTER);
            File file = folder.resolve(library.getLibraryName() + ".lua").toAbsolutePath().toFile();
            f.apply(CancelRequesters.ALWAYS_CANCEL_REQUESTER);
            try {
                FileWriter writer = new FileWriter(file);
                library.writeLuaFile(writer);
                writer.close();
            } catch (IOException e) {
                Julti.log(Level.ERROR, "Failed to write lua documentation for library " + library.getLibraryName() + ": " + ExceptionUtil.toDetailedString(e));
            }
        });
    }
}
