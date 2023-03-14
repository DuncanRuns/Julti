package xyz.duncanruns.julti;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import org.apache.commons.io.FilenameUtils;
import xyz.duncanruns.julti.script.ScriptHotkeyData;
import xyz.duncanruns.julti.util.FileUtil;
import xyz.duncanruns.julti.util.HotkeyUtil;
import xyz.duncanruns.julti.util.MonitorUtil;
import xyz.duncanruns.julti.win32.Win32Con;

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
import java.util.stream.Collectors;

@SuppressWarnings({"UnusedDeclaration", "CanBeFinal"})
public final class JultiOptions {
    private final static Gson GSON_WRITER = new GsonBuilder().setPrettyPrinting().create();
    private static JultiOptions INSTANCE = null;

    private transient final Path location;
    private transient final String profileName;

    // Reset
    public boolean pauseOnLoad = true;
    public boolean useF3 = true;
    public boolean unpauseOnSwitch = true;
    public String clipboardOnReset = "";
    public int resetMode = 0; // 0 = Multi, 1 = Wall, 2 = Dynamic Wall
    public boolean coopMode = false;
    public int dirtReleasePercent = 0;

    // Wall
    public boolean cleanWall = false;
    public boolean wallResetAllAfterPlaying = false;
    public boolean wallLockInsteadOfPlay = true;
    public boolean wallBypass = true;
    public boolean returnToWallIfNoneLoaded = true;
    public boolean autoCalcWallSize = true;
    public int overrideRowsAmount = 3;
    public int overrideColumnsAmount = 3;
    public long wallResetCooldown = 150L;
    public float lockedInstanceSpace = 16.666668f;
    public boolean dwReplaceLocked = true;

    // Window
    public boolean letJultiMoveWindows = true;
    public boolean useBorderless = false;
    public boolean unsquishOnLock = false;
    public int[] windowPos = MonitorUtil.getPrimaryMonitor().position;
    public int[] windowSize = MonitorUtil.getPrimaryMonitor().size;
    public float wideResetSquish = 1f;

    // Hotkeys
    public List<Integer> resetHotkey = Collections.singletonList((int) 'U');
    public List<Integer> bgResetHotkey = Collections.emptyList();
    public List<Integer> wallResetHotkey = Collections.singletonList((int) 'T');
    public List<Integer> wallSingleResetHotkey = Collections.singletonList((int) 'E');
    public List<Integer> wallLockHotkey = Collections.singletonList(1);
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
    public String obsWindowNameFormat = "* projector (scene) - *";
    public boolean obsPressHotkeys = false;
    public boolean obsUseNumpad = true;
    public boolean obsUseAlt = false;
    public List<Integer> switchToWallHotkey = Collections.singletonList(Win32Con.VK_F12);

    // Other
    public String multiMCPath = "";
    public boolean launchOffline = false;
    public String launchOfflineName = "Instance*";
    public int resetCounter = 0;

    // Affinity
    public boolean useAffinity = true;
    public int threadsPlaying = Math.max(1, AffinityManager.AVAILABLE_THREADS);
    public int threadsPrePreview = Math.max(1, AffinityManager.AVAILABLE_THREADS * 31 / 32);
    public int threadsStartPreview = Math.max(1, AffinityManager.AVAILABLE_THREADS * 28 / 32);
    public int threadsPreview = Math.max(1, AffinityManager.AVAILABLE_THREADS * 10 / 32);
    public int threadsWorldLoaded = Math.max(1, AffinityManager.AVAILABLE_THREADS * 8 / 32);
    public int threadsLocked = Math.max(1, AffinityManager.AVAILABLE_THREADS * 31 / 32);
    public int threadsBackground = Math.max(1, AffinityManager.AVAILABLE_THREADS * 8 / 32);
    public int affinityBurst = 300;

    // Sounds
    public String singleResetSound = JultiOptions.getJultiDir().resolve("sounds").resolve("click.wav").toAbsolutePath().toString();
    public float singleResetVolume = 0.7f;
    public String multiResetSound = JultiOptions.getJultiDir().resolve("sounds").resolve("click.wav").toAbsolutePath().toString();
    public float multiResetVolume = 0.7f;
    public String lockSound = JultiOptions.getJultiDir().resolve("sounds").resolve("plop.wav").toAbsolutePath().toString();
    public float lockVolume = 0.7f;
    public String playSound = "";
    public float playVolume = 0.0f;

    // Hidden
    public List<String> lastInstances = new ArrayList<>();
    public int[] lastGUIPos = new int[]{0, 0};
    public String lastCheckedVersion = "v0.0.0";
    public boolean noCopeMode = false;
    public boolean pieChartOnLoad = false;
    public boolean autoFullscreen = false;
    public boolean showDebug = false;
    public boolean preventWindowNaming = false;

    // Auto Reset
    public boolean autoResetForBeach = false;
    public boolean autoResetBackground = true;
    public boolean autoCheckAllOnWall = true;

    public JultiOptions(Path location) {
        this.location = location;
        this.profileName = FilenameUtils.removeExtension(location.getFileName().toString());
    }

    public static JultiOptions getInstance() {
        return getInstance(false);
    }

