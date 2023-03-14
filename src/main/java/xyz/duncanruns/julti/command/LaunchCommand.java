package xyz.duncanruns.julti.command;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.cancelrequester.CancelRequester;
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
    public void run(String[] args, Julti julti, CancelRequester cancelRequester) {
        List<MinecraftInstance> toLaunch;
        if (args[0].equals("all")) {
            toLaunch = julti.getInstanceManager().getInstances();
        } else {
            toLaunch = CommandManager.getInstances(args[0], julti);
        }

        if (toLaunch.isEmpty()) {
            log(Level.ERROR, "No instances found");
            return;
        }
        SafeInstanceLauncher.launchInstances(toLaunch, cancelRequester);
    }
}
