package xyz.duncanruns.julti.plugin;

import com.google.gson.Gson;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.util.ExceptionUtil;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class PluginManager {
    private static final PluginManager INSTANCE = new PluginManager();
    private static final Gson GSON = new Gson();
    private static final Path PLUGINS_PATH = JultiOptions.getJultiDir().resolve("plugins").toAbsolutePath();

    private final List<JultiPluginData> loadedPluginDataList = new ArrayList<>();

    private PluginManager() {
    }

    public static PluginManager getPluginManager() {
        return INSTANCE;
    }

    private static void importJar(File file) throws Exception {
        Method addURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        addURL.setAccessible(true);
        addURL.invoke(URLClassLoader.getSystemClassLoader(), file.toURI().toURL());
    }

    @SuppressWarnings("all") //Suppress the redundant cast warning which resolves an ambiguous case
    private static String getJarJPJContents(Path jarPath) throws IOException {
        try (FileSystem fs = FileSystems.newFileSystem(jarPath, (ClassLoader) null)) {
            Path jsonFilePath = fs.getPath("julti.plugin.json");
            byte[] jsonData = Files.readAllBytes(jsonFilePath);
            return new String(jsonData, StandardCharsets.UTF_8);
        }
    }

    public void loadPluginsFromFolder() throws IOException {
        if (!Files.exists(PLUGINS_PATH)) {
            return;
        }
        Files.list(PLUGINS_PATH).filter(path -> path.getFileName().toString().endsWith(".jar")).forEach(path -> {
            try {
                this.checkPluginJar(path);
            } catch (Exception e) {
                Julti.log(Level.WARN, "Failed to load plugin " + path + "!\n" + ExceptionUtil.toDetailedString(e));
            }
        });
    }

    /**
     * Checks the plugin jar to see if it has a unique id, then loads its class files and runs its initializer.
     */
    private void checkPluginJar(Path path) throws Exception {
        JultiPluginData jultiPluginData = JultiPluginData.fromString(getJarJPJContents(path));
        if (this.registerPlugin(jultiPluginData)) {
            importJar(path.toFile());
        } else {
            Julti.log(Level.WARN, "Failed to load plugin " + path + ", because there is another plugin with the same id already loaded.");
        }
    }

    /**
     * Loads a plugin from a plugin data object.
     *
     * @param jultiPluginData the plugin data object
     *
     * @return true if the plugin has a unique id, otherwise false
     */
    public boolean registerPlugin(JultiPluginData jultiPluginData) {
        if (this.loadedPluginDataList.isEmpty() || this.loadedPluginDataList.stream().noneMatch(jultiPluginData::matchesOther)) {
            this.loadedPluginDataList.add(jultiPluginData);
            return true;
        }
        return false;
    }

    public void initializePlugins() {
        this.loadedPluginDataList.forEach(pluginData -> {
            if (pluginData.initializer == null || pluginData.initializer.isEmpty()) {
                return;
            }
            try {
                PluginInitializer pluginInitializer = (PluginInitializer) Class.forName(pluginData.initializer).newInstance();
                pluginInitializer.initialize();
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static class JultiPluginData {
        public String name = null;
        public String id = null;
        public String version = null;
        public String initializer = null;

        public static JultiPluginData fromString(String string) {
            return GSON.fromJson(string, JultiPluginData.class);
        }

        public boolean matchesOther(JultiPluginData other) {
            return other != null && this.id.equals(other.id);
        }
    }
}
