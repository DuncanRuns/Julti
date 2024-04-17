package xyz.duncanruns.julti.command;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.cancelrequester.CancelRequester;
import xyz.duncanruns.julti.script.CustomizableManager;
import xyz.duncanruns.julti.util.ResetCounter;

import java.util.Arrays;

public class TestCommand extends Command {

    CommandManager innerManager = new CommandManager(Arrays.asList(
            new IncrementCommand(),
            new AddPluginDataCommand(),
            new RemovePluginDataCommand(),
            new ScriptCustomizeCommand()
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

    static class AddPluginDataCommand extends Command {
        @Override
        public String helpDescription() {
            return "test addplugindata [plugin id] [json]- Adds plugin data to the profile";
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
            return "addplugindata";
        }

        @Override
        public void run(String[] args, CancelRequester cancelRequester) {
            String pluginId = args[0];
            String jsonString = CommandManager.combineArgs(CommandManager.withoutFirst(args));
            JsonObject object = new Gson().fromJson(jsonString, JsonObject.class);
            JultiOptions.getJultiOptions().pluginData.put(pluginId, object);
            Julti.log(Level.INFO, "Added plugin data for " + pluginId);
        }
    }

    static class RemovePluginDataCommand extends Command {

        @Override
        public String helpDescription() {
            return "test removeplugindata [plugin id] - Removes plugin data from the profile";
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
            return "removeplugindata";
        }

        @Override
        public void run(String[] args, CancelRequester cancelRequester) {
            String pluginId = args[0];
            if (JultiOptions.getJultiOptions().pluginData.remove(pluginId) == null) {
                Julti.log(Level.ERROR, "No plugin data exists for " + pluginId);
            } else {
                Julti.log(Level.INFO, "Removed plugin data for " + pluginId);
            }
        }
    }

    static class ScriptCustomizeCommand extends Command {

        @Override
        public String helpDescription() {
            return "test scriptcust <Script Name>,<Variable Name>,<Value>,<Data Type> - Tests adding a value to customizable storage";
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
            return "scriptcust";
        }

        @Override
        public void run(String[] args, CancelRequester cancelRequester) {
            String s = CommandManager.combineArgs(args);
            String[] split = s.split(",");
            if (!CustomizableManager.isValid(split[3], split[2])) {
                Julti.log(Level.ERROR, "Not in bounds");
                return;
            }
            CustomizableManager.set(split[0], split[1], split[2], CustomizableManager.getType(split[3]));
            Julti.log(Level.INFO, "Set");
        }
    }
}
