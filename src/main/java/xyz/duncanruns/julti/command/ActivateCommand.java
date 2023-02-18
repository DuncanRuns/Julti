package xyz.duncanruns.julti.command;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.instance.MinecraftInstance;

import java.util.List;

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
    public void run(String[] args, Julti julti) {
        if (args[0].equals("wall")) {
            julti.focusWall();
            return;
        }
        List<MinecraftInstance> instances = args[0].equals("all") ? julti.getInstanceManager().getInstances() : CommandManager.getInstances(args[0], julti);
        if (instances.size() == 0) {
            log(Level.ERROR, "No instance found");
            return;
        }
        List<MinecraftInstance> allInstances = julti.getInstanceManager().getInstances();
        for (MinecraftInstance i : instances) {
            i.activate(1 + allInstances.indexOf(i));
            julti.switchScene(i);
            sleep(500);
        }
    }
}
