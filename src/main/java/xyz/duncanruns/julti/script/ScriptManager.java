package xyz.duncanruns.julti.script;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.cancelrequester.CancelRequester;
import xyz.duncanruns.julti.cancelrequester.CancelRequesters;
import xyz.duncanruns.julti.command.CommandManager;
import xyz.duncanruns.julti.util.ExceptionUtil;
import xyz.duncanruns.julti.util.FileUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static xyz.duncanruns.julti.Julti.log;

public class ScriptManager {
    private static final Path SCRIPTS_PATH = JultiOptions.getJultiDir().resolve("scripts.txt");
    private static final String DEFAULT_SCRIPTS = "Warmup + Launch;0;log Launching all instances...; launch all; wait launch all; log Waiting 5 seconds for title screen...; sleep 5000; log Activating all instances to remove mouse jank...; activate all; activate wall; log Waiting 8 seconds for obs pickup...; sleep 8000; log Resetting all and waiting for world load...; reset all; wait load all; log Unpausing all instances...; activate all; activate wall; log Waiting 20 seconds for warmup...; sleep 20000; log Resetting all and returning to wall...; reset all; activate wall; log Warmup done;\n" +
            "Start Coping;1;opentolan; chatmessage /gamemode spectator;\n" +
            "Fight Dragon;1;opentolan; chatmessage /gamemode creative; chatmessage /clear; chatmessage /effect give @s minecraft:saturation 10 100; chatmessage /replaceitem entity @s weapon.offhand bread 3; chatmessage /replaceitem entity @s hotbar.8 cobblestone 23; chatmessage /replaceitem entity @s hotbar.7 ender_pearl 5; chatmessage /give @s iron_axe; chatmessage /give @s iron_pickaxe; chatmessage /give @s iron_shovel; chatmessage /give @s water_bucket; chatmessage /give @s flint_and_steel; chatmessage /give @s bread 3; chatmessage /give @s string 60; chatmessage /give @s oak_planks 17; chatmessage /give @s obsidian 4; chatmessage /give @s crafting_table; chatmessage /gamemode survival; chatmessage /give @s oak_boat; chatmessage /setblock ~ ~ ~ end_portal;";
    private static final List<Script> SCRIPTS = new CopyOnWriteArrayList<>();
    private static CancelRequester cancelRequester = CancelRequesters.ALWAYS_CANCEL_REQUESTER; // Will change from fake requester to other requesters
    private static Thread runningScriptThread = null;

    private static final Object LOCK = new Object();

    public static void reload() {
        String scriptsFileContents = "";
        if (Files.exists(SCRIPTS_PATH)) {
            try {
                scriptsFileContents = FileUtil.readString(SCRIPTS_PATH);
            } catch (IOException e) {
                scriptsFileContents = DEFAULT_SCRIPTS;
            }
        } else {
            scriptsFileContents = DEFAULT_SCRIPTS;
        }

        SCRIPTS.clear();
        // For every whitespace stripped line in the file, if it is a savable string, add it as a script
        Arrays.stream(scriptsFileContents.split("\n")).map(String::trim).filter(s -> !s.isEmpty()).forEach(s -> {
            if (Script.isSavableString(s)) {
                SCRIPTS.add(Script.fromSavableString(s));
            }
        });
        save();
    }

    private static void save() {
        JultiOptions.ensureJultiDir();
        StringBuilder out = new StringBuilder(500);
        SCRIPTS.forEach(script -> out.append(script.toSavableString()).append("\n"));
        try {
            FileUtil.writeString(SCRIPTS_PATH, out.toString().trim());
        } catch (IOException e) {
            Julti.log(Level.ERROR, "Failed to save scripts:\n" + ExceptionUtil.toDetailedString(e));
        }
    }

    public static boolean runScript(String scriptName) {
        return runScript(scriptName, false, (byte) 0);
    }

    public static boolean runScript(String scriptName, boolean fromHotkey, byte hotkeyContext) {
        synchronized (LOCK) {
            return runScriptInternal(scriptName, fromHotkey, hotkeyContext);
        }
    }

    private static boolean runScriptInternal(String scriptName, boolean fromHotkey, byte hotkeyContext) {
        if ((runningScriptThread == null || !runningScriptThread.isAlive()) && !cancelRequester.isCancelRequested()) {
            cancelRequester.cancel();
        }

        if (!getScriptNames().contains(scriptName)) {
            log(Level.ERROR, "Could not run script " + scriptName + " because it does not exists.");
            return false;
        }

        if (!cancelRequester.isCancelRequested()) {
            return false;
        }

        runningScriptThread = null;
        cancelRequester = new CancelRequester();

        Script script = getScript(scriptName);
        if (!(
                script != null && (!fromHotkey || (script.getHotkeyContext() & hotkeyContext) > 0)
        )) {
            return false;
        }

        runningScriptThread = new Thread(() -> {
            try {
                String[] commands = script.getCommands().split(";");

                for (int i = 0; i < commands.length && !cancelRequester.isCancelRequested(); i++) {
                    CommandManager.getMainManager().runCommand(commands[i], cancelRequester);
                }

            } catch (Exception e) {
                Julti.log(Level.ERROR, "Error during script execution:\n" + ExceptionUtil.toDetailedString(e));
            } finally {
                cancelRequester.cancel();
            }
        }, "script-runner");
        runningScriptThread.start();
        return true;
    }

    public static List<String> getScriptNames() {
        return SCRIPTS.stream().map(Script::getName).collect(Collectors.toList());
    }

    private static Script getScript(String scriptName) {
        for (Script script : SCRIPTS) {
            if (script.getName().equalsIgnoreCase(scriptName.trim())) {
                return script;
            }
        }
        return null;
    }

    public static void requestCancel() {
        if (cancelRequester.cancel()) {
            log(Level.INFO, "Script canceled");
        }
    }

    public static boolean isDuplicateImport(String scriptString) {
        if (!Script.isSavableString(scriptString)) {
            return false;
        }
        Script script = Script.fromSavableString(scriptString);
        return getScript(script.getName()) != null;
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
        if (SCRIPTS.removeIf(script -> script.getName().equalsIgnoreCase(name.trim()))) {
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

    public static List<String> getHotkeyableScriptNames() {
        return SCRIPTS.stream().filter(s -> s.getHotkeyContext() > 0).map(Script::getName).collect(Collectors.toList());
    }

    public static byte getHotkeyContext(String name) {
        Script script = getScript(name);
        return script == null ? -1 : script.getHotkeyContext();
    }
}
