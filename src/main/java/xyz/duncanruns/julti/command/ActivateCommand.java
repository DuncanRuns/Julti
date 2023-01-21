package xyz.duncanruns.julti.command;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.instance.MinecraftInstance;

import java.util.List;

public class ActivateCommand extends Command {

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
        return "activate";
    }

    @Override
    public boolean run(String[] args, Julti julti) {
        List<MinecraftInstance> instances = CommandManager.getInstances(args[0], julti);
        if (instances.size() != 1) {
            log(Level.ERROR, "No instance found");
            return false;
        }
        MinecraftInstance i = instances.get(0);
        i.activate(1 + julti.getInstanceManager().getInstances().indexOf(i));
        return true;
    }
}
