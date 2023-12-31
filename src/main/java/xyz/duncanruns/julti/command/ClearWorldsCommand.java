package xyz.duncanruns.julti.command;

import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.cancelrequester.CancelRequester;
import xyz.duncanruns.julti.util.BopperUtil;

public class ClearWorldsCommand extends Command {
    @Override
    public String helpDescription() {
        return "clearworlds - Clears worlds";
    }

    @Override
    public int getMinArgs() {
        return 0;
    }

    @Override
    public int getMaxArgs() {
        return 0;
    }

    @Override
    public String getName() {
        return "clearworlds";
    }

    @Override
    public void run(String[] args, CancelRequester cancelRequester) {
        Julti.waitForExecute(BopperUtil::clearWorlds);
    }
}
