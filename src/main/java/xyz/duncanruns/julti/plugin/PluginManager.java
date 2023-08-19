package xyz.duncanruns.julti.plugin;

import com.google.gson.Gson;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.util.ExceptionUtil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@SuppressWarnings({"ConstantValue", "DataFlowIssue"})
public final class PluginManager {
    private static final PluginManager INSTANCE = new PluginManager();
    private static final Gson GSON = new Gson();
    private static final Path PLUGINS_PATH = JultiOptions.getJultiDir().resolve("plugins").toAbsolutePath();

    private final List<LoadedJultiPlugin> loadedPlugins = new ArrayList<>();

    private PluginManager() {
    }

    public static PluginManager getPluginManager() {
        return INSTANCE;
    }

    private static PluginInitializer importJar(File file, String initializer) throws Exception {
        // https://stackoverflow.com/questions/11016092/how-to-load-classes-at-runtime-from-a-folder-or-jar
        JarFile jarFile = new JarFile(file);
        Enumeration<JarEntry> e = jarFile.entries();

        URL[] urls = {new URL("jar:file:" + file + "!/")};
        URLClassLoader cl = URLClassLoader.newInstance(urls);

        while (e.hasMoreElements()) {
            JarEntry je = e.nextElement();
            if (je.isDirectory() || !je.getName().endsWith(".class")) {
                continue;
            }
            // -6 because of .class
            String className = je.getName().substring(0, je.getName().length() - 6);
            className = className.replace('/', '.');
            cl.loadClass(className);
        }

        PluginInitializer pi = (PluginInitializer) cl.loadClass(initializer).newInstance();
        jarFile.close();
        return pi;
    }

    @SuppressWarnings("all") //Suppress the redundant cast warning which resolves an ambiguous case
    private static String getJarJPJContents(Path jarPath) throws IOException {
        try (FileSystem fs = FileSystems.newFileSystem(jarPath, (ClassLoader) null)) {
            Path jsonFilePath = fs.getPath("julti.plugin.json");
            byte[] jsonData = Files.readAllBytes(jsonFilePath);
            return new String(jsonData, StandardCharsets.UTF_8);
        }
    }

    public List<LoadedJultiPlugin> getLoadedPlugins() {
        return Collections.unmodifiableList(this.loadedPlugins);
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
        if (this.canRegister(jultiPluginData)) {
            PluginInitializer pluginInitializer = importJar(path.toFile(), jultiPluginData.initializer);
            this.registerPlugin(jultiPluginData, pluginInitializer);
        } else {
            Julti.log(Level.WARN, "Failed to load plugin " + path + ", because there is another plugin with the same id already loaded.");
        }
    }

    /**
     * Loads a plugin from a plugin data object and an initializer.
     *
     * @param data the plugin data object
     */
    public void registerPlugin(JultiPluginData data, PluginInitializer initializer) {
        this.loadedPlugins.add(new LoadedJultiPlugin(data, initializer));
    }

    private boolean canRegister(JultiPluginData data) {
        return this.loadedPlugins.isEmpty() || this.loadedPlugins.stream().map(p -> p.pluginData).noneMatch(data::matchesOther);
    }

    public void initializePlugins() {
        this.loadedPlugins.forEach(plugin -> plugin.pluginInitializer.initialize());
    }

    public static class JultiPluginData {
        public final String name = null;
        public final String id = null;
        public final String version = null;
        public final String initializer = null;

        public static JultiPluginData fromString(String string) {
            return GSON.fromJson(string, JultiPluginData.class);
        }

        public boolean matchesOther(JultiPluginData other) {
            return other != null && this.id.equals(other.id);
        }
    }

    public static class LoadedJultiPlugin {
        public final JultiPluginData pluginData;
        public final PluginInitializer pluginInitializer;

        private LoadedJultiPlugin(JultiPluginData data, PluginInitializer initializer) {
            this.pluginData = data;
            this.pluginInitializer = initializer;
        }
    }
}
