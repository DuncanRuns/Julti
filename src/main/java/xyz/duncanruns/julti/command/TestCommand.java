package xyz.duncanruns.julti.command;

import xyz.duncanruns.julti.cancelrequester.CancelRequester;
import xyz.duncanruns.julti.util.ResetCounter;

import java.util.Arrays;

public class TestCommand extends Command {

    CommandManager innerManager = new CommandManager(Arrays.asList(
            new IncrementCommand()
    ));

    @Override
    public String helpDescription() {
        return this.innerManager.getDescriptions(false);
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
        return "test";
    }

    @Override
    public void run(String[] args, CancelRequester cancelRequester) {
        this.innerManager.runCommand(args, cancelRequester);
    }

    static class IncrementCommand extends Command {

        @Override
        public String helpDescription() {
            return "test increment - Increments the reset counter by 1\n" +
                    "test increment [i] - Increments the reset counter by i";
        }

        @Override
        public int getMinArgs() {
            return 0;
        }

        @Override
        public int getMaxArgs() {
            return 1;
        }

        @Override
        public String getName() {
            return "increment";
        }

        @Override
        public void run(String[] args, CancelRequester cancelRequester) {
            int total = 1;
            if (args.length > 0) {
                total = Integer.parseInt(args[0]);
            }
            for (int i = 0; i < total; i++) {
                ResetCounter.increment();
            }
        }
    }
}
