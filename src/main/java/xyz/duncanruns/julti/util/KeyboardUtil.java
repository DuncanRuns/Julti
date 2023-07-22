package xyz.duncanruns.julti.util;

import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.Win32VK;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinUser;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.win32.User32;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static xyz.duncanruns.julti.util.SleepUtil.sleep;

public final class KeyboardUtil {

    public static final List<Integer> ALL_MODIFIERS = Arrays.asList(User32.VK_CONTROL, User32.VK_LCONTROL, User32.VK_RCONTROL, User32.VK_SHIFT, User32.VK_LSHIFT, User32.VK_RSHIFT, User32.VK_MENU, User32.VK_LMENU, User32.VK_RMENU);
    // Modifier keys which capture multiple keys
    public static final List<Integer> BROAD_MODIFIERS = Arrays.asList(User32.VK_CONTROL, User32.VK_SHIFT, User32.VK_MENU);
    // Modifier keys which reference only single keys
    public static final List<Integer> SINGLE_MODIFIERS = Arrays.asList(User32.VK_LCONTROL, User32.VK_RCONTROL, User32.VK_LSHIFT, User32.VK_RSHIFT, User32.VK_LMENU, User32.VK_RMENU);
    private static final String[] KEY_NAMES = getKeyNamesArray();

    private KeyboardUtil() {
    }

    public static void keyDown(Win32VK vk) {
        keyDown(vk.code);
    }

    public static void keyDown(int vk) {
        changeKeyState(vk, true);
    }

    public static void keyUp(Win32VK vk) {
        keyUp(vk.code);
    }

    public static void keyUp(int vk) {
        changeKeyState(vk, false);
    }

    private static void changeKeyState(int vk, boolean isDown) {
        // https://stackoverflow.com/questions/28538234/sending-a-keyboard-input-with-java-jna-and-sendinput

        WinUser.INPUT input = new WinUser.INPUT();
        input.type = new WinDef.DWORD(WinUser.INPUT.INPUT_KEYBOARD);
        input.input.setType("ki");
        input.input.ki.wScan = new WinDef.WORD(0);
        input.input.ki.time = new WinDef.DWORD(0);
        input.input.ki.dwExtraInfo = new BaseTSD.ULONG_PTR(0);

        input.input.ki.wVk = new WinDef.WORD(vk);
        input.input.ki.dwFlags = new WinDef.DWORD(isDown ? 0 : 2);

        User32.INSTANCE.SendInput(new WinDef.DWORD(1), (WinUser.INPUT[]) input.toArray(1), input.size());
    }

    public static void copyToClipboard(String string) {
        try {
            Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .setContents(
                            new StringSelection(string),
                            null
                    );
        } catch (Exception e) {
            Julti.log(Level.ERROR, "Failed to copy string to clipboard:\n" + ExceptionUtil.toDetailedString(e));
        }
    }

    // This code may cause a system exit. Removed for safety.
    //private static String findKeyName(int vKey) {
    //    WTypes.LPSTR lpstr = new WTypes.LPSTR("");
    //    User32.INSTANCE.GetKeyNameTextA(new WinDef.LONG(createLParamKeyDown(vKey).longValue()), lpstr, 128);
    //    String out = lpstr.getValue();
    //    if (out.isEmpty()) {
    //        return "Unknown Key";
    //    }
    //    return out;
    //}