    public static JultiOptions getInstance(boolean reload) {
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
                String name = FileUtil.readString(selectedFilePath);
                if (name.isEmpty()) {
                    name = "default";
                }
                return jultiDir.resolve("profiles").resolve(name + ".json");
            } catch (Exception ignored) {
            }
        } else {
            try {
                FileUtil.writeString(selectedFilePath, "default");
            } catch (IOException ignored) {
            }
        }
        return jultiDir.resolve("profiles").resolve("default.json");
    }

    public void tryLoad() {
        if (Files.isRegularFile(this.location)) {
            try {
                // Regular gson's fromJson can't load json strings into existing objects and can only create new objects, this is a work-around.
                Gson gson = new GsonBuilder().registerTypeAdapter(JultiOptions.class, (InstanceCreator<?>) type -> this).create();
                gson.fromJson(FileUtil.readString(this.location), JultiOptions.class);
            } catch (Exception ignored) {}
        }
    }

    public static Path getJultiDir() {
        return Paths.get(System.getProperty("user.home")).resolve(".Julti");
    }

    public static void changeProfile(String profileName) {
        Path selectedFilePath = getJultiDir().resolve("selectedprofile.txt");
        try {
            ensureJultiDir();
            FileUtil.writeString(selectedFilePath, profileName);
            INSTANCE = null;
        } catch (Exception ignored) {}
    }

    public static void ensureJultiDir() {
        // Special care is needed to make a .Julti folder for some reason...
        // Using Files.createDirectories on a path.getParent() would create .Julti as a file for some reason.
        boolean ignored = new File((System.getProperty("user.home") + "/.Julti/").replace("\\", "/").replace("//", "/")).mkdirs();
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
            profiles = new String[] { "default" };
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

    public static void removeProfile(String profileName) {
        Path resolve = getSelectedProfilePath().resolveSibling(profileName + ".json");
        boolean ignored = resolve.toFile().delete();
    }

    public String testJson() {
        return GSON_WRITER.toJson(this);
    }

    public void trySave() {
        try {
            ensureJultiDir();
            Files.createDirectories(this.location.getParent());
            FileUtil.writeString(this.location, GSON_WRITER.toJson(this));
        } catch (Exception ignored) {}
    }

    public String getProfileName() {
        return this.profileName;
    }

    public List<Path> getLastInstancePaths() {
        return this.lastInstances.stream().map(Paths::get).collect(Collectors.toList());
    }

    public String getValueString(String optionName) {
        Object value = this.getValue(optionName);
        if (value == null) return null;
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
        try { optionField = getClass().getField(optionName); }
        catch (NoSuchFieldException ignored) {}

        if (optionField == null || Modifier.isTransient(optionField.getModifiers())) {
            return null;
        }
        if (optionField.getType().isPrimitive()) {
            // Basic value to change
            Class<?> clazz = optionField.getType();
            try {
                if (boolean.class == clazz) return optionField.getBoolean(this);
                if (byte.class == clazz) return optionField.getByte(this);
                if (short.class == clazz) return optionField.getShort(this);
                if (int.class == clazz) return optionField.getInt(this);
                if (long.class == clazz) return optionField.getLong(this);
                if (float.class == clazz) return optionField.getFloat(this);
                if (double.class == clazz) return optionField.getDouble(this);
            } catch (Exception e) {
                // This should theoretically never run
                return null;
            }
        } else {
            try { return optionField.get(this); }
            catch (IllegalAccessException ignored) {}
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public HotkeyUtil.Hotkey getHotkeyFromSetting(String name) {
        List<Integer> keys = (List<Integer>) getValue(name);
        Object value = getValue(name + "IM");
        boolean ignoreModifiers = value != null && (Boolean) value;
        return ignoreModifiers ? new HotkeyUtil.HotkeyIM(keys) : new HotkeyUtil.Hotkey(keys);
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
            if (scriptHotkeyData == null) { return true; }
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
        for (Field field : getClass().getFields()) {
            if (!Modifier.isTransient(field.getModifiers())) {
                names.add(field.getName() + " (" + field.getType().getSimpleName() + ")");
            }
        }
        return names;
    }

    public boolean trySetValue(String optionName, String valueString) {
        try {
            Field optionField = getClass().getField(optionName);
            if (optionField.getType().isPrimitive()) {
                Class<?> clazz = optionField.getType();
                if (boolean.class == clazz) optionField.setBoolean(this, Boolean.parseBoolean(valueString));
                if (byte.class == clazz) optionField.setByte(this, Byte.parseByte(valueString));
                if (short.class == clazz) optionField.setShort(this, Short.parseShort(valueString));
                if (int.class == clazz) optionField.setInt(this, Integer.parseInt(valueString));
                if (long.class == clazz) optionField.setLong(this, Long.parseLong(valueString));
                if (float.class == clazz) optionField.setFloat(this, Float.parseFloat(valueString));
                if (double.class == clazz) optionField.setDouble(this, Double.parseDouble(valueString));
                return true;
            } else if (optionField.getType().isArray()) {
                // Only int arrays exist for now, so assuming int.
                String[] words = valueString.split(" ");
                int[] ints = new int[words.length];
                for (int i = 0; i < ints.length; i++) {
                    String word = words[i];
                    if (word.endsWith(",")) {
                        word = word.substring(0, word.length() - 1);
                    }
                    ints[i] = Integer.parseInt(word);
                }
                optionField.set(this, ints);
                return true;
            } else {
                optionField.set(this, valueString);
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    public void trySetHotkey(String optionName, List<Integer> keys) {
        try {
            Field optionField = getClass().getField(optionName);
            optionField.set(this, keys);
        } catch (Exception ignored) {}
    }

    public void copyTo(String profileName) {
        try {
            ensureJultiDir();
            Files.createDirectories(this.location.getParent());
            FileUtil.writeString(this.location.resolveSibling(profileName + ".json"), GSON_WRITER.toJson(this));
        } catch (Exception ignored) {}
    }
}
