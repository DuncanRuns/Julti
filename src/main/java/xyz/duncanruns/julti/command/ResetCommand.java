package xyz.duncanruns.julti.command;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.cancelrequester.CancelRequester;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.management.InstanceManager;
import xyz.duncanruns.julti.resetting.ResetHelper;
import xyz.duncanruns.julti.util.DoAllFastUtil;

import java.util.List;

import static xyz.duncanruns.julti.Julti.log;

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
    public void run(String[] args, CancelRequester cancelRequester) {
        List<MinecraftInstance> toReset;
        if (args[0].equals("all")) {
            toReset = InstanceManager.getManager().getInstances();
        } else {
            toReset = CommandManager.getInstances(args[0]);
        }

        if (toReset.isEmpty()) {
            throw new CommandFailedException("No instances found");
        }
        Julti.waitForExecute(() -> DoAllFastUtil.doAllFast(toReset, instance -> ResetHelper.getManager().resetInstance(instance)));
    }
}
