package xyz.duncanruns.julti.command;

import xyz.duncanruns.julti.cancelrequester.CancelRequester;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.management.InstanceManager;

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
    public void run(String[] args, CancelRequester cancelRequester) {
        List<MinecraftInstance> toClose;
        if (args[0].equals("all")) {
            toClose = InstanceManager.getInstanceManager().getInstances();
        } else {
            toClose = CommandManager.getInstances(args[0]);
        }

        if (toClose.isEmpty()) {
            throw new CommandFailedException("No instances found");
        }
        for (MinecraftInstance instance : toClose) {
            if (cancelRequester.isCancelRequested()) {
                return;
            }
            instance.closeWindow();
        }
    }
}
