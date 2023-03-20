package xyz.duncanruns.julti.util;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinUser;
import xyz.duncanruns.julti.win32.User32;
import xyz.duncanruns.julti.win32.Win32Con;

import java.awt.*;

public final class MouseUtil {
    private MouseUtil() {

    }

    public static boolean isMouseVisible() {
        User32.CURSORINFO pci = new User32.CURSORINFO();
        pci.cbSize = new WinDef.DWORD(Native.getNativeSize(pci.getClass(), pci));
        User32.INSTANCE.GetCursorInfo(pci);
        return (pci.flags.longValue() & 0x1) == 1;
    }

    public static void clickTopLeft(HWND hwnd) {
        WinDef.RECT rect = new WinDef.RECT();
        User32.INSTANCE.GetClientRect(hwnd, rect);
        User32.INSTANCE.PostMessageA(hwnd, new WinDef.UINT(0x0201), new WinDef.WPARAM(1), new WinDef.LPARAM(0L));
    }

    public static void keyDown(int vk) {
        changeKeyState(vk, true);
    }

    private static void changeKeyState(int vk, boolean isDown) {
        // https://stackoverflow.com/questions/28538234/sending-a-keyboard-input-with-java-jna-and-sendinput

        WinUser.INPUT input = new WinUser.INPUT();
        input.type = new WinDef.DWORD(WinUser.INPUT.INPUT_MOUSE);
        input.input.setType("mi");
        WinUser.MOUSEINPUT mi = input.input.mi;
        mi.dx = new WinDef.LONG(0);
        mi.dy = new WinDef.LONG(0);
        mi.mouseData = new WinDef.DWORD(vk == 5 || vk == 6 ? vk - 4 : 0);
        int buttonEventBit = 0;
        if (isDown) {
            switch (vk) {
                case Win32Con.VK_LBUTTON:
                    buttonEventBit = Win32Con.MOUSEEVENTF_LEFTDOWN;
                    break;
                case Win32Con.VK_RBUTTON:
                    buttonEventBit = Win32Con.MOUSEEVENTF_RIGHTDOWN;
                    break;
                case Win32Con.VK_MBUTTON:
                    buttonEventBit = Win32Con.MOUSEEVENTF_MIDDLEDOWN;
                    break;
                case Win32Con.VK_XBUTTON1:
                case Win32Con.VK_XBUTTON2:
                    buttonEventBit = Win32Con.MOUSEEVENTF_XDOWN;
                    break;
            }
        } else {
            switch (vk) {
                case Win32Con.VK_LBUTTON:
                    buttonEventBit = Win32Con.MOUSEEVENTF_LEFTUP;
                    break;
                case Win32Con.VK_RBUTTON:
                    buttonEventBit = Win32Con.MOUSEEVENTF_RIGHTUP;
                    break;
                case Win32Con.VK_MBUTTON:
                    buttonEventBit = Win32Con.MOUSEEVENTF_MIDDLEUP;
                    break;
                case Win32Con.VK_XBUTTON1:
                case Win32Con.VK_XBUTTON2:
                    buttonEventBit = Win32Con.MOUSEEVENTF_XUP;
                    break;
            }
        }
        mi.dwFlags = new WinDef.DWORD(buttonEventBit);
        mi.time = new WinDef.DWORD(0);
        mi.dwExtraInfo = new BaseTSD.ULONG_PTR();

        User32.INSTANCE.SendInput(new WinDef.DWORD(1), (WinUser.INPUT[]) input.toArray(1), input.size());
    }

    public static void keyUp(int vk) {
        changeKeyState(vk, false);
    }

    public static Point getMousePos() {
        WinDef.POINT p = new WinDef.POINT();
        User32.INSTANCE.GetCursorPos(p);
        return new Point(p.x, p.y);
    }
}
