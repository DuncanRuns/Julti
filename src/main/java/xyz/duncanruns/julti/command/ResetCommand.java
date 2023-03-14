package xyz.duncanruns.julti.command;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.cancelrequester.CancelRequester;
import xyz.duncanruns.julti.instance.MinecraftInstance;

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
        List<MinecraftInstance> toReset;
        if (args[0].equals("all")) {
            toReset = julti.getInstanceManager().getInstances();
        } else {
            toReset = CommandManager.getInstances(args[0], julti);
        }

        if (toReset.isEmpty()) {
            log(Level.ERROR, "No instances found");
            return;
        }
        toReset.forEach(i -> julti.getResetManager().resetInstance(i));
    }
}
