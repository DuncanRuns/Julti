package xyz.duncanruns.julti.util;

import com.github.tuupertunut.powershelllibjava.PowerShell;
import com.github.tuupertunut.powershelllibjava.PowerShellExecutionException;

import java.io.IOException;

public class PowerShellUtil {
    private static final PowerShell POWER_SHELL;

    static {
        try {
            POWER_SHELL = PowerShell.open();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String execute(String command) throws PowerShellExecutionException, IOException {
        return POWER_SHELL.executeCommands(command);
    }
}
