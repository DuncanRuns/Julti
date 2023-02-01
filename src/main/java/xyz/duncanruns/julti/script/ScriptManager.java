package xyz.duncanruns.julti.script;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.util.LogReceiver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class ScriptManager {
    private static final Logger LOGGER = LogManager.getLogger("Script Manager");
    private static final Path SCRIPTS_PATH = JultiOptions.getJultiDir().resolve("scripts.txt");
    private static final List<Script> SCRIPTS = new CopyOnWriteArrayList<>();
    private static AtomicBoolean scriptRunning = new AtomicBoolean(false);

    public static void reload() {
        String scriptsFileContents = "";
        if (Files.exists(SCRIPTS_PATH)) {
            try {
                scriptsFileContents = Files.readString(SCRIPTS_PATH);
            } catch (IOException ignored) {
            }
        }

        SCRIPTS.clear();
        // For every whitespace stripped line in the file, if it is a savable string, add it as a script
        Arrays.stream(scriptsFileContents.split("\n")).map(String::strip).filter(s -> !s.isEmpty()).forEach(s -> {
            if (Script.isSavableString(s)) {
                SCRIPTS.add(Script.fromSavableString(s));
            }
        });
    }

    public static boolean runScript(Julti julti, String scriptName) {
        return runScript(julti, scriptName, false, (byte) 0);
    }

    public static boolean runScript(Julti julti, String scriptName, boolean fromHotkey, byte hotkeyContext) {
        final AtomicBoolean running = scriptRunning;

        if (running.get()) return false;

        Script script = getScript(scriptName);
        if (!(
                script != null && (!fromHotkey || (script.getHotkeyContext() & hotkeyContext) > 0)
        )) return false;

        new Thread(() -> {
            running.set(true);
            String[] commands = script.getCommands().split(";");

            for (int i = 0; i < commands.length && running.get(); i++) {
                julti.runCommand(commands[i]);
            }

            running.set(false);
        }, "script-runner").start();
        return true;
    }

    private static Script getScript(String scriptName) {
        for (Script script : SCRIPTS) {
            if (script.getName().equalsIgnoreCase(scriptName.strip())) {
                return script;
            }
        }
        return null;
    }

    public static boolean isDuplicateImport(String scriptString) {
        if (!Script.isSavableString(scriptString)) return false;
        Script script = Script.fromSavableString(scriptString);
        return getScript(script.getName()) != null;
    }

    public static void requestCancel() {
        if (!scriptRunning.get()) return;
        scriptRunning.set(false);
        scriptRunning = new AtomicBoolean(false);
        log(Level.INFO, "Script canceled");
    }

    public static void log(Level level, String message) {
        LOGGER.log(level, message);
        LogReceiver.receive(level, message);
    }

    public static boolean forceAddScript(String savableString) {
        if (!Script.isSavableString(savableString)) {
            return false;
        }
        Script newScript = Script.fromSavableString(savableString);
        removeScript(newScript.getName());
        addScript(savableString);
        return true;
    }

    public static boolean removeScript(String name) {
        if (SCRIPTS.removeIf(script -> script.getName().equalsIgnoreCase(name.strip()))) {
            save();
            return true;
        }
        return false;
    }

    public static boolean addScript(String savableString) {
        if (!Script.isSavableString(savableString)) {
            return false;
        }
        Script newScript = Script.fromSavableString(savableString);
        // If any script name matches the new script name
        if (SCRIPTS.stream().anyMatch(script -> script.getName().equalsIgnoreCase(newScript.getName()))) {
            return false;
        }
        SCRIPTS.add(newScript);
        save();
        return true;
    }

    private static void save() {
        JultiOptions.ensureJultiDir();
        StringBuilder out = new StringBuilder(500);
        SCRIPTS.forEach(script -> out.append(script.toSavableString()).append("\n"));
        try {
            Files.writeString(SCRIPTS_PATH, out.toString().strip());
        } catch (IOException ignored) {
        }
    }

    public static List<String> getScriptNames() {
        return SCRIPTS.stream().map(Script::getName).collect(Collectors.toList());
    }

    public static List<String> getHotkeyableScriptNames() {
        return SCRIPTS.stream().filter(s -> s.getHotkeyContext() > 0).map(Script::getName).collect(Collectors.toList());
    }

    public static byte getHotkeyContext(String name) {
        Script script = getScript(name);
        return script == null ? -1 : script.getHotkeyContext();
    }
}
