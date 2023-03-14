package xyz.duncanruns.julti.command;

import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.util.requester.CancelRequester;

import java.util.List;

public class CloseCommand extends Command {
    @Override
    public String helpDescription() {
        return "close [instances] - Closes the specified instances\nclose all - Closes all instances";
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
        return "close";
    }

    @Override
    public void run(String[] args, Julti julti, CancelRequester cancelRequester) {
        List<MinecraftInstance> toClose = this.getInstancesFromArg(args[0], julti);
        for (MinecraftInstance instance : toClose) {
            if (cancelRequester.isCancelRequested()) { return; }
            instance.closeWindow();
        }
    }
}
