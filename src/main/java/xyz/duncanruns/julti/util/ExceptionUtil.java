package xyz.duncanruns.julti.util;

import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.io.PrintWriter;
import java.io.StringWriter;

public final class ExceptionUtil {
    private ExceptionUtil() {
    }

    public static String toDetailedString(Throwable t) {
        StringWriter out = new StringWriter();
        out.write(t.toString() + "\n");
        t.printStackTrace(new PrintWriter(out));
        return out.toString();
    }

    public static void showExceptionAndExit(Throwable t, String message) {
        String detailedException = toDetailedString(t);
        int ans = JOptionPane.showOptionDialog(null, message, "Julti: Crash", JOptionPane.OK_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE, null, new Object[]{"Copy Error", "Cancel"}, "Copy Error");
        if (ans == 0) {
            KeyboardUtil.copyToClipboard("Error during startup or main loop: " + detailedException);
        }
        LogManager.getLogger("Julti-Crash").error(detailedException); // We don't want to use Julti.log because it has a couple more steps.
        System.exit(1);
    }
}
