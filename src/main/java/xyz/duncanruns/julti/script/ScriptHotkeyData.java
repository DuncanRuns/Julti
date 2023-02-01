package xyz.duncanruns.julti.script;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
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
            AtomicInteger lastColonLocation = new AtomicInteger(-1);
            Pattern.compile(":").matcher(string).results().forEach(matchResult -> lastColonLocation.set(matchResult.start()));
            if (lastColonLocation.get() == -1) return null;

            final String[] imAndKeys = string.substring(lastColonLocation.get() + 1).split(";");

            final List<Integer> keys = imAndKeys.length == 1 ? Collections.emptyList() : Arrays.stream(imAndKeys[1].split(",")).map(Integer::parseInt).collect(Collectors.toList());
            final boolean ignoreModifiers = Boolean.parseBoolean(imAndKeys[0]);
            final String scriptName = string.substring(0, lastColonLocation.get());

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
