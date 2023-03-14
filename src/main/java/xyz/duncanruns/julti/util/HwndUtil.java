package xyz.duncanruns.julti.util;

import com.github.tuupertunut.powershelllibjava.PowerShell;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Psapi;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.LONG;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.win32.User32;
import xyz.duncanruns.julti.win32.Win32Con;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

public final class HwndUtil {
    private static final Pattern MC_PATTERN = Pattern.compile("(^Minecraft\\* - Instance \\d\\d?$)|(^Minecraft\\*? 1\\.[1-9]\\d*(\\.[1-9]\\d*)?( - .+)?$)");
    private static final Robot ROBOT;
    private static final PowerShell POWER_SHELL;
    private static final byte[] EXECUTABLE_PATH_BUFFER = new byte[1024];
    private static WinDef.HWND OBS_HWND = null;

    static {
        try { POWER_SHELL = PowerShell.open(); }
        catch (IOException e) { throw new RuntimeException(e); }
        try { ROBOT = new Robot(); }
        catch (AWTException e) { throw new RuntimeException(e); }
    }

    public static List<WinDef.HWND> getAllMinecraftHwnds() {
        return getAllHwnds();
    }

    private static List<WinDef.HWND> getAllHwnds() {
        List<WinDef.HWND> list = new ArrayList<>();
        User32.INSTANCE.EnumWindows((hwnd, arg) -> {
            String title = getHwndTitle(hwnd);
            if (HwndUtil.MC_PATTERN.matcher(title).matches()) {
                list.add(hwnd);
            }
            return true;
        }, null);
        return list;
    }

