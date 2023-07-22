package xyz.duncanruns.julti.command;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.cancelrequester.CancelRequester;

import static xyz.duncanruns.julti.Julti.log;

public class OptionCommand extends Command {
    private final CommandManager innerManager = new CommandManager(new Command[]{
            new OptionListCommand(),
            new OptionGetCommand(),
            new OptionSetCommand()
    });

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
        return "option";
    }

    @Override
    public void run(String[] args, CancelRequester cancelRequester) {
        Julti.waitForExecute(() -> this.innerManager.runCommand(args, cancelRequester));
    }

    private static class OptionListCommand extends Command {

        @Override
        public String helpDescription() {
            return "option list - Lists all options";
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
            return "list";
        }

        @Override
        public void run(String[] args, CancelRequester cancelRequester) {
            JultiOptions options = JultiOptions.getJultiOptions();
            StringBuilder optionNames = new StringBuilder();
            for (String optionName : options.getOptionNamesWithType()) {
                if (!optionNames.toString().isEmpty()) {
                    optionNames.append("\n");
                }
                optionNames.append("- ").append(optionName);
            }
            log(Level.INFO, "All available options:\n" + optionNames);
        }
    }

    private static class OptionGetCommand extends Command {

        @Override
        public String helpDescription() {
            return "option get [option] - Gets the value of the specified option";
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
            return "get";
        }

        @Override
        public void run(String[] args, CancelRequester cancelRequester) {
            JultiOptions options = JultiOptions.getJultiOptions();
            String optionName = args[0];
            String value = options.getValueString(optionName);
            if (value == null) {
                throw new CommandFailedException("Option \"" + optionName + "\" does not exist. ");
            } else {
                log(Level.INFO, "Option \"" + optionName + "\" has a value of: " + value);
            }
        }
    }

    private static class OptionSetCommand extends Command {

        @Override
        public String helpDescription() {
            return "option set [option] [value] - Sets the value of the specified option to the specified value";
        }

        @Override
        public int getMinArgs() {
            return 2;
        }

        @Override
        public int getMaxArgs() {
            return Integer.MAX_VALUE;
        }

        @Override
        public String getName() {
            return "set";
        }

        @Override
        public void run(String[] args, CancelRequester cancelRequester) {
            JultiOptions options = JultiOptions.getJultiOptions();
            String[] valueArgs = CommandManager.withoutFirst(args);
            String all = CommandManager.combineArgs(valueArgs);
            String optionName = args[0];
            if (options.trySetValue(optionName, all)) {
                log(Level.INFO, "Set \"" + optionName + "\" to " + options.getValueString(optionName) + ".");
            } else {
                throw new CommandFailedException("Could not set value.");
            }
        }
    }

}