package xyz.duncanruns.julti.command;

import xyz.duncanruns.julti.cancelrequester.CancelRequester;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.management.InstanceManager;

public class CancelIfCommand extends Command {
    @Override
    public String helpDescription() {
        return "cancelif [playtime] ...";
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
        return "cancelif";
    }

    @Override
    public void run(String[] args, CancelRequester cancelRequester) {
        switch (args[0]) {
            case "playtime":
                this.runPlaytime(CommandManager.withoutFirst(args), cancelRequester);
                break;
            default:
                throw new CommandFailedException("Invalid cancelif command!");
        }
    }

    private void runPlaytime(String[] args, CancelRequester cancelRequester) {
        if (args.length < 2) {
            throw new CommandFailedException("Invalid cancelif playtime command! Example usage (spaces required): cancelif playtime > 10000");
        }
        MinecraftInstance playingInstance = InstanceManager.getInstanceManager().getSelectedInstance();
        if (playingInstance == null) {
            return;
        }

        long specifiedTime = Long.parseLong(args[1]);
        long playTime = Math.abs(System.currentTimeMillis() - playingInstance.getLastActivation());

        switch (args[0]) {
            case "<":
                if (playTime < specifiedTime) {
                    cancelRequester.cancel();
                }
                break;
            case ">":
                if (playTime > specifiedTime) {
                    cancelRequester.cancel();
                }
                break;
            default:
                throw new CommandFailedException("Invalid cancelif playtime command! Example usage: cancelif playtime > 10000");
        }
    }
}
