package xyz.duncanruns.julti.command;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.instance.MinecraftInstance;

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
    public boolean run(String[] args, Julti julti) {
        List<MinecraftInstance> toLock;
        if (args[0].equals("all")) toLock = julti.getInstanceManager().getInstances();
        else toLock = CommandManager.getInstances(args[0], julti);

        if (toLock.isEmpty()) {
            log(Level.ERROR, "No instances found");
            return false;
        }
        toLock.forEach(i -> julti.getResetManager().lockInstance(i));
        return true;
    }
}
