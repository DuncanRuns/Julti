package xyz.duncanruns.julti.messages;

public class HotkeyPressQMessage extends QMessage {
    private final String hotkeyCode;

    public HotkeyPressQMessage(String hotkeyCode) {
        this.hotkeyCode = hotkeyCode;
    }

    public String getHotkeyCode() {
        return this.hotkeyCode;
    }
}
