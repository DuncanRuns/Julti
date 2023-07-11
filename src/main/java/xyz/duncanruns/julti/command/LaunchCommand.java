package xyz.duncanruns.julti.command;

import xyz.duncanruns.julti.cancelrequester.CancelRequester;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.management.InstanceManager;
import xyz.duncanruns.julti.util.SafeInstanceLauncher;

import java.util.List;

public class LaunchCommand extends Command {

    @Override
    public String helpDescription() {
        return "launch [instances] - Launches the specified instances\nlaunch all - Launch all instances";
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
        return "launch";
    }

    @Override
    public void run(String[] args, CancelRequester cancelRequester) {
        List<MinecraftInstance> toLaunch;
        if (args[0].equals("all")) {
            toLaunch = InstanceManager.getManager().getInstances();
        } else {
            toLaunch = CommandManager.getInstances(args[0]);
        }

        if (toLaunch.isEmpty()) {
            throw new CommandFailedException("No instances found");
        }
        SafeInstanceLauncher.launchInstances(toLaunch, cancelRequester);
    }
}
