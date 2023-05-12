package xyz.duncanruns.julti.util;

import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.LONG;
import com.sun.jna.platform.win32.WinDef.RECT;
import xyz.duncanruns.julti.win32.User32;

import java.awt.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class WindowStateUtil {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final int BORDERLESS_STYLE = ~(User32.WS_BORDER
            | User32.WS_DLGFRAME
            | User32.WS_THICKFRAME
            | User32.WS_MINIMIZEBOX
            | User32.WS_MAXIMIZEBOX
            | User32.WS_SYSMENU);

    private WindowStateUtil() {
    }

    public static boolean isHwndBorderless(HWND hwnd) {
        int style = getHwndStyle(hwnd);
        int styleWithBorderless = style & BORDERLESS_STYLE;
        return styleWithBorderless == style;
    }

    public static void setHwndBorderless(HWND hwnd) {
        int style = getHwndStyle(hwnd);
        style &= BORDERLESS_STYLE;
        setHwndStyle(hwnd, style);
    }

    public static int getHwndStyle(HWND hwnd) {
        return User32.INSTANCE.GetWindowLongA(hwnd, User32.GWL_STYLE).intValue();
    }

    public static void setHwndStyle(HWND hwnd, int style) {
        User32.INSTANCE.SetWindowLongA(hwnd, User32.GWL_STYLE, new LONG(style));
    }

    public static void undoHwndBorderless(HWND hwnd) {
        setHwndStyle(hwnd, 382664704);
    }

    public static void maximizeHwnd(HWND hwnd) {
        User32.INSTANCE.ShowWindow(hwnd, User32.SW_SHOWMAXIMIZED);
    }

    public static boolean isHwndMaximized(HWND hwnd) {
        return User32.INSTANCE.IsZoomed(hwnd);
    }

    public static void restoreHwnd(HWND hwnd) {
        User32.INSTANCE.ShowWindow(hwnd, User32.SW_SHOWNOACTIVATE);
    }

    public static Rectangle getHwndRectangle(HWND hwnd) {
        RECT rect = new RECT();
        User32.INSTANCE.GetWindowRect(hwnd, rect);
        return new Rectangle(rect.left, rect.top, rect.right - rect.left, rect.bottom - rect.top);
    }

    public static void queueSetHwndRectangle(HWND hwnd, Rectangle rectangle) {
        EXECUTOR.execute(() -> setHwndRectangle(hwnd, rectangle));
    }

    public static void setHwndRectangle(HWND hwnd, Rectangle rectangle) {
        User32.INSTANCE.MoveWindow(hwnd, rectangle.x, rectangle.y, rectangle.width, rectangle.height, true);
    }
}
