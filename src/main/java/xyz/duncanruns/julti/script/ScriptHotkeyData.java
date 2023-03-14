package xyz.duncanruns.julti.script;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ScriptHotkeyData {
    public final String scriptName;
    public boolean ignoreModifiers;
    public List<Integer> keys;

    public ScriptHotkeyData(String scriptName, boolean ignoreModifiers, List<Integer> keys) {
        this.scriptName = scriptName;
        this.ignoreModifiers = ignoreModifiers;
        this.keys = keys;
    }

    public static ScriptHotkeyData parseString(String string) {
        try {
            int lastColonLocation = -1;
            for (int i = 0; i < string.length(); i++) {
                if (string.charAt(i) == ':') {
                    lastColonLocation = i;
                }
            }

            if (lastColonLocation == -1) return null;

            final String[] imAndKeys = string.substring(lastColonLocation + 1).split(";");

            final List<Integer> keys = imAndKeys.length == 1 ? Collections.emptyList() : Arrays.stream(imAndKeys[1].split(",")).map(Integer::parseInt).collect(Collectors.toList());
            final boolean ignoreModifiers = Boolean.parseBoolean(imAndKeys[0]);
            final String scriptName = string.substring(0, lastColonLocation);

            return new ScriptHotkeyData(scriptName, ignoreModifiers, keys);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String toString() {
        final StringBuilder keysOut = new StringBuilder();
        this.keys.forEach(integer -> {
            if (!keysOut.toString().isEmpty()) { keysOut.append(","); }
            keysOut.append(integer);
        });
        return this.scriptName + ":" + this.ignoreModifiers + ";" + keysOut;
    }
}
