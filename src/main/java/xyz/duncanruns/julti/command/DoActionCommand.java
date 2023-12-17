package xyz.duncanruns.julti.command;

import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.cancelrequester.CancelRequester;
import xyz.duncanruns.julti.messages.HotkeyPressQMessage;
import xyz.duncanruns.julti.util.MouseUtil;

public class DoActionCommand extends Command {
    // Julti.getJulti().queueMessage(new HotkeyPressQMessage(hotkeyAction.getRight(), MouseUtil.getMousePos()));
    @Override
    public String helpDescription() {
        return "doaction [action id] - Replicates the functionality of a built in hotkey.";
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
        return "doaction";
    }

    @Override
    public void run(String[] args, CancelRequester cancelRequester) {
        Julti.getJulti().queueMessage(new HotkeyPressQMessage(args[0], MouseUtil.getMousePos()));
    }
}
