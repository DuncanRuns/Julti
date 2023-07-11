package xyz.duncanruns.julti.command;

import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.cancelrequester.CancelRequester;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.management.InstanceManager;

import java.util.List;

import static xyz.duncanruns.julti.util.SleepUtil.sleep;

public class ActivateCommand extends Command {

    @Override
    public String helpDescription() {
        return "activate [instances] - Activates the specified instances\nactivate all - Activate all instances\nactivate wall - Activates wall";
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
        return "activate";
    }

    @Override
    public void run(String[] args, CancelRequester cancelRequester) {
        if (args[0].equals("wall")) {
            Julti.waitForExecute(() -> Julti.getInstance().focusWall());
            return;
        }
        List<MinecraftInstance> instances = args[0].equals("all") ? InstanceManager.getManager().getInstances() : CommandManager.getInstances(args[0]);
        if (instances.size() == 0) {
            throw new CommandFailedException("No instances found");
        }
        // Do setup mode for multiple instances
        boolean doingSetup = instances.size() > 1;
        for (MinecraftInstance i : instances) {
            if (cancelRequester.isCancelRequested()) {
                return;
            }
            Julti.waitForExecute(() -> Julti.getInstance().activateInstance(i, doingSetup));
            sleep(500);
        }
    }
}
