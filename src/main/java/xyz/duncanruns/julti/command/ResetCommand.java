package xyz.duncanruns.julti.command;

import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.util.requester.CancelRequester;

import java.util.List;

public class ResetCommand extends Command {
    @Override
    public String helpDescription() {
        return "reset [instances] - Resets the specified instances\nreset all - Resets all instances";
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
        return "reset";
    }

    @Override
    public void run(String[] args, Julti julti, CancelRequester cancelRequester) {
        List<MinecraftInstance> toReset = this.getInstancesFromArg(args[0], julti);
        toReset.forEach(i -> julti.getResetManager().resetInstance(i));
    }
}
