package xyz.duncanruns.julti.plugin;

import com.google.gson.Gson;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.util.ExceptionUtil;
import xyz.duncanruns.julti.util.ResourceUtil;
import xyz.duncanruns.julti.util.VersionUtil;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings({"ConstantValue", "DataFlowIssue"})
public final class PluginManager {
    private static final PluginManager INSTANCE = new PluginManager();
    private static final Gson GSON = new Gson();
    private static final Path PLUGINS_PATH = JultiOptions.getJultiDir().resolve("plugins").toAbsolutePath();

    private final List<LoadedJultiPlugin> loadedPlugins = new ArrayList<>();
    private final Set<String> pluginCollisions = new HashSet<>();

    private PluginManager() {
    }

    public static Path getPluginsPath() {
        return PLUGINS_PATH;
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
            try {
                cl.loadClass(className);
            } catch (Error nce) {
                // A fabric fail class is a class meant to crash loading with fabric. Useful to make sure players don't try to use Julti plugins as a fabric mod.
                // Julti fails to load them since they refer to a class that doesn't exist, so we ignore it.
                boolean isFabricFailClass = nce.getMessage().contains("net/fabricmc/api/ModInitializer");
                if (!isFabricFailClass) {
                    // If it is not a fabric fail class, we do want to warn for this
                    Julti.log(Level.WARN, "Failed to load class '" + className + "'! Julti may crash if this is needed by a plugin...");
                }
            }
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

    private static void warnWontLoad(JultiPluginData oldData) {
        Julti.log(Level.WARN, String.format("%s v%s will not load because it is not the newest version detected.", oldData.name, oldData.version));
    }

    public Set<String> getPluginCollisions() {
        return Collections.unmodifiableSet(this.pluginCollisions);
    }

    public List<LoadedJultiPlugin> getLoadedPlugins() {
        return Collections.unmodifiableList(this.loadedPlugins);
    }

    public List<Pair<Path, JultiPluginData>> getFolderPlugins() throws IOException {
        if (!Files.exists(PLUGINS_PATH)) {
            return Collections.emptyList();
        }
        List<Pair<Path, JultiPluginData>> plugins = new ArrayList<>();
        try (Stream<Path> list = Files.list(PLUGINS_PATH)) {
            list.filter(path -> path.getFileName().toString().endsWith(".jar")).forEach(path -> {
                try {
                    JultiPluginData data = JultiPluginData.fromString(getJarJPJContents(path));
                    plugins.add(Pair.of(path, data));
                } catch (Throwable e) {
                    Julti.log(Level.WARN, "Failed to read plugin " + path + "!\n" + ExceptionUtil.toDetailedString(e));
                }
            });
        }
        return plugins;
    }

    private List<Pair<Path, JultiPluginData>> getDefaultPlugins() throws IOException, URISyntaxException {
        List<String> fileNames = ResourceUtil.getResourcesFromFolder("defaultplugins").stream().map(s -> "/defaultplugins/" + s).collect(Collectors.toList());

        Julti.log(Level.DEBUG, "Default Plugins:" + fileNames);

        List<Pair<Path, JultiPluginData>> plugins = new ArrayList<>();

        for (String fileName : fileNames) {
            Path path = Paths.get(File.createTempFile(fileName, null).getPath());
            if (!fileName.startsWith("/")) {
                fileName = "/" + fileName;
            }
            ResourceUtil.copyResourceToFile(fileName, path);
            try {
                JultiPluginData data = JultiPluginData.fromString(getJarJPJContents(path));
                plugins.add(Pair.of(path, data));
            } catch (Exception e) {
                Julti.log(Level.ERROR, "Failed to read default plugin: " + fileName);
            }
        }
        return plugins;
    }

    public void loadPlugins() {
        List<Pair<Path, JultiPluginData>> folderPlugins = Collections.emptyList();
        List<Pair<Path, JultiPluginData>> defaultPlugins = Collections.emptyList();
        try {
            folderPlugins = this.getFolderPlugins();
        } catch (IOException e) {
            Julti.log(Level.ERROR, "Failed to load plugins from folder: " + ExceptionUtil.toDetailedString(e));
        }
        try {
            defaultPlugins = this.getDefaultPlugins();
        } catch (IOException | URISyntaxException e) {
            Julti.log(Level.ERROR, "Failed to load default plugins: " + ExceptionUtil.toDetailedString(e));
        }

        // Mod ID -> path and data
        Map<String, Pair<Path, JultiPluginData>> bestPluginVersions = new HashMap<>();

        Stream.concat(folderPlugins.stream(), defaultPlugins.stream()).forEach(pair -> {
            JultiPluginData data = pair.getRight();

            if (bestPluginVersions.containsKey(data.id)) {
                if (VersionUtil.tryCompare(data.version.split("\\+")[0], bestPluginVersions.get(data.id).getRight().version.split("\\+")[0], 0) > 0) {
                    JultiPluginData oldData = bestPluginVersions.get(data.id).getRight();
                    bestPluginVersions.put(data.id, pair);
                    warnWontLoad(oldData);
                } else {
                    warnWontLoad(data);
                }
            } else {
                bestPluginVersions.put(data.id, pair);
            }
        });

        // Check already loaded plugins (which can only be dev plugins because default and folder aren't registered yet)
        for (LoadedJultiPlugin loadedPlugin : this.getLoadedPlugins()) {
            String loadedDevPluginID = loadedPlugin.pluginData.id;
            if (bestPluginVersions.containsKey(loadedDevPluginID)) {
                Pair<Path, JultiPluginData> removed = bestPluginVersions.remove(loadedDevPluginID);
                JultiPluginData data = removed.getRight();
                Julti.log(Level.WARN, String.format("%s v%s will not load because a dev plugin will be initialized instead.", data.name, data.version));
            }
        }

        for (Map.Entry<String, Pair<Path, JultiPluginData>> entry : bestPluginVersions.entrySet()) {
            try {
                this.loadPluginJar(entry.getValue().getLeft(), entry.getValue().getRight());
            } catch (Exception e) {
                Julti.log(Level.ERROR, "Failed to load plugin from " + entry.getValue().getLeft() + ": " + ExceptionUtil.toDetailedString(e));
            }
        }
    }

    private void loadPluginJar(Path path, JultiPluginData jultiPluginData) throws Exception {
        if (this.canRegister(jultiPluginData)) {
            PluginInitializer pluginInitializer = importJar(path.toFile(), jultiPluginData.initializer);
            this.registerPlugin(jultiPluginData, pluginInitializer);
        } else {
            Julti.log(Level.WARN, "Failed to load plugin " + path + ", because there is another plugin with the same id already loaded.");
            this.pluginCollisions.add(jultiPluginData.id);
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

    public static class LoadedJultiPlugin {
        public final JultiPluginData pluginData;
        public final PluginInitializer pluginInitializer;

        private LoadedJultiPlugin(JultiPluginData data, PluginInitializer initializer) {
            this.pluginData = data;
            this.pluginInitializer = initializer;
        }
    }
}
