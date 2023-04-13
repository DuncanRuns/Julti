package xyz.duncanruns.julti.command;

import xyz.duncanruns.julti.cancelrequester.CancelRequester;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.management.InstanceManager;

public class OpenToLanCommand extends Command {
    @Override
    public String helpDescription() {
        return "opentolan - Opens the current active instance to lan";
    }

    @Override
    public int getMinArgs() {
        return 0;
    }

    @Override
    public int getMaxArgs() {
        return 0;
    }

    @Override
    public String getName() {
        return "opentolan";
    }

    @Override
    public void run(String[] args, CancelRequester cancelRequester) {
        MinecraftInstance selectedInstance = InstanceManager.getManager().getSelectedInstance();
        if (selectedInstance != null) {
            selectedInstance.openToLan();
        }
    }
}
