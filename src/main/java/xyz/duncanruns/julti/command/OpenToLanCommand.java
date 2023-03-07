package xyz.duncanruns.julti.command;

import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.util.CancelRequester;

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
    public void run(String[] args, Julti julti, CancelRequester cancelRequester) {
        MinecraftInstance selectedInstance = julti.getInstanceManager().getSelectedInstance();
        if (selectedInstance != null) selectedInstance.openToLan(false);
    }
}
