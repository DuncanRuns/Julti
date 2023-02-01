package xyz.duncanruns.julti.script;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ScriptHotkeyData {
    public String scriptName;
    public boolean ignoreModifiers;
    public List<Integer> keys;

    public ScriptHotkeyData(String scriptName, boolean ignoreModifiers, List<Integer> keys) {
        this.scriptName = scriptName;
        this.ignoreModifiers = ignoreModifiers;
        this.keys = keys;
    }

    public static ScriptHotkeyData parseString(String string) {
        try {
            final String[] nameAndArgs = string.split(":");
            final String[] imAndKeys = nameAndArgs[1].split(";");

            final List<Integer> keys = imAndKeys.length == 1 ? Collections.emptyList() : Arrays.stream(imAndKeys[1].split(",")).map(Integer::parseInt).collect(Collectors.toList());
            final boolean ignoreModifiers = Boolean.parseBoolean(imAndKeys[0]);
            final String scriptName = nameAndArgs[0];
            return new ScriptHotkeyData(scriptName, ignoreModifiers, keys);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String toString() {
        final StringBuilder keysOut = new StringBuilder();
        keys.forEach(integer -> {
            if (!keysOut.toString().isEmpty()) keysOut.append(",");
            keysOut.append(integer);
        });
        return scriptName + ":" + ignoreModifiers + ";" + keysOut;
    }
}
