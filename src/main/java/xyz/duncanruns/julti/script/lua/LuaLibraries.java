package xyz.duncanruns.julti.script.lua;

import org.luaj.vm2.Globals;
import xyz.duncanruns.julti.cancelrequester.CancelRequester;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class LuaLibraries {
    private static final List<Function<CancelRequester, LuaLibrary>> LIBRARY_PROVIDERS = new ArrayList<>();

    public static void registerLuaLibrary(Function<CancelRequester, LuaLibrary> libraryProvider) {
        LIBRARY_PROVIDERS.add(libraryProvider);
    }

    public static List<Function<CancelRequester, LuaLibrary>> getLibraryProviders() {
        return LIBRARY_PROVIDERS;
    }

    public static void addLibraries(Globals globals, CancelRequester cr) {
        LIBRARY_PROVIDERS.forEach(provider -> globals.load(provider.apply(cr)));
    }
}
