package xyz.duncanruns.julti.command;

import xyz.duncanruns.julti.cancelrequester.CancelRequester;
import xyz.duncanruns.julti.script.ScriptManager;

public class GenerateLuaDocsCommand extends Command {
    @Override
    public String helpDescription() {
        return "genluadocs - Generates lua docs for all registered libraries and places them in the .Julti/scripts/libs folder";
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
        return "genluadocs";
    }

    @Override
    public void run(String[] args, CancelRequester cancelRequester) {
        ScriptManager.generateDocs();
    }
}
