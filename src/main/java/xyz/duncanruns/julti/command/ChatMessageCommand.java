package xyz.duncanruns.julti.command;

import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.cancelrequester.CancelRequester;
import xyz.duncanruns.julti.instance.MinecraftInstance;

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
    public void run(String[] args, Julti julti, CancelRequester cancelRequester) {
        MinecraftInstance selectedInstance = julti.getInstanceManager().getSelectedInstance();
        if (selectedInstance != null) {
            selectedInstance.sendChatMessage(CommandManager.combineArgs(args));
        }
    }
}
