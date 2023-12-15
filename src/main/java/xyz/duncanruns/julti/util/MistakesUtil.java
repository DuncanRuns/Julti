package xyz.duncanruns.julti.util;

import com.github.tuupertunut.powershelllibjava.PowerShellExecutionException;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.gui.JultiGUI;
import xyz.duncanruns.julti.win32.User32;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * A utility class to detect external mistakes (such as some other software being open) that the user could be making.
 */
public final class MistakesUtil {
    private MistakesUtil() {
    }

    public static void notifyMistake(String title, String message) {
        JOptionPane.showMessageDialog(JultiGUI.getJultiGUI(), message, title, JOptionPane.WARNING_MESSAGE);
    }

    public static void tryCheckWallMacroOpen() {
        try {
            checkWallMacroOpen();
        } catch (IOException e) {
            Julti.log(Level.ERROR, "Failed to check if a wall macro is opened: " + ExceptionUtil.toDetailedString(e));
        }
    }

    public static void tryCheckOtherJultiOpen() {
        try {
            checkOtherJultiOpen();
        } catch (PowerShellExecutionException | IOException e) {
            Julti.log(Level.ERROR, "Failed to check if another instance of Julti is opened: " + ExceptionUtil.toDetailedString(e));
        }
    }

    /**
     * <a href="https://stackoverflow.com/questions/35129457/how-to-check-if-a-process-is-running-on-windows">Source</a>
     *
     * @author itsdxrk
     * @author jojoe77777
     * @author DuncanRuns
     */
    private static void checkWallMacroOpen() throws IOException {
        String tasksCmd = System.getenv("windir") + "/system32/wbem/wmic.exe path Win32_Process where " +
                "\"CommandLine Like '%AutoHotkey.exe% %TheWall.ahk%'\" get ProcessId /format:list";
        Process p = Runtime.getRuntime().exec(tasksCmd);
        BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));

        String pid;
        boolean firstFound = false;
        while ((pid = input.readLine()) != null) {
            pid = pid.trim();
            if (pid.isEmpty()) { // pid cannot be null as that is checked in the while loop condition
                continue;
            }
            if (!firstFound) {
                firstFound = true;
                continue;
            }
            String message = "MultiResetWall is open. To prevent issues, please close the ahk script.";
            Julti.log(Level.WARN, message);
            notifyMistake("Julti: Warning", message);
            input.close(); // not sure if this is necessary
            return;
        }
    }

    /**
     * @author itsdxrk
     * @author jojoe77777
     * @author DuncanRuns
     * @author draconix6
     */
    private static void checkOtherJultiOpen() throws PowerShellExecutionException, IOException {
        User32.INSTANCE.EnumWindows((hWnd, arg) -> {
            int pid = PidUtil.getPidFromHwnd(hWnd);
            // keep checking windows if current window is self
            if (pid == PidUtil.getPidForSelf()) {
                return true;
            }

            String title = WindowTitleUtil.getHwndTitle(hWnd);
            if (title.contains("Julti")) {
                // window title contains julti - can probably stop there, however we check the running process just in case
                try {
                    String out = PowerShellUtil.execute("$proc = Get-CimInstance Win32_Process -Filter \"ProcessId = PIDHERE\";$proc.CommandLine".replace("PIDHERE", String.valueOf(pid)));
                    if (out.contains("javaw.exe")) {
                        String message = "Another instance of Julti is open. To prevent issues, please close one of them.";
                        Julti.log(Level.WARN, message);
                        notifyMistake("Julti: Warning", message);
                        return false;
                    }
                } catch (PowerShellExecutionException | IOException e) {
                    Julti.log(Level.WARN, "Another instance of Julti may be open. To prevent issues, ensure only one Julti is running.");
                    return false;
                }
            }
            return true;
        }, null);
    }

    public static void checkStartupMistakes() {
        tryCheckWallMacroOpen();
        tryCheckOtherJultiOpen();
    }
}
