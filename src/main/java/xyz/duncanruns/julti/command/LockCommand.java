package xyz.duncanruns.julti.command;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.cancelrequester.CancelRequester;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.management.InstanceManager;
import xyz.duncanruns.julti.resetting.ResetHelper;

import java.util.List;

import static xyz.duncanruns.julti.Julti.log;

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
    public void run(String[] args, CancelRequester cancelRequester) {
        List<MinecraftInstance> toLock;
        if (args[0].equals("all")) {
            toLock = InstanceManager.getManager().getInstances();
        } else {
            toLock = CommandManager.getInstances(args[0]);
        }

        if (toLock.isEmpty()) {
            log(Level.ERROR, "No instances found");
            return;
        }
        Julti.waitForExecute(() -> toLock.forEach(i -> ResetHelper.getManager().lockInstance(i)));
    }
}
