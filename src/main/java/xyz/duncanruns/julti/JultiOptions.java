package xyz.duncanruns.julti;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import org.apache.commons.io.FilenameUtils;
import xyz.duncanruns.julti.util.FileUtil;
import xyz.duncanruns.julti.util.MonitorUtil;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public final class JultiOptions {
    private final static Gson GSON_WRITER = new GsonBuilder().setPrettyPrinting().create();
    private static JultiOptions INSTANCE = null;

    private transient final Path location;
    private transient final String profileName;

    // Reset
    public boolean pauseOnLoad = true;
    public boolean useF3 = true;
    public String clipboardOnReset = "";
    public boolean useWall = false;
    public boolean wallOneAtATime = false;

    // Window
    public boolean useBorderless = false;
    public int[] windowPos = MonitorUtil.getDefaultMonitor().position;
    public int[] windowSize = MonitorUtil.getDefaultMonitor().size;

    // Automated Tasks
    public boolean autoHide = false;
    public int autoHideTime = 5;
    public boolean autoClearWorlds = true;

    // Hotkeys
    public List<Integer> resetHotkey = Collections.singletonList(0x55);
    public List<Integer> hideHotkey = Collections.singletonList(0x50);
    public List<Integer> bgResetHotkey = Collections.singletonList(0xDB);
    public List<Integer> wallResetHotkey = Collections.singletonList(0x55);
    public List<Integer> wallSingleResetHotkey = Collections.singletonList((int) 'E');
    public List<Integer> wallLockHotkey = Collections.singletonList(1);
    public List<Integer> wallPlayHotkey = Collections.singletonList(2);
    public HashMap<String, List<Integer>> extraHotkeys = new HashMap<>();

    // OBS
    public boolean obsPressHotkey = true;
    public boolean obsUseNumpad = true;
    public boolean obsUseAlt = false;

    // Hidden
    public List<String> lastInstances = new ArrayList<>();

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
            changeProfile("default");
        }
        return jultiDir.resolve("profiles").resolve("default.json");
    }

    public boolean tryLoad() {
        if (Files.isRegularFile(location)) {
            try {
                // Regular gson's fromJson can't load json strings into existing objects and can only create new objects, this is a work-around.
                Gson gson = new GsonBuilder().registerTypeAdapter(JultiOptions.class, (InstanceCreator<?>) type -> this).create();
                gson.fromJson(FileUtil.readString(location), JultiOptions.class);
                return true;
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    public static Path getJultiDir() {
        return Paths.get(System.getProperty("user.home")).resolve(".Julti");
    }

    public static boolean changeProfile(String profileName) {
        Path selectedFilePath = getJultiDir().resolve("selectedprofile.txt");
        try {
            ensureJultiDir();
            FileUtil.writeString(selectedFilePath, profileName);
            INSTANCE = null;
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static void ensureJultiDir() {
        // Special care is needed to make a .Julti folder for some reason...
        // Using Files.createDirectories on a path.getParent() would create .Julti as a file for some reason.
        new File((System.getProperty("user.home") + "/.Julti/").replace("\\", "/").replace("//", "/")).mkdirs();
    }

    public String testJson() {
        return GSON_WRITER.toJson(this);
    }

    public boolean trySave() {
        try {
            ensureJultiDir();
            Files.createDirectories(location.getParent());
            FileUtil.writeString(location, GSON_WRITER.toJson(this));
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public String getProfileName() {
        return profileName;
    }

    public List<Path> getLastInstancePaths() {
        List<Path> instancePaths = new ArrayList<>(lastInstances.size());
        for (String instanceStr : lastInstances) {
            instancePaths.add(Paths.get(instanceStr));
        }
        return Collections.unmodifiableList(instancePaths);
    }

    @Override
    public int hashCode() {
        return location.hashCode();
    }
}
