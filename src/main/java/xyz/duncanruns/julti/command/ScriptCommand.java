package xyz.duncanruns.julti.command;

import xyz.duncanruns.julti.cancelrequester.CancelRequester;
import xyz.duncanruns.julti.script.ScriptManager;

public class ScriptCommand extends Command {
    @Override
    public String helpDescription() {
        return "script [script name] - Runs a script of the specified name";
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
        return "script";
    }

    @Override
    public void run(String[] args, CancelRequester cancelRequester) {
        String scriptName = CommandManager.combineArgs(args);
        if (!ScriptManager.getScriptNames().contains(scriptName)) {
            throw new CommandFailedException("Command failed: invalid script name!");
        }
        ScriptManager.runScriptAndWait(scriptName);
    }
}
