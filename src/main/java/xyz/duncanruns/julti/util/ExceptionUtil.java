package xyz.duncanruns.julti.util;

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
}
