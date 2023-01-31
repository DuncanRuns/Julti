package xyz.duncanruns.julti.command;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;

public class LogCommand extends Command {
    @Override
    public String helpDescription() {
        return "log [message] - Logs a message";
    }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override
    public int getMaxArgs() {
        return Integer.MAX_VALUE;
    }

    @Override
    public String getName() {
        return "log";
    }

    @Override
    public void run(String[] args, Julti julti) {
        log(Level.INFO, CommandManager.combineArgs(args));
    }
}
