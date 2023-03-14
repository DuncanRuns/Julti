package xyz.duncanruns.julti.command;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.util.requester.CancelRequester;
import xyz.duncanruns.julti.util.LogReceiver;
import xyz.duncanruns.julti.util.requester.CancelRequesters;

import java.util.List;

public abstract class Command {
    private static final Logger LOGGER = LogManager.getLogger("Command");

    public static void log(Level level, String message) {
        LOGGER.log(level, message);
        LogReceiver.receive(level, message);
    }

    protected static void sleep(long millis) {
        try { Thread.sleep(millis); }
        catch (InterruptedException e) { throw new RuntimeException(e); }
    }

    public abstract String helpDescription();

    public abstract int getMinArgs();

    public abstract int getMaxArgs();

    public abstract String getName();

    public final void run(String[] args, Julti julti) {
        run(args, julti, CancelRequesters.NEVER_CANCEL_REQUESTER);
    }

    public abstract void run(String[] args, Julti julti, CancelRequester cancelRequester);

    public List<MinecraftInstance> getInstancesFromArg(String arg, Julti julti) {
        List<MinecraftInstance> instances;
        if (arg.equals("all")) { instances = julti.getInstanceManager().getInstances(); }
        else { instances = CommandManager.getInstances(arg, julti); }

        if (instances.isEmpty()) {
            log(Level.ERROR, "No instances found");
        }

        return instances;
    }
}
