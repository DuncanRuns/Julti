package xyz.duncanruns.julti.command;

import xyz.duncanruns.julti.Julti;

public class SleepCommand extends Command {
    @Override
    public String helpDescription() {
        return "sleep [time] - Sleep for the specified amount of time (in milliseconds)";
    }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override
    public int getMaxArgs() {
        return 1;
    }

    @Override
    public String getName() {
        return "sleep";
    }

    @Override
    public void run(String[] args, Julti julti) {
        try {
            Thread.sleep(Long.parseLong(args[0]));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
