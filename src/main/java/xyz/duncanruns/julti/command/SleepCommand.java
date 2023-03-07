package xyz.duncanruns.julti.command;

import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.util.CancelRequester;

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
    public void run(String[] args, Julti julti, CancelRequester cancelRequester) {
        long millis = Long.parseLong(args[0]);
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < millis && !cancelRequester.isCancelRequested()) {
            sleep(200);
        }
    }
}
