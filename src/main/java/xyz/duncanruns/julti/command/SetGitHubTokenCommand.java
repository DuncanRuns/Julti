package xyz.duncanruns.julti.command;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.cancelrequester.CancelRequester;
import xyz.duncanruns.julti.util.GitHubUtil;

public class SetGitHubTokenCommand extends Command {
    @Override
    public String helpDescription() {
        return "setgithubtoken [token] - Sets your GitHub token for accessing GitHub hosted things (scripts, legal mods, updates)";
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
        return "setgithubtoken";
    }

    @Override
    public void run(String[] args, CancelRequester cancelRequester) {
        if (GitHubUtil.setToken(args[0])) {
            Julti.log(Level.INFO, "GitHub token set!");
        } else {
            Julti.log(Level.ERROR, "Failed to set token!");
        }
    }
}
