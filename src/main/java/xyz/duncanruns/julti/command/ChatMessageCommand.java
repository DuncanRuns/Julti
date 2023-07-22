package xyz.duncanruns.julti.command;

import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.cancelrequester.CancelRequester;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.management.InstanceManager;

public class ChatMessageCommand extends Command {
    @Override
    public String helpDescription() {
        return "chatmessage [message] - Sends a chat message into the current instance";
    }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override
    public int getMaxArgs() {
        return Integer.MAX_VALUE;
    }

    @Override
    public String getName() {
        return "chatmessage";
    }

    @Override
    public void run(String[] args, CancelRequester cancelRequester) {
        MinecraftInstance selectedInstance = InstanceManager.getInstanceManager().getSelectedInstance();
        if (selectedInstance != null) {
            Julti.waitForExecute(() -> selectedInstance.sendChatMessage(CommandManager.combineArgs(args), false));
        }
    }
}
