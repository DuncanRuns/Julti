package xyz.duncanruns.julti;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonObject;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.script.ScriptHotkeyData;
import xyz.duncanruns.julti.util.ExceptionUtil;
import xyz.duncanruns.julti.util.FileUtil;
import xyz.duncanruns.julti.util.MonitorUtil;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class JultiOptions {
    private final static Gson GSON_WRITER = new GsonBuilder().setPrettyPrinting().create();
    private final static Gson GSON_OBJECT_MAKER = new Gson();
    private final static Map<String, Consumer<JsonObject>> PLUGIN_DATA_LOADER = new HashMap<>();
    private final static Map<String, Supplier<JsonObject>> PLUGIN_DATA_SAVER = new HashMap<>();
    private static JultiOptions INSTANCE = null;

    private static final int MAX_THREADS = Runtime.getRuntime().availableProcessors();

    private transient final Path location;
    private transient final String profileName;

    // Reset
    public boolean useF3 = true;
    public boolean unpauseOnSwitch = true;
    public String clipboardOnReset = "";
    public String resetStyle = "Wall";
    public boolean coopMode = false;

    // Wall
    public boolean wallLockInsteadOfPlay = true;
    public boolean wallSmartSwitch = false;
    public boolean wallBypass = true;
    public boolean returnToWallIfNoneLoaded = true;
    public boolean autoCalcWallSize = true;
    public int overrideRowsAmount = 3;
    public int overrideColumnsAmount = 3;
    public long wallResetCooldown = 150L;
    public float lockedInstanceSpace = 16.666668f;
    public boolean dwReplaceLocked = true;
    public boolean doDirtCovers = false;
    public boolean useFreezeFilter = false;
    public int freezePercent = 80;

    // Window
    public boolean letJultiMoveWindows = true;
    public boolean useBorderless = false;
    public boolean maximizeWhenPlaying = true;
    public boolean maximizeWhenResetting = true;

    public int[] playingWindowSize = MonitorUtil.getPrimaryMonitor().size;
    public int[] resettingWindowSize = this.playingWindowSize;
    public int[] windowPos = MonitorUtil.getPrimaryMonitor().centerPosition;
    public boolean windowPosIsCenter = true;

    public boolean prepareWindowOnLock = false;

    // Hotkeys
    public List<Integer> resetHotkey = Collections.singletonList((int) 'U');
    public List<Integer> bgResetHotkey = Collections.emptyList();
    public List<Integer> wallResetHotkey = Collections.singletonList((int) 'T');
    public List<Integer> wallSingleResetHotkey = Collections.singletonList((int) 'E');
    public List<Integer> wallLockHotkey = Arrays.asList(160, 1);
    public List<Integer> wallPlayHotkey = Collections.singletonList((int) 'R');
    public List<Integer> wallFocusResetHotkey = Collections.singletonList((int) 'F');
    public List<Integer> cancelScriptHotkey = Arrays.asList(164, 46);
    public List<Integer> wallPlayLockHotkey = Collections.emptyList();

    public boolean resetHotkeyIM = true;
    public boolean bgResetHotkeyIM = true;
    public boolean wallResetHotkeyIM = true;
    public boolean wallSingleResetHotkeyIM = true;
    public boolean wallLockHotkeyIM = true;
    public boolean wallPlayHotkeyIM = true;
    public boolean wallFocusResetHotkeyIM = true;
    public boolean wallPlayLockHotkeyIM = true;
    public boolean cancelScriptHotkeyIM = true;

    public List<String> scriptHotkeys = new ArrayList<>();

    // OBS
    public int instanceSpacing = 0;
    public boolean useCustomWallWindow = false;
    public String customWallNameFormat = "* projector (scene) - *";
    public boolean invisibleDirtCovers = false;
    public boolean centerAlignActiveInstance = false;
    public float centerAlignScaleX = 1f;
    public float centerAlignScaleY = 1f;
    public boolean showInstanceIndicators = true;

    // Other
    public String multiMCPath = "";
    public boolean launchOffline = false;
    public String launchOfflineName = "Instance*";
    public long launchDelay = 500;
    public int resetCounter = 0;
    public boolean minimizeToTray = false;


    // Affinity
    public boolean useAffinity = true;
    public int threadsPlaying = Math.max(1, MAX_THREADS);
    public int threadsPrePreview = this.threadsPlaying;
    public int threadsStartPreview = this.threadsPlaying;
    public int threadsPreview = (int) Math.floor(Math.min(MAX_THREADS, Math.max(4, 0.25f * MAX_THREADS)));
    public int threadsWorldLoaded = this.threadsPreview;
    public int threadsLocked = this.threadsPlaying;
    public int threadsBackground = (int) Math.floor(Math.min(MAX_THREADS, Math.max(4, 0.25f * MAX_THREADS)));
    public int affinityBurst = 1000;

    // Sounds
    public String singleResetSound = JultiOptions.getJultiDir().resolve("sounds").resolve("click.wav").toAbsolutePath().toString();
    public float singleResetVolume = 0.7f;
    public String multiResetSound = JultiOptions.getJultiDir().resolve("sounds").resolve("click.wav").toAbsolutePath().toString();
    public float multiResetVolume = 0.7f;
    public String lockSound = JultiOptions.getJultiDir().resolve("sounds").resolve("plop.wav").toAbsolutePath().toString();
    public float lockVolume = 0.7f;
    public String playSound = "";
    public float playVolume = 0.0f;

    // Experimental
    public boolean enableExperimentalOptions = false;
    public boolean showDebug = false;
    public boolean autoFullscreen = false;
    public boolean fullscreenBeforeUnpause = true;
    public boolean usePlayingSizeWithFullscreen = true;
    public int fullscreenDelay = 50;
    public boolean pieChartOnLoad = false;
    public boolean preventWindowNaming = false;
    public boolean alwaysOnTopProjector = false;
    public boolean minimizeProjectorWhenPlaying = false;
    public boolean activateProjectorOnReset = false;
    public boolean useAltSwitching = false;
    public boolean allowResetDuringGenerating = false;
    public boolean resizeableBorderless = false;
    // public boolean forceActivate = false;

    // Launching
    public List<String> launchingProgramPaths = new ArrayList<>();

    // Plugins
    public Map<String, JsonObject> pluginData = new HashMap<>();

    // Hidden
    public List<String> instancePaths = new ArrayList<>();
    public int[] lastGUIPos = new int[]{0, 0};
    public String lastCheckedVersion = "v0.0.0";

    public JultiOptions(Path location) {
        this.location = location;
        this.profileName = location == null ? null : FilenameUtils.removeExtension(location.getFileName().toString());
    }

    public static void registerPluginDataLoader(String pluginId, Consumer<JsonObject> dataConsumer) {
        PLUGIN_DATA_LOADER.put(pluginId, dataConsumer);
    }

    public static void registerPluginDataSaver(String pluginId, Supplier<JsonObject> dataProvider) {
        PLUGIN_DATA_SAVER.put(pluginId, dataProvider);
    }

    public static JultiOptions getJultiOptions() {
        return getJultiOptions(false);
    }

    public static JultiOptions getJultiOptions(boolean reload) {
        if (reload) {
            INSTANCE = null;
        }
        if (INSTANCE == null) {
            INSTANCE = new JultiOptions(getSelectedProfilePath());
            INSTANCE.tryLoad();
        }
        return INSTANCE;
    }

    public static Path getSelectedProfilePath() {
        Path jultiDir = getJultiDir();
        Path selectedFilePath = jultiDir.resolve("selectedprofile.txt");
        if (Files.isRegularFile(selectedFilePath)) {
            try {
                String name = FileUtil.readString(selectedFilePath).trim();
                if (name.isEmpty()) {
                    name = "default";
                }
                return jultiDir.resolve("profiles").resolve(name + ".json");
            } catch (Exception e) {
                Julti.log(Level.ERROR, "Exception during getSelectedProfilePath:\n" + ExceptionUtil.toDetailedString(e));
            }
        } else {
            try {
                FileUtil.writeString(selectedFilePath, "default");
            } catch (IOException e) {
                Julti.log(Level.ERROR, "Exception during getSelectedProfilePath:\n" + ExceptionUtil.toDetailedString(e));
            }
        }
        return jultiDir.resolve("profiles").resolve("default.json");
    }

    public static Path getJultiDir() {
        return Paths.get(System.getProperty("user.home")).resolve(".Julti").toAbsolutePath();
    }

    public static boolean tryChangeProfile(String profileName) {
        Path selectedFilePath = getJultiDir().resolve("selectedprofile.txt");
        try {
            ensureJultiDir();
            FileUtil.writeString(selectedFilePath, profileName);
            getJultiOptions(true);
            return true;
        } catch (Exception e) {
            Julti.log(Level.ERROR, "Failed to change profile:\n" + ExceptionUtil.toDetailedString(e));
            return false;
        }
    }

    public static void ensureJultiDir() {
        // Special care is needed to make a .Julti folder for some reason...
        // Using Files.createDirectories on a path.getParent() would create .Julti as a file for some reason.
        new File((System.getProperty("user.home") + "/.Julti/").replace("\\", "/").replace("//", "/")).mkdirs();
    }

    /**
     * Returns a String array of profile names where the first element is the name of the currently selected array.
     *
     * @return a String array of profile names where the first element is the name of the currently selected array.
     */
    public static String[] getProfileNames() {
        ArrayList<String> names = new ArrayList<>();
        String first = getSelectedProfileName();
        names.add(first);
        String[] profiles = getJultiDir().resolve("profiles").toFile().list();
        if (profiles == null || profiles.length == 0) {
            profiles = new String[]{"default"};
        }
        Arrays.stream(profiles).iterator().forEachRemaining(s -> {
            if (s.endsWith(".json")) {
                String nextName = s.substring(0, s.length() - 5);
                if (!nextName.equals(first)) {
                    names.add(nextName);
                }
            }
        });
        return names.toArray(new String[0]);
    }

    public static String getSelectedProfileName() {
        Path selectedPath = getSelectedProfilePath();
        String fileName = selectedPath.getName(selectedPath.getNameCount() - 1).toString();
        return fileName.substring(0, fileName.length() - 5);
    }

    public static boolean removeProfile(String profileName) {
        Path resolve = getSelectedProfilePath().resolveSibling(profileName + ".json");
        return resolve.toFile().delete();
    }

    public static JultiOptions getDefaults() {
        return new JultiOptions(null);
    }

    public boolean tryLoad() {
        if (Files.isRegularFile(this.location)) {
            try {
                // Regular gson's fromJson can't load json strings into existing objects and can only create new objects, this is a work-around.
                String jsonString = FileUtil.readString(this.location);
                OldOptions oldOptions = GSON_OBJECT_MAKER.fromJson(jsonString, OldOptions.class);
                Gson gson = new GsonBuilder().registerTypeAdapter(JultiOptions.class, (InstanceCreator<?>) type -> this).create();
                gson.fromJson(jsonString, JultiOptions.class);
                this.processOldOptions(oldOptions);
                return true;
            } catch (Exception e) {
                Julti.log(Level.ERROR, "Failed to load options:\n" + ExceptionUtil.toDetailedString(e));
            }
        }
        return false;
    }

    public void triggerPluginDataLoaders() {
        PLUGIN_DATA_LOADER.forEach((pluginId, dataConsumer) -> dataConsumer.accept(this.pluginData.getOrDefault(pluginId, new JsonObject())));
    }

    private void processOldOptions(OldOptions oldOptions) {
        List<String> changes = new ArrayList<>();
        if (oldOptions.lastInstances != null) {
            // Just a name change, no logging needed
            this.instancePaths = oldOptions.lastInstances;
        }
        if (oldOptions.unsquishOnLock != null) {
            this.prepareWindowOnLock = oldOptions.unsquishOnLock;
        }
        if (oldOptions.noCopeMode != null && oldOptions.noCopeMode) {
            changes.add("No Cope Mode was previously enabled, but has been removed");
        }
        if (oldOptions.dirtReleasePercent != null) {
            this.doDirtCovers = oldOptions.dirtReleasePercent >= 0;
            changes.add("A percentage was set for dirt covers, this option has been removed");
        }
        if (oldOptions.windowSize != null) {
            this.resettingWindowSize = this.playingWindowSize = oldOptions.windowSize;
        }
        if (oldOptions.wideResetSquish != null) {
            changes.add("Wide reset squish has been replaced by resetting window size, your old settings have been converted");
            this.resettingWindowSize = new int[]{this.resettingWindowSize[0], (int) (this.resettingWindowSize[1] / oldOptions.wideResetSquish)};
        }
        if (oldOptions.pauseOnLoad != null && !oldOptions.pauseOnLoad) {
            changes.add("The \"Pause on Load\" was disabled, the option has been removed and worlds will now always pause on load");
        }

        if (oldOptions.obsWindowNameFormat != null) {
            changes.add("The obsWindowNameFormat has been removed and replaced by a better detection system. If you were not using OBS to run your wall, you can enable the new custom wall option in Julti options.");
            this.customWallNameFormat = oldOptions.obsWindowNameFormat;
        }

        if (oldOptions.useMaximizeWithFullscreen != null) {
            changes.add("Window management options have had major changes, please ensure things are still working correctly.");

            // Update old window options to new ones
            this.maximizeWhenResetting = false;
            this.windowPosIsCenter = false;

            if (this.autoFullscreen) {
                this.maximizeWhenPlaying = oldOptions.useMaximizeWithFullscreen;
            }
        }

        if (oldOptions.resetMode != null) {
            switch (oldOptions.resetMode) {
                case 0:
                    this.resetStyle = "Multi";
                    break;
                case 1:
                    this.resetStyle = "Wall";
                    break;
                case 2:
                    this.resetStyle = "Dynamic Wall";
                    break;
            }
        }

        if (oldOptions.showInstanceIndicators == null) {
            this.showInstanceIndicators = false;
        }

        if (oldOptions.fullscreenDelay == null) {
            this.fullscreenDelay = 0;
        }

        if (changes.isEmpty()) {
            return;
        }

        StringBuilder out = new StringBuilder("--------------------\nYou have updated Julti from an older version, the following changes to your options have been made:");
        changes.forEach(s -> out.append("\n- ").append(s));
        out.append("\n--------------------");
        Julti.doLater(() -> {
            for (String s : out.toString().split("\n")) {
                Julti.log(Level.INFO, s);
            }
        });
    }

    public boolean trySave() {
        try {
            // Trigger plugin data savers
            PLUGIN_DATA_SAVER.forEach((string, jsonObjectSupplier) -> this.pluginData.put(string, jsonObjectSupplier.get()));
            ensureJultiDir();
            Files.createDirectories(this.location.getParent());
            FileUtil.writeString(this.location, GSON_WRITER.toJson(this));
            return true;
        } catch (Exception e) {
            Julti.log(Level.ERROR, "Failed to save options:\n" + ExceptionUtil.toDetailedString(e));
            return false;
        }
    }

    public String getProfileName() {
        return this.profileName;
    }

    public List<Path> getLastInstancePaths() {
        return this.instancePaths.stream().map(Paths::get).collect(Collectors.toList());
    }

    public String getValueString(String optionName) {
        Object value = this.getValue(optionName);
        if (value == null) {
            return null;
        }
        if (value.getClass().isArray()) {
            List<Object> objectList = new ArrayList<>();
            for (int i = 0; i < Array.getLength(value); i++) {
                objectList.add(Array.get(value, i));
            }
            value = objectList;
        }
        return String.valueOf(value);
    }

    @Nullable
    public Object getValue(String optionName) {
        Field optionField = null;
        try {
            optionField = this.getClass().getField(optionName);
        } catch (NoSuchFieldException ignored) {
            // Handled by nullability
        }
        if (optionField == null || Modifier.isTransient(optionField.getModifiers())) {
            return null;
        }
        try {
            return optionField.get(this);
        } catch (IllegalAccessException ignored) {
            // Handled by nullability
        }
        return null;
    }

    public ScriptHotkeyData getScriptHotkeyData(String scriptName) {
        ScriptHotkeyData out = null;

        for (ScriptHotkeyData replaceData : this.scriptHotkeys.stream().map(ScriptHotkeyData::parseString).filter(Objects::nonNull).collect(Collectors.toList())) {
            if (replaceData.scriptName.equals(scriptName)) {
                out = replaceData;
                break;
            }
        }

        if (out == null) {
            return new ScriptHotkeyData(scriptName, true, Collections.emptyList());
        }
        return out;
    }

    public void setScriptHotkey(ScriptHotkeyData data) {
        this.scriptHotkeys.removeIf(s -> {
            ScriptHotkeyData scriptHotkeyData = ScriptHotkeyData.parseString(s);
            if (scriptHotkeyData == null) {
                return true;
            }
            return scriptHotkeyData.scriptName.equals(data.scriptName);
        });
        this.scriptHotkeys.add(data.toString());
    }

    @Override
    public int hashCode() {
        return this.location.hashCode();
    }

    public List<String> getOptionNamesWithType() {
        List<String> names = new ArrayList<>();
        for (Field field : this.getClass().getFields()) {
            if (!Modifier.isTransient(field.getModifiers())) {
                names.add(field.getName() + " (" + field.getType().getSimpleName() + ")");
            }
        }
        return names;
    }

    /**
     * A magical all-encompassing option value setter that handles many types of objects.
     * <p>
     * If the option is a primitive field, the object will be converted to a String and then parsed to the
     * appropriate wrapper object before setting.
     * <p>
     * If the option is a String field, toString() will be called on the value object.
     * <p>
     * If the option is not a primitive or String field, the value will be directly set
     *
     * @param optionName the name of the field in this options object
     * @param value      the value object
     *
     * @return true if the value was set successfully, otherwise false
     */
    public boolean trySetValue(String optionName, Object value) {
        try {

            // Get field and class objects

            Field optionField = this.getClass().getField(optionName);
            Class<?> fieldClazz = optionField.getType();
            Class<?> valueClazz = value.getClass();

            // Convert the object if possible

            // If the field type is a primitive (or a primitive wrapper), we can convert the value object to the correct wrapper class
            if (ClassUtils.isPrimitiveOrWrapper(fieldClazz)) {
                // Get the wrapper class of the field
                Class<?> wrapperClazz = ClassUtils.primitiveToWrapper(fieldClazz);
                // Check if the value object is already the correct class
                if (valueClazz != wrapperClazz) {
                    // We can use the valueOf(String) static method from the wrapper class
                    value = wrapperClazz.getMethod("valueOf", String.class).invoke(null, value.toString());
                }
            } else if (fieldClazz == String.class) {
                // If the option field is a string field, we can use toString() to ensure the value object is a String
                value = value.toString();
            }

            // Set the field

            optionField.set(this, value);
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    public boolean tryCopyTo(String profileName) {
        try {
            ensureJultiDir();
            Files.createDirectories(this.location.getParent());
            FileUtil.writeString(this.location.resolveSibling(profileName + ".json"), GSON_WRITER.toJson(this));
            return true;
        } catch (Exception e) {
            Julti.log(Level.ERROR, "Failed to copy profile:\n" + ExceptionUtil.toDetailedString(e));
            return false;
        }
    }

    private static class OldOptions {
        public int[] windowSize = null;
        public Float wideResetSquish = null;
        public Integer dirtReleasePercent = null;
        public Boolean pauseOnLoad = null;
        public List<String> lastInstances = null;
        public Boolean noCopeMode = null;
        public Boolean unsquishOnLock = null;
        public Boolean cleanWall = null;
        public String obsWindowNameFormat = null;
        public Integer resetMode = null;
        public Boolean useMaximizeWithFullscreen = null;
        public Boolean showInstanceIndicators = null;
        public Integer fullscreenDelay = null;
    }
}
