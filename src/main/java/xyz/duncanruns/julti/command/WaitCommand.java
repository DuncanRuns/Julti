package xyz.duncanruns.julti.command;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.cancelrequester.CancelRequester;
import xyz.duncanruns.julti.instance.InstanceState;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.management.InstanceManager;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

import static xyz.duncanruns.julti.Julti.log;
import static xyz.duncanruns.julti.util.SleepUtil.sleep;

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
    public void run(String[] args, CancelRequester cancelRequester) {
        List<MinecraftInstance> instances = args[1].equals("all") ? InstanceManager.getManager().getInstances() : CommandManager.getInstances(args[1]);
        if (instances.size() == 0) {
            throw new CommandFailedException("No instances found");
        }
        for (MinecraftInstance instance : instances) {
            BooleanSupplier supplier;
            switch (args[0]) {
                case "launch":
                    supplier = () -> InstanceManager.getManager().getMatchingInstance(instance).hasWindow();
                    break;
                case "previewload":
                    supplier = () -> instance.getStateTracker().isCurrentState(InstanceState.PREVIEWING);
                    break;
                case "load":
                    supplier = () -> instance.getStateTracker().isCurrentState(InstanceState.INWORLD);
                    break;
                default:
                    throw new CommandFailedException("Invalid wait argument! Please use launch, previewload, or load.");
            }
            while ((!cancelRequester.isCancelRequested()) && (!supplier.getAsBoolean())) {
                sleep(50);
                if (cancelRequester.isCancelRequested()) {
                    break;
                }
                AtomicBoolean b = new AtomicBoolean(false);
                Julti.waitForExecute(() -> b.set(supplier.getAsBoolean()));
                if (b.get()) {
                    break;
                }
            }
        }
        if (!cancelRequester.isCancelRequested()) {
            log(Level.INFO, "Finished waiting for " + instances.size() + " instances.");
        }
    }
}
