package xyz.duncanruns.julti.command;

import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.util.requester.CancelRequester;

import java.util.List;

public class LockCommand extends Command {
    @Override
    public String helpDescription() {
        return "lock [instances] - Locks the specified instances\nlock all - Locks all instances";
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
        return "lock";
    }

    @Override
    public void run(String[] args, Julti julti, CancelRequester cancelRequester) {
        List<MinecraftInstance> toLock = this.getInstancesFromArg(args[0], julti);
        toLock.forEach(i -> julti.getResetManager().lockInstance(i));
    }
}
