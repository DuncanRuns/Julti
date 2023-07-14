package xyz.duncanruns.julti.management;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.JultiOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class LogReceiver {
    private static final ExecutorService receiverQueue = Executors.newSingleThreadExecutor();
    private static Consumer<String> logConsumer = null;

    private LogReceiver() {
    }

    public static void setLogConsumer(Consumer<String> consumer) {
        logConsumer = consumer;
    }

    public static void receive(Level level, String message) {
        receiverQueue.execute(() -> {
            if (level.equals(Level.DEBUG) && !JultiOptions.getJultiOptions().showDebug) {
                return;
            }
            if (logConsumer != null) {
                logConsumer.accept("[" + getTimeString() + "/" + level.name() + "] " + message);
            }
        });
    }

    private static String getTimeString() {
        return new SimpleDateFormat("HH:mm:ss").format(new Date());
    }

}
