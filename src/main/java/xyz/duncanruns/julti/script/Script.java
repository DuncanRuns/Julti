package xyz.duncanruns.julti.script;

import java.util.regex.Pattern;

public class Script {
    private static final Pattern SAVABLE_STRING_PATTERN = Pattern.compile("^[^;]+;[0-4];.+$");
    private final String name;
    private final byte hotkeyContext;
    private final String commands; // ; separated commands

    public Script(String name, byte hotkeyContext, String commands) {
        this.name = name.trim();
        this.hotkeyContext = hotkeyContext;
        this.commands = commands.trim();
    }

    public static boolean isSavableString(String string) {
        return SAVABLE_STRING_PATTERN.matcher(string).matches();
    }

    public static Script fromSavableString(String string) {
        String name = string.substring(0, string.indexOf(';'));
        string = string.substring(string.indexOf(';') + 1);
        byte hotkeyContext = Byte.parseByte(string.substring(0, string.indexOf(';')));
        String commands = string.substring(string.indexOf(';') + 1);
        return new Script(name, hotkeyContext, commands);
    }

    public byte getHotkeyContext() {
        return this.hotkeyContext;
    }

    public String getCommands() {
        return this.commands;
    }

    public String toSavableString() {
        return this.name + ";" + this.hotkeyContext + ";" + this.commands;
    }

    @Override
    public String toString() {
        return "Script{" +
                "name='" + this.getName() + '\'' +
                '}';
    }

    public String getName() {
        return this.name;
    }
}
