package xyz.duncanruns.julti.script;

import java.util.regex.Pattern;

public class Script {
    private static final Pattern SAVABLE_STRING_PATTERN = Pattern.compile("^[^;]+;\\d;.+$");
    private String name;
    private byte hotkeyContext;
    private String commands; // ; separated commands

    public Script(String name, byte hotkeyContext, String commands) {
        this.name = name.strip();
        this.hotkeyContext = hotkeyContext;
        this.commands = commands.strip();
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
        return hotkeyContext;
    }

    public void setHotkeyContext(byte hotkeyContext) {
        this.hotkeyContext = hotkeyContext;
    }

    public String getCommands() {
        return commands;
    }

    public void setCommands(String commands) {
        this.commands = commands.strip();
    }

    public String toSavableString() {
        return name + ";" + hotkeyContext + ";" + commands;
    }

    @Override
    public String toString() {
        return "Script{" +
                "name='" + getName() + '\'' +
                '}';
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name.strip();
    }
}
