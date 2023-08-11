package xyz.duncanruns.julti.plugin;

import xyz.duncanruns.julti.JultiOptions;

import java.nio.file.Path;

public final class PluginManager {
    private static final PluginManager INSTANCE = new PluginManager();
    private static final Path PLUGINS_PATH = JultiOptions.getJultiDir().resolve("plugins").toAbsolutePath();

    private PluginManager() {
    }

    public static PluginManager getPluginManager() {
        return INSTANCE;
    }
}
