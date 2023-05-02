package xyz.duncanruns.julti.messages;

import java.awt.*;

public class HotkeyPressQMessage extends QMessage {
    private final String hotkeyCode;
    private final Point mousePosition;

    public HotkeyPressQMessage(String hotkeyCode, Point mousePosition) {
        this.hotkeyCode = hotkeyCode;
        this.mousePosition = mousePosition;
    }

    public String getHotkeyCode() {
        return this.hotkeyCode;
    }

    public Point getMousePosition() {
        return this.mousePosition;
    }
}
