package xyz.duncanruns.julti.command;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.instance.MinecraftInstance;

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
    public void run(String[] args, Julti julti) {
        List<MinecraftInstance> toClose;
        if (args[0].equals("all")) toClose = julti.getInstanceManager().getInstances();
        else toClose = CommandManager.getInstances(args[0], julti);

        if (toClose.isEmpty()) {
            log(Level.ERROR, "No instances found");
            return;
        }
        toClose.forEach(MinecraftInstance::closeWindow);
    }
}