    private static String[] getKeyNamesArray() {

        // For some reason actually calling findKeyName will randomly cause a system exit.
        //String[] names = new String[0xFE + 1];
        //for (int vKey = 0; vKey <= 0xFE; vKey++) {
        //    names[vKey] = findKeyName(vKey);
        //}
        //for (int vKey : new int[]{User32.VK_LCONTROL, User32.VK_LSHIFT, User32.VK_LMENU}) {
        //    names[vKey] = "Left " + names[vKey];
        //}
        //return names;

        // Original Strings generated from code above
        return new String[]{"Unknown Key", "Left Click", "Right Click", "Scroll Lock", "Middle Click", "X1", "X2", "Unknown Key", "Backspace", "Tab", "Unknown Key", "Unknown Key", "Num 5", "Enter", "Unknown Key", "Unknown Key", "Shift", "Ctrl", "Alt", "Unknown Key", "Caps Lock", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Esc", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Space", "Page Up", "Page Down", "End", "Home", "Left", "Up", "Right", "Down", "Unknown Key", "Unknown Key", "Unknown Key", "Sys Req", "Insert", "Delete", "Unknown Key", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Num 0", "Num 1", "Num 2", "Num 3", "Num 4", "Num 5", "Num 6", "Num 7", "Num 8", "Num 9", "Num *", "Num +", "Unknown Key", "Num -", "Num Del", "Num /", "F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8", "F9", "F10", "F11", "F12", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Num Lock", "Scroll Lock", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Left Shift", "Right Shift", "Left Ctrl", "Right Ctrl", "Left Alt", "Right Alt", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "M", "D", "C", "B", "P", "Q", "J", "G", "Unknown Key", "Unknown Key", "Unknown Key", "F", "Unknown Key", "Unknown Key", ";", "=", ",", "-", ".", "/", "`", "Unknown Key", "F15", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "[", "\\", "]", "'", "Unknown Key", "Unknown Key", "Unknown Key", "\\", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key", "Unknown Key"};
    }

    public static List<Integer> getPressedNonModKeys() {
        List<Integer> pressedKeys = getPressedKeys();

        if (!pressedKeys.isEmpty()) {
            pressedKeys.removeAll(ALL_MODIFIERS);
        }
        return pressedKeys;
    }

    public static List<Integer> getPressedKeys() {
        List<Integer> initPressedKeys = new ArrayList<>();
        for (int vKey = 1; vKey <= 0xFE; vKey++) {
            if (isPressed(vKey)) {
                initPressedKeys.add(vKey);
            }
        }
        if (!initPressedKeys.isEmpty()) {
            initPressedKeys.removeAll(BROAD_MODIFIERS);
        }

        List<Integer> pressedKeys = new ArrayList<>();

        for (Integer vKey : initPressedKeys) {
            if (ALL_MODIFIERS.contains(vKey)) {
                pressedKeys.add(vKey);
            }
        }
        for (Integer vKey : initPressedKeys) {
            if (!ALL_MODIFIERS.contains(vKey)) {
                pressedKeys.add(vKey);
            }
        }
        return pressedKeys;
    }

    public static boolean isPressed(int vKey) {
        return User32.INSTANCE.GetAsyncKeyState(vKey) < 0;
    }

    /**
     * Returns only a single non-modifier key along with any single modifiers pressed at the time.
     *
     * @return A list of integers representing virtual keys.
     */
    public static List<Integer> getPressedKeyWithMods() {
        return getPressedKeyWithMods(Collections.emptyList());
    }

    /**
     * Returns only a single non-modifier key along with any single modifiers pressed at the time.
     *
     * @return A list of integers representing virtual keys.
     */
    public static List<Integer> getPressedKeyWithMods(List<Integer> excludeKeys) {
        List<Integer> initPressedKeys = getPressedKeys();
        initPressedKeys.removeAll(excludeKeys);

        List<Integer> pressedKeys = new ArrayList<>();

        for (Integer vKey : initPressedKeys) {
            if (ALL_MODIFIERS.contains(vKey)) {
                pressedKeys.add(vKey);
            }
        }
        for (Integer vKey : initPressedKeys) {
            if (!ALL_MODIFIERS.contains(vKey)) {
                // As soon as a single non-modifier key is found, return the keyList
                pressedKeys.add(vKey);
                return pressedKeys;
            }
        }
        // No non-modifier keys pressed.
        return Collections.emptyList();

    }

    // LPARAM STUFF -> https://stackoverflow.com/questions/54638741/how-is-the-lparam-of-postmessage-constructe

    public static String getKeyName(int vKey) {
        return KEY_NAMES[vKey];
    }

    private static Pair<Integer, Boolean> virtualKeyToScanCode(int virtualKey) {
        int scanCode = User32.INSTANCE.MapVirtualKeyA(new WinDef.UINT(virtualKey), new WinDef.UINT(0)).intValue();
        boolean isExtended = false;
        switch (Win32VK.fromValue(virtualKey)) {
            case VK_RMENU:
            case VK_RCONTROL:
            case VK_LEFT:
            case VK_UP:
            case VK_RIGHT:
            case VK_DOWN:
            case VK_PRIOR:
            case VK_NEXT:
            case VK_END:
            case VK_HOME:
            case VK_INSERT:
            case VK_DELETE:
            case VK_DIVIDE:
            case VK_NUMLOCK:
                isExtended = true;
                break;
        }
        return new ImmutablePair<>(scanCode, isExtended);
    }

    private static WinDef.LPARAM createLParam(int virtualKey, int repeatCount, boolean transitionState, boolean previousKeyState, boolean contextCode) {
        Pair<Integer, Boolean> scanCode = virtualKeyToScanCode(virtualKey);
        return new WinDef.LPARAM(((transitionState ? 1 : 0) << 31) | ((previousKeyState ? 1 : 0) << 30) | ((contextCode ? 1 : 0) << 29) | ((scanCode.getRight() ? 1 : 0) << 24) | (scanCode.getLeft() << 16) | (repeatCount));
    }

    private static WinDef.LPARAM createLParamKeyDown(int virtualKey) {
        return createLParam(virtualKey, 1, false, false, false);
    }

    private static WinDef.LPARAM createLParamKeyUp(int virtualKey) {
        return createLParam(virtualKey, 1, true, true, false);
    }

    public static void sendKeyDownToHwnd(HWND hwnd, int virtualKey) {
        User32.INSTANCE.PostMessageA(hwnd, new WinDef.UINT(256), new WinDef.WPARAM(virtualKey), createLParamKeyDown(virtualKey));
    }

    public static void sendKeyDownToHwnd(HWND hwnd, Win32VK virtualKey) {
        sendKeyDownToHwnd(hwnd, virtualKey.code);
    }

    public static void sendKeyUpToHwnd(HWND hwnd, int virtualKey) {
        User32.INSTANCE.PostMessageA(hwnd, new WinDef.UINT(257), new WinDef.WPARAM(virtualKey), createLParamKeyUp(virtualKey));
    }

    public static void sendKeyUpToHwnd(HWND hwnd, Win32VK virtualKey) {
        sendKeyUpToHwnd(hwnd, virtualKey.code);
    }

    public static void sendKeyToHwnd(HWND hwnd, int virtualKey) {
        sendKeyDownToHwnd(hwnd, virtualKey);
        sendKeyUpToHwnd(hwnd, virtualKey);
    }

    public static void sendKeyToHwnd(HWND hwnd, Win32VK virtualKey) {
        sendKeyToHwnd(hwnd, virtualKey.code);
    }

    public static void sendCharToHwnd(HWND hwnd, int character) {
        User32.INSTANCE.PostMessageA(hwnd, new WinDef.UINT(WinUser.WM_CHAR), new WinDef.WPARAM(character), new WinDef.LPARAM(0));
    }

    public static void sendKeyToHwnd(HWND hwnd, int virtualKey, long pressTime) {
        sendKeyDownToHwnd(hwnd, virtualKey);
        if (pressTime > 0) {
            sleep(pressTime);
        }
        sendKeyUpToHwnd(hwnd, virtualKey);
    }

    public static void sendKeyToHwnd(HWND hwnd, Win32VK virtualKey, long pressTime) {
        sendKeyToHwnd(hwnd, virtualKey.code, pressTime);
    }

    public static void releaseAllModifiersForHwnd(HWND hwnd) {
        for (int key : ALL_MODIFIERS) {
            sendKeyUpToHwnd(hwnd, key);
        }
    }

    public static void releaseAllModifiers() {
        for (int key : ALL_MODIFIERS) {
            keyUp(key);
        }
    }
}