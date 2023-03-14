package xyz.duncanruns.julti.command;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.util.requester.CancelRequester;

import java.util.List;
import java.util.function.BooleanSupplier;

public class WaitCommand extends Command {
    @Override
    public String helpDescription() {
        return "wait <launch/previewload/load> [instances/all] - Wait until the specified instances have reached a certain state";
    }

    @Override
    public int getMinArgs() {
        return 2;
    }

    @Override
    public int getMaxArgs() {
        return 2;
    }

    @Override
    public String getName() {
        return "wait";
    }

    @Override
    public void run(String[] args, Julti julti, CancelRequester cancelRequester) {
        List<MinecraftInstance> instances = args[1].equals("all") ? julti.getInstanceManager().getInstances() : CommandManager.getInstances(args[1], julti);
        if (instances.size() == 0) {
            log(Level.ERROR, "No instance found");
            return;
        }
        for (MinecraftInstance instance : instances) {
            BooleanSupplier supplier;
            switch (args[0]) {
                case "launch":
                    supplier = instance::hasWindowOrBeenReplaced;
                    break;
                case "previewload":
                    supplier = instance::isPreviewLoaded;
                    break;
                case "load":
                    supplier = instance::isWorldLoaded;
                    break;
                default:
                    log(Level.ERROR, "Invalid wait argument! Please use launch, previewload, or load.");
                    return;
            }
            while (!cancelRequester.isCancelRequested() && !supplier.getAsBoolean()) {
                sleep(50);
            }
        }
        if (!cancelRequester.isCancelRequested()) {
            log(Level.INFO, "Finished waiting for " + instances.size() + " instances.");
        }
    }
}
