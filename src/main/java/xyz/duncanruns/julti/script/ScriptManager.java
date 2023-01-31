package xyz.duncanruns.julti.script;

import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ScriptManager {
    private static final Path SCRIPTS_PATH = JultiOptions.getJultiDir().resolve("scripts.txt");
    private static final List<Script> SCRIPTS = new CopyOnWriteArrayList<>();
    private static boolean alreadyRunning = false;
    private static boolean cancel = false;

    public static void initialize() {
        String scriptsFileContents = "";
        if (Files.exists(SCRIPTS_PATH)) {
            try {
                scriptsFileContents = Files.readString(SCRIPTS_PATH);
            } catch (IOException ignored) {
            }
        }

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
        if (alreadyRunning) return false;
        Script script = getScript(scriptName);
        if (script == null || (fromHotkey && script.getHotkeyContext() != hotkeyContext)) return false;

        alreadyRunning = true;

        new Thread(() -> {
            String[] commands = script.getCommands().split(";");

            for (int i = 0; i < commands.length && !cancel; i++) {
                julti.runCommand(commands[i]);
            }

            alreadyRunning = false;
            cancel = false;
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

    public static void requestCancel() {
        if (alreadyRunning) cancel = true;
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

    public static boolean removeScript(String name) {
        if (SCRIPTS.removeIf(script -> script.getName().equalsIgnoreCase(name.strip()))) {
            save();
            return true;
        }
        return false;
    }
}
