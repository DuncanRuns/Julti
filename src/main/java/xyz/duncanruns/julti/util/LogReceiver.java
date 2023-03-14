package xyz.duncanruns.julti.util;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.JultiOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class LogReceiver {
    private static final ExecutorService RECEIVER_QUEUE = Executors.newSingleThreadExecutor();
    private static Consumer<String> LOG_CONSUMER;

    public static void setLogConsumer(Consumer<String> consumer) {
        LOG_CONSUMER = consumer;
    }

    public static void receive(Level level, String message) {
        RECEIVER_QUEUE.execute(() -> {
            if (level.equals(Level.DEBUG) && !JultiOptions.getInstance().showDebug) return;
            if (LOG_CONSUMER != null) {
                LOG_CONSUMER.accept("[" + getTimeString() + "/" + level.name() + "] " + message);
            }
        });
    }

    private static String getTimeString() {
        return new SimpleDateFormat("HH:mm:ss").format(new Date());
    }
}
