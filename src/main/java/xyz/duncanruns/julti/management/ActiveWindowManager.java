package xyz.duncanruns.julti.management;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Win32VK;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.ptr.IntByReference;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.util.KeyboardUtil;
import xyz.duncanruns.julti.util.MonitorUtil;
import xyz.duncanruns.julti.util.WindowTitleUtil;
import xyz.duncanruns.julti.win32.User32;

import java.awt.*;
import java.util.Objects;

public final class ActiveWindowManager {
    private static HWND activeHwnd;
    private static String activeTitle;

    private static HWND lastWallHwnd;

    private ActiveWindowManager() {
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

    public static void activateHwnd(HWND hwnd) {
        if (JultiOptions.getJultiOptions().useAltSwitching) {
            KeyboardUtil.keyDown(Win32VK.VK_LMENU);
            KeyboardUtil.keyUp(Win32VK.VK_LMENU);
            User32.INSTANCE.SetForegroundWindow(hwnd);
            User32.INSTANCE.BringWindowToTop(hwnd);
        } else {
            // Using Erlend Robaye's answer from https://stackoverflow.com/questions/20444735/issue-with-setforegroundwindow-in-net
            // I believe specnr also uses this
            int currentlyFocusedWindowProcessId = User32.INSTANCE.GetWindowThreadProcessId(User32.INSTANCE.GetForegroundWindow(), new IntByReference(0));
            int appThread = Kernel32.INSTANCE.GetCurrentThreadId();

            User32.INSTANCE.AttachThreadInput(new WinDef.DWORD(currentlyFocusedWindowProcessId), new WinDef.DWORD(appThread), true);
            // If the order of SetForegroundWindow and BringWindowToTop are switched, keyboard focus gets fucked
            User32.INSTANCE.SetForegroundWindow(hwnd);
            User32.INSTANCE.BringWindowToTop(hwnd);
            User32.INSTANCE.AttachThreadInput(new WinDef.DWORD(currentlyFocusedWindowProcessId), new WinDef.DWORD(appThread), false);
        }
    }
}