    public static String getHwndTitle(WinDef.HWND hwnd) {
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

    // Sets a window to be borderless but does not move it.
    public static void setHwndBorderless(WinDef.HWND hwnd) {
        long style = getHwndStyle(hwnd);
        style &= ~(Win32Con.WS_BORDER
                | Win32Con.WS_DLGFRAME
                | Win32Con.WS_THICKFRAME
                | Win32Con.WS_MINIMIZEBOX
                | Win32Con.WS_MAXIMIZEBOX
                | Win32Con.WS_SYSMENU);
        setHwndStyle(hwnd, style);
    }

    public static long getHwndStyle(WinDef.HWND hwnd) {
        return User32.INSTANCE.GetWindowLongA(hwnd, Win32Con.GWL_STYLE).longValue();
    }

    public static void setHwndStyle(WinDef.HWND hwnd, long style) {
        User32.INSTANCE.SetWindowLongA(hwnd, Win32Con.GWL_STYLE, new LONG(style));
    }

    public static void sendCloseMessage(WinDef.HWND hwnd) {
        User32.INSTANCE.SendNotifyMessageA(hwnd, new WinDef.UINT(Win32Con.WM_SYSCOMMAND), new WinDef.WPARAM(Win32Con.SC_CLOSE), new WinDef.LPARAM(0));
    }

    public static boolean isHwndBorderless(WinDef.HWND hwnd) {
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

    public static void undoHwndBorderless(WinDef.HWND hwnd) {
        setHwndStyle(hwnd, 382664704);
    }

    public static void maximizeHwnd(WinDef.HWND hwnd) {
        //User32.INSTANCE.ShowWindow(hwnd, Win32Con.SW_SHOWMAXIMIZED);

        // Fast maximize yoinked from ahk macros
        User32.INSTANCE.SendMessageA(hwnd, new WinDef.UINT(0x0112), new WinDef.WPARAM(0xF030), new WinDef.LPARAM(0));
    }

    public static void showHwnd(WinDef.HWND hwnd) {
        User32.INSTANCE.ShowWindow(hwnd, Win32Con.SW_SHOW);
    }

    public static void restoreHwnd(WinDef.HWND hwnd) {
        User32.INSTANCE.ShowWindow(hwnd, Win32Con.SW_SHOWNOACTIVATE);
    }

    public static void setHwndTitle(WinDef.HWND hwnd, String title) {
        User32.INSTANCE.SetWindowTextA(hwnd, title);
    }

    public static void activateHwnd(WinDef.HWND hwnd) {
        // Windows requires alt to be pressed to switch to a window.... maybe?
        // It needed it in the python implementation but apparently not here. Will keep it anyway.
        if (hwnd == null) return;
        ROBOT.keyPress(KeyEvent.VK_ALT);
        ROBOT.keyRelease(KeyEvent.VK_ALT);
        User32.INSTANCE.SetForegroundWindow(hwnd);
        User32.INSTANCE.BringWindowToTop(hwnd);
    }

    public static void moveHwnd(WinDef.HWND hwnd, int x, int y, int w, int h) {
        User32.INSTANCE.MoveWindow(hwnd, x, y, w, h, true);
    }

    /**
     * @author Spencr & Duncan
     */
    public static Path getInstancePathFromPid(int pid) {
        try {
            String response = getCommandLine(pid);
            if (response == null) { return null; }
            if (response.contains("--gameDir")) {
                int ind = response.indexOf("--gameDir") + 10;
                return Paths.get(takeArg(response, ind));
            } else if (response.contains("Djava.library.path")) {
                int ind;
                if (response.contains("\"-Djava.library.path")) { ind = response.indexOf("\"-Djava.library.path"); }
                else { ind = response.indexOf("-Djava.library.path"); }
                String nativesPathStr = takeArg(response, ind).substring(20);
                return Paths.get(nativesPathStr).resolveSibling(".minecraft");
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String getCommandLine(int pid) {
        try {
            return POWER_SHELL.executeCommands("$proc = Get-CimInstance Win32_Process -Filter \"ProcessId = PIDHERE\";$proc.CommandLine".replace("PIDHERE", String.valueOf(pid)));
        } catch (Exception ignored) {}
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
        int scanInd = 1;
        if (sub.charAt(0) == '"') {
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
            while (scanInd < sub.length()) {
                if (sub.charAt(scanInd) == ' ') {
                    break;
                }
                scanInd++;
            }
            return sub.substring(0, scanInd);
        }
    }

    public static boolean isHwndMaximized(WinDef.HWND hwnd) {
        return User32.INSTANCE.IsZoomed(hwnd);
    }

    public static Rectangle getHwndRectangle(WinDef.HWND hwnd) {
        WinDef.RECT rect = new WinDef.RECT();
        User32.INSTANCE.GetWindowRect(hwnd, rect);
        return new Rectangle(rect.left, rect.top, rect.right - rect.left, rect.bottom - rect.top);
    }

    /**
     * Waits for a window with an exact name. Once the window appears, the window handle is returned.
     * <p>
     * DO NOT use this method unless you are certain that the window will be present.
     *
     * @param exactName the exact name of the window
     * @return the window handle
     */
    @SuppressWarnings("UnusedDeclaration")
    public static WinDef.HWND waitForWindow(String exactName) {
        AtomicReference<WinDef.HWND> out = new AtomicReference<>(null);
        // made it so that it's no longer busy-waiting
        // possible behaviour change, but it's unused anyway.
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            while (out.get() == null) {
                User32.INSTANCE.EnumWindows((hWnd, arg) -> {
                    if (HwndUtil.getHwndTitle(hWnd).equals(exactName)) {
                        out.set(hWnd);
                        return false;
                    }
                    return true;
                }, null);
            }
        }, 0, 1000 / 20, TimeUnit.MILLISECONDS);
        return out.get();
    }

    public static boolean multiMCExists(String exeName) {
        AtomicBoolean out = new AtomicBoolean(false);
        User32.INSTANCE.EnumWindows((hWnd, arg) -> {
            if (getProcessExecutable(getPidFromHwnd(hWnd)).endsWith(exeName)) {
                out.set(true);
                return false;
            }
            return true;
        }, null);
        return out.get();
    }

    public static String getProcessExecutable(int processId) {
        //Help from https://stackoverflow.com/questions/15693210/getmodulefilename-for-window-in-focus-jna-windows-os
        WinNT.HANDLE process = Kernel32.INSTANCE.OpenProcess(0x0400 | 0x0010, false, processId);

        StringBuilder out = new StringBuilder();
        synchronized (EXECUTABLE_PATH_BUFFER) {
            Psapi.INSTANCE.GetModuleFileNameExA(process, null, EXECUTABLE_PATH_BUFFER, 1024);
            for (byte a : EXECUTABLE_PATH_BUFFER) {
                if (a == 0)
                    break;
                out.append((char) a);
            }
        }
        return out.toString();
    }

    public static int getPidFromHwnd(WinDef.HWND hwnd) {
        final IntByReference pidPointer = new IntByReference();
        User32.INSTANCE.GetWindowThreadProcessId(hwnd, pidPointer);
        return pidPointer.getValue();
    }

    public static boolean obsWallCheckActiveQuick() {
        if (OBS_HWND == null) {
            OBS_HWND = HwndUtil.getOBSWallHwnd(JultiOptions.getInstance().obsWindowNameFormat);
        }
        return Objects.equals(getCurrentHwnd(), OBS_HWND);
    }

    public static WinDef.HWND getOBSWallHwnd(String projectorFormat) {
        if (OBS_HWND != null) {
            if (hwndExists(OBS_HWND) && isOBSWallHwnd(projectorFormat, OBS_HWND)) {
                return OBS_HWND;
            }
        }

        OBS_HWND = null;
        User32.INSTANCE.EnumWindows((hWnd, arg) -> {
            if (isOBSWallHwnd(projectorFormat, hWnd)) {
                OBS_HWND = hWnd;
                return false;
            }
            return true;
        }, null);
        return OBS_HWND;
    }

    public static WinDef.HWND getCurrentHwnd() {
        return User32.INSTANCE.GetForegroundWindow();
    }

    public static boolean hwndExists(WinDef.HWND hwnd) {
        return User32.INSTANCE.IsWindow(hwnd);
    }

    public static boolean isOBSWallHwnd(String projectorFormat, WinDef.HWND hwnd) {
        if (hwnd == null) { return false; }
        Julti.log(Level.DEBUG, "HwndUtil.isOBSWallHwnd -> hwnd is not null");
        String regex = '^' + projectorFormat.toLowerCase().replaceAll("([^a-zA-Z0-9 ])", "\\\\$1").replace("\\*", ".*") + '$';
        Julti.log(Level.DEBUG, "HwndUtil.isOBSWallHwnd -> regex pattern is " + regex);
        final Pattern pattern = Pattern.compile(regex);
        String title = getHwndTitle(hwnd).toLowerCase();
        Julti.log(Level.DEBUG, "HwndUtil.isOBSWallHwnd -> title to match is " + title);
        return pattern.matcher(title).matches();
    }
}
