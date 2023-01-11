package xyz.duncanruns.julti.util;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.JultiOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Consumer;

public final class LogReceiver {
    private static Consumer<String> logConsumer = null;

    private LogReceiver() {
    }

    public static void setLogConsumer(Consumer<String> consumer) {
        logConsumer = consumer;
    }

    public static void receive(Level level, String message) {
        if (level.equals(Level.DEBUG) && !JultiOptions.getInstance().showDebug) return;
        if (logConsumer != null)
            logConsumer.accept("[" + getTimeString() + "/" + level.name() + "] " + message);
    }

    private static String getTimeString() {
        return new SimpleDateFormat("HH:mm:ss").format(new Date());
    }

}
