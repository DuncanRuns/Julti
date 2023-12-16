package xyz.duncanruns.julti.command;

import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.cancelrequester.CancelRequester;
import xyz.duncanruns.julti.instance.KeyPresser;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.management.InstanceManager;

import java.util.List;

import static xyz.duncanruns.julti.util.SleepUtil.sleep;

public class ActivateCommand extends Command {

    @Override
    public String helpDescription() {
        return "activate [instances] - Activates the specified instances\nactivate all - Activate all instances\nactivate wall - Activates wall\nactivate all unpause - Activate and unpause all instances";
    }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override
    public int getMaxArgs() {
        return 2;
    }

    @Override
    public String getName() {
        return "activate";
    }

    @Override
    public void run(String[] args, CancelRequester cancelRequester) {
        if (args[0].equals("wall")) {
            Julti.waitForExecute(() -> Julti.getJulti().focusWall());
            return;
        }
        boolean doUnpause;
        if (args.length > 1) {
            if (args[1].equals("unpause")) {
                doUnpause = true;
            } else {
                throw new CommandFailedException("Invalid extra argument for activate command! The only extra argument available is \"unpause\"");
            }
        } else {
            doUnpause = false;
        }
        List<MinecraftInstance> instances = args[0].equals("all") ? InstanceManager.getInstanceManager().getInstances() : CommandManager.getInstances(args[0]);
        if (instances.isEmpty()) {
            throw new CommandFailedException("No instances found");
        }
        // Do setup mode for multiple instances
        boolean doingSetup = instances.size() > 1;
        for (MinecraftInstance i : instances) {
            if (cancelRequester.isCancelRequested()) {
                return;
            }
            Julti.waitForExecute(() -> {
                Julti.getJulti().activateInstance(i, doingSetup);
                if (doingSetup) {
                    MinecraftInstance selectedInstance = InstanceManager.getInstanceManager().getSelectedInstance();
                    if (selectedInstance == null) {
                        return;
                    }
                    if (doUnpause) {
                        sleep(50);
                        KeyPresser keyPresser = selectedInstance.getKeyPresser();
                        if (keyPresser != null) {
                            keyPresser.pressEsc();
                            keyPresser.pressEsc();
                            keyPresser.pressEsc();
                        }
                    }
                    if (selectedInstance.isFullscreen()) {
                        selectedInstance.ensureNotFullscreen();
                    }
                }
            });
            sleep(500);
        }
    }
}
