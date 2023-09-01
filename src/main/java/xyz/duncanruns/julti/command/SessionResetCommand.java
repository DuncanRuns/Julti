package xyz.duncanruns.julti.command;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.cancelrequester.CancelRequester;
import xyz.duncanruns.julti.util.ResetCounter;

public class SessionResetCommand extends Command {
    @Override
    public String helpDescription() {
        return "sessionresets [num] - Sets the current number of resets in the current session";
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
        return "sessionresets";
    }

    @Override
    public void run(String[] args, CancelRequester cancelRequester) {
        ResetCounter.sessionCounter = Integer.parseInt(args[0]);
        ResetCounter.updateFiles();
        Julti.log(Level.INFO, "Updated session reset counter to " + ResetCounter.sessionCounter + ".");
    }
}
