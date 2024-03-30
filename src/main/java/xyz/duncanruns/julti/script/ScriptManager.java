package xyz.duncanruns.julti.script;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.cancelrequester.CancelRequester;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ScriptManager {
    public static Path SCRIPTS_FOLDER = JultiOptions.getJultiDir().resolve("scripts");
    public static Set<Script> SCRIPTS = new HashSet<>();

    public static CancelRequester cancelRequester = new CancelRequester();

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

    public static void writeLegacyScript(String s) throws IOException {
        String[] parts = s.split(";");
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
        FileUtil.writeString(SCRIPTS_FOLDER.resolve(newName + ".txt"), contents.toString());
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
        CancelRequester oldCR = cancelRequester;
        cancelRequester = new CancelRequester();
        oldCR.cancel();
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
        findScript(name).ifPresent(script -> new Thread(
                () -> script.run(ScriptManager.cancelRequester),
                "script-thread-" + name
        ).start());
    }

    public static void runScriptAndWait(String name) {
        findScript(name).ifPresent(script -> {
            try {
                script.run(ScriptManager.cancelRequester);
            } catch (Throwable t) {
                Julti.log(Level.ERROR, "Failed to run script: " + ExceptionUtil.toDetailedString(t));
            }
        });
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
}
