package xyz.duncanruns.julti.management;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.HWND;
import xyz.duncanruns.julti.util.MonitorUtil;
import xyz.duncanruns.julti.util.WindowTitleUtil;

import java.awt.*;
import java.util.Objects;

public final class ActiveWindowTracker {
    private static HWND activeHwnd;
    private static String activeTitle;

    private static HWND lastWallHwnd;

    private ActiveWindowTracker() {
    }

    public static HWND getActiveHwnd() {
        return activeHwnd;
    }

    public static boolean isWindowActive(HWND hwnd) {
        return Objects.equals(hwnd, getActiveHwnd());
    }

    public static boolean isWallActive() {
        if (activeHwnd == null) {
            return false;
        }
        return WindowTitleUtil.isOBSTitle(activeTitle);
    }

    public static void update() {
        activeHwnd = User32.INSTANCE.GetForegroundWindow();
        activeTitle = WindowTitleUtil.getHwndTitle(activeHwnd);
        if (isWallActive()) {
            lastWallHwnd = activeHwnd;
        }
    }

    public static Rectangle getActiveWindowBounds() {
        if (activeHwnd == null) {
            return MonitorUtil.getPrimaryMonitor().bounds;
        }
        WinDef.RECT rect = new WinDef.RECT();
        User32.INSTANCE.GetWindowRect(activeHwnd, rect);
        return new Rectangle(rect.left, rect.top, rect.right - rect.left, rect.bottom - rect.top);
    }

    public static HWND getLastWallHwnd() {
        return lastWallHwnd;
    }
}
