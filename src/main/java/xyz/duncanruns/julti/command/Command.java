package xyz.duncanruns.julti.command;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.util.LogReceiver;

public abstract class Command {
    private static final Logger LOGGER = LogManager.getLogger("Command");

    public static void log(Level level, String message) {
        LOGGER.log(level, message);
        LogReceiver.receive(level, message);
    }

    protected static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public abstract String helpDescription();

    public abstract int getMinArgs();

    public abstract int getMaxArgs();

    public abstract String getName();

    public abstract void run(String[] args, Julti julti);
}
