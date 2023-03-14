package xyz.duncanruns.julti.command;

import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.util.requester.CancelRequester;

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
        long endTime = System.currentTimeMillis() + millis;
        long currentTime;
        while ((currentTime = System.currentTimeMillis()) < endTime && !cancelRequester.isCancelRequested()) {
            sleep(Math.min(100, endTime - currentTime));
        }
    }
}
