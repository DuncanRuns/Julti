package xyz.duncanruns.julti.script;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.cancelrequester.CancelRequester;
import xyz.duncanruns.julti.cancelrequester.CancelRequesterManager;
import xyz.duncanruns.julti.util.ExceptionUtil;
import xyz.duncanruns.julti.util.FileUtil;
import xyz.duncanruns.julti.util.LauncherUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ScriptManager {
    public static Path SCRIPTS_FOLDER = JultiOptions.getJultiDir().resolve("scripts");
    public static Set<Script> SCRIPTS = new HashSet<>();
    public static final Pattern GIST_PATTERN = Pattern.compile("^(https://gist\\.github\\.com/\\w+/)\\w+$");
    public static final Pattern LEGACY_CODE_PATTERN = Pattern.compile("^.+; ?\\d ?;.+$");

    public static CancelRequesterManager requesterManager = new CancelRequesterManager();

    public static void reload() {
        SCRIPTS.clear();
        ensureScriptsDir();
        convertOldScripts();

        try (Stream<Path> files = Files.list(SCRIPTS_FOLDER)) {
            files.forEach(path -> Script.tryLoad(path).ifPresent(script -> {
                if (findScript(script.getName()).isPresent()) {
                    Julti.log(Level.WARN, "You have multiple scripts with the same name (" + script.getName() + "), only one will be loaded!");
                } else {
                    SCRIPTS.add(script);
                }
            }));
        } catch (IOException e) {
            Julti.log(Level.ERROR, "Failed to load scripts: " + ExceptionUtil.toDetailedString(e));
        }
    }

    private static void convertOldScripts() {
        Path scriptsPath = JultiOptions.getJultiDir().resolve("scripts.txt");
        if (!Files.isRegularFile(scriptsPath)) {
            return;
        }
        String out;
        try {
            out = FileUtil.readString(scriptsPath);
        } catch (IOException e) {
            Julti.log(Level.ERROR, "Failed to convert old scripts: " + ExceptionUtil.toDetailedString(e));
            return;
        }

        for (String s : out.split("\n")) {
            try {
                writeLegacyScript(s);
            } catch (NumberFormatException | IOException ignored) {
            }
        }
        if (!Julti.VERSION.equals("DEV")) {
            try {
                Files.deleteIfExists(scriptsPath);
            } catch (IOException ignored) {
                // bruh
            }
        }
    }

    public static void writeLegacyScript(String legacyCode) throws IOException {
        String[] parts = legacyCode.split(";");
        String name = parts[0];
        StringBuilder contents = new StringBuilder();
        switch (Byte.parseByte(parts[1])) {
            case 1:
                contents.append("# hotkey-context=game");
                break;
            case 2:
                contents.append("# hotkey-context=wall");
                break;
            case 3:
                contents.append("# hotkey-context=anywhere");
                break;
        }

        for (int i = 0; i < parts.length; i++) {
            if (i < 2) {
                continue;
            }
            contents.append("\n").append(parts[i].trim());
        }

        // Old script name might not be file name compatible
        String newName = name.replaceAll("[\\\\/:*?\"<>|]", "");
        if (!newName.equals(name)) {
            // Old script name isn't file name compatible so convert hotkey option to new compatible name as well
            JultiOptions options = JultiOptions.getJultiOptions();
            options.scriptHotkeys.stream().filter(shds -> shds.startsWith(name + ":")).findAny().ifPresent(shds -> {
                options.scriptHotkeys.remove(shds);
                options.scriptHotkeys.add(newName + shds.substring(name.length()));
                options.trySave();
            });
        }
        writeLegacyScript(newName, contents.toString());
    }

    public static void writeLegacyScript(String name, String contents) throws IOException {
        name = name.replaceAll("[\\\\/:*?\"<>|]", "");
        FileUtil.writeString(SCRIPTS_FOLDER.resolve(name + ".txt"), contents);
    }

    public static void writeLuaScript(String name, String contents) throws IOException {
        name = name.replaceAll("[\\\\/:*?\"<>|]", "");
        FileUtil.writeString(SCRIPTS_FOLDER.resolve(name + ".lua"), contents);
    }

    public static void writeScript(String name, String contents, boolean isLegacy) throws IOException {
        if (isLegacy) {
            writeLegacyScript(name, contents);
        } else {
            writeLuaScript(name, contents);
        }
    }

    private static void ensureScriptsDir() {
        if (!Files.isDirectory(SCRIPTS_FOLDER)) {
            try {
                Files.createDirectory(SCRIPTS_FOLDER);
            } catch (IOException ignored) {
            }
        }
    }

    public static List<String> getScriptNames() {
        return SCRIPTS.stream().map(Script::getName).collect(Collectors.toList());
    }

    public static void cancelAllScripts() {
        requesterManager.cancelAll();
    }

    public static void deleteScript(String name) {
        findScript(name).ifPresent(script -> {
            try {
                Files.deleteIfExists(script.getPath());
            } catch (IOException ignored) {
            }
            SCRIPTS.remove(script);
        });
    }

    public static List<String> getHotkeyableScriptNames() {
        return SCRIPTS.stream().filter(script -> script.getHotkeyContext() > 0).map(Script::getName).collect(Collectors.toList());
    }

    public static byte getHotkeyContext(String name) {
        return findScript(name).map(Script::getHotkeyContext).orElse((byte) -1);
    }

    public static void runScript(String name) {
        new Thread(() -> runScriptAndWait(name), "script-thread-" + name.toLowerCase().replace(" ", "")).start();
    }

    public static void runScriptAndWait(String name) {
        Optional<Script> scriptOpt = findScript(name);
        if (!scriptOpt.isPresent()) {
            return;
        }
        Script script = scriptOpt.get();
        if (!script.allowsParallelRunning() && requesterManager.isActive(name)) {
            return;
        }
        CancelRequester cancelRequester = requesterManager.createNew(name);
        try {
            script.run(cancelRequester);
        } catch (Throwable t) {
            Julti.log(Level.ERROR, "Failed to run script: " + ExceptionUtil.toDetailedString(t));
        }
    }

    private static Optional<Script> findScript(String name) {
        return SCRIPTS.stream().filter(script -> script.getName().equals(name)).findAny();
    }

    public static boolean openScriptForEditing(String name) {
        return findScript(name).map(script -> {
            LauncherUtil.openFile(script.getPath().toString());
            return true;
        }).orElse(false);
    }

    public static boolean isLegacyImportCode(String string) {
        return LEGACY_CODE_PATTERN.matcher(string).matches();
    }

    public static boolean isGist(String string) {
        return GIST_PATTERN.matcher(string).matches();
    }

}
