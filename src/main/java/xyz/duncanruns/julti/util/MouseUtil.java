package xyz.duncanruns.julti.util;

import com.sun.jna.platform.win32.WinDef;
import xyz.duncanruns.julti.win32.User32;

import java.awt.*;

public final class MouseUtil {
    private MouseUtil() {}

    public static Point getMousePos() {
        WinDef.POINT p = new WinDef.POINT();
        User32.INSTANCE.GetCursorPos(p);
        return new Point(p.x, p.y);
    }
}
