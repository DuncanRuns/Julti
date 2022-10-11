package xyz.duncanruns.julti.util;

import com.github.tuupertunut.powershelllibjava.PowerShell;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.LONG;
import com.sun.jna.ptr.IntByReference;
import org.apache.commons.text.StringEscapeUtils;
import xyz.duncanruns.julti.win32.User32;
import xyz.duncanruns.julti.win32.Win32Con;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class HwndUtil {
    private static final Pattern MC_PATTERN = Pattern.compile("(^Minecraft\\* - Instance \\d\\d?$)|(^Minecraft\\*? 1\\.[1-9]\\d*(\\.[1-9]\\d*)?( - .+)?$)");
    private static final Robot ROBOT;
    private static final PowerShell POWER_SHELL;

    static {
        try {
            POWER_SHELL = PowerShell.open();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            ROBOT = new Robot();
        } catch (AWTException e) {
            throw new RuntimeException(e);
        }
    }

    private HwndUtil() {
    }

    public static Pointer getCurrentHwnd() {
        return User32.INSTANCE.GetForegroundWindow();
    }

    public static List<Pointer> getAllMinecraftHwnds() {
        return getAllHwnds(MC_PATTERN);
    }

    private static List<Pointer> getAllHwnds(Pattern pattern) {
        List<Pointer> list = new ArrayList<>();
        User32.INSTANCE.EnumWindows((hwnd, arg) -> {
            String title = getHwndTitle(hwnd);
            if (pattern.matcher(title).matches()) {
                list.add(hwnd);
            }
            return true;
        }, null);
        return list;
    }

    public static String getHwndTitle(Pointer hwnd) {
        byte[] x = new byte[128];
        User32.INSTANCE.GetWindowTextA(hwnd, x, 128);
        StringBuilder out = new StringBuilder();
        for (byte a : x) {
            if (a == 0)
                break;
            out.append((char) a);
        }
        return out.toString();
    }

    public static boolean hwndExists(Pointer hwnd) {
        return User32.INSTANCE.IsWindow(hwnd);
    }

    // Sets a window to be borderless but does not move it.
    public static void setHwndBorderless(Pointer hwnd) {
        long style = getHwndStyle(hwnd);
        style &= ~(Win32Con.WS_BORDER
                | Win32Con.WS_DLGFRAME
                | Win32Con.WS_THICKFRAME
                | Win32Con.WS_MINIMIZEBOX
                | Win32Con.WS_MAXIMIZEBOX
                | Win32Con.WS_SYSMENU);
        setHwndStyle(hwnd, style);
    }

    public static long getHwndStyle(Pointer hwnd) {
        return User32.INSTANCE.GetWindowLongA(hwnd, Win32Con.GWL_STYLE).longValue();
    }

    public static void setHwndStyle(Pointer hwnd, long style) {
        User32.INSTANCE.SetWindowLongA(hwnd, Win32Con.GWL_STYLE, new LONG(style));
    }

    public static void sendCloseMessage(Pointer hwnd) {
        User32.INSTANCE.SendNotifyMessageA(new WinDef.HWND(hwnd), new WinDef.UINT(Win32Con.WM_SYSCOMMAND), new WinDef.WPARAM(Win32Con.SC_CLOSE), new WinDef.LPARAM(0));
    }

    public static boolean IsHwndBorderless(Pointer hwnd) {
        long oldStyle = getHwndStyle(hwnd);
        long newStyle = oldStyle;
        newStyle &= ~(Win32Con.WS_BORDER
                | Win32Con.WS_DLGFRAME
                | Win32Con.WS_THICKFRAME
                | Win32Con.WS_MINIMIZEBOX
                | Win32Con.WS_MAXIMIZEBOX
                | Win32Con.WS_SYSMENU);
        return newStyle == oldStyle;
    }

    public static void undoHwndBorderless(Pointer hwnd) {
        setHwndStyle(hwnd, 382664704);
    }

    public static void showHwnd(Pointer hwnd) {
        User32.INSTANCE.ShowWindow(hwnd, Win32Con.SW_SHOWMAXIMIZED);
    }

    public static void setHwndTitle(Pointer hwnd, String title) {
        User32.INSTANCE.SetWindowTextA(hwnd, title);
    }

    public static void activateHwnd(Pointer hwnd) {
        // Windows requires alt to be pressed to switch to a window.... maybe?
        // It needed it in the python implementation but apparently not here. Will keep it anyway.
        ROBOT.keyPress(KeyEvent.VK_ALT);
        ROBOT.keyRelease(KeyEvent.VK_ALT);
        User32.INSTANCE.SetForegroundWindow(hwnd);
    }

    public static void moveHwnd(Pointer hwnd, int x, int y, int w, int h) {
        User32.INSTANCE.MoveWindow(hwnd, x, y, w, h, true);
    }

    public static Path getInstancePathFromPid(int pid) {
        // Thanks specnr
        try {
            String response = getCommandLine(pid);
            if (response == null) {
                return null;
            }
            if (response.contains("--gameDir")) {
                int ind = response.indexOf("--gameDir") + 10;
                return Paths.get(takeArg(response, ind));
            } else if (response.contains("Djava.library.path")) {
                int ind;
                if (response.contains("\"-Djava.library.path")) {
                    ind = response.indexOf("\"-Djava.library.path");
                } else {
                    ind = response.indexOf("-Djava.library.path");
                }
                String nativesPathStr = takeArg(response, ind).substring(20);
                return Paths.get(nativesPathStr).resolveSibling(".minecraft");
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String getCommandLine(int pid) throws IOException {
        try {
            return POWER_SHELL.executeCommands("$proc = Get-CimInstance Win32_Process -Filter \"ProcessId = PIDHERE\";$proc.CommandLine".replace("PIDHERE", String.valueOf(pid)));
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String takeArg(String string, int ind) throws Exception {
        String sub = string.substring(ind);
        if (sub.isEmpty()) {
            return "";
        }
        while (sub.charAt(0) == ' ') {
            sub = sub.substring(1);
            if (sub.isEmpty()) {
                return "";
            }
        }
        if (sub.charAt(0) == '"') {
            int scanInd = 1;
            int bsc = 0;
            while (scanInd < sub.length()) {
                if (sub.charAt(scanInd) == '\\') {
                    bsc += 1;
                } else if (sub.charAt(scanInd) == '"') {
                    if (bsc % 2 == 0) {
                        break;
                    } else {
                        bsc = 0;
                    }
                } else {
                    bsc = 0;
                }
                scanInd++;
            }
            if (scanInd == sub.length()) {
                throw new Exception(); // QUOTATION WAS NOT ENDED
            }
            return StringEscapeUtils.escapeJava(sub.substring(1, scanInd));
        } else {
            int scanInd = 1;
            while (scanInd < sub.length()) {
                if (sub.charAt(scanInd) == ' ') {
                    break;
                }
                scanInd++;
            }
            return sub.substring(0, scanInd);
        }
    }

    public static int getPidFromHwnd(Pointer hwnd) {
        final IntByReference pidPointer = new IntByReference();
        User32.INSTANCE.GetWindowThreadProcessId(hwnd, pidPointer);
        return pidPointer.getValue();
    }

    public static boolean isHwndMinimized(Pointer hwnd) {
        return User32.INSTANCE.IsIconic(hwnd);
    }
}
