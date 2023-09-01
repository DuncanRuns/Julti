package xyz.duncanruns.julti.command;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.cancelrequester.CancelRequester;
import xyz.duncanruns.julti.cancelrequester.CancelRequesters;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.management.InstanceManager;
import xyz.duncanruns.julti.resetting.ResetHelper;

import java.util.*;
import java.util.regex.Pattern;

import static xyz.duncanruns.julti.Julti.log;

public class CommandManager {
    private final static Pattern INSTANCES_ARG_PATTERN = Pattern.compile("^(~?-?\\d+)(,(~?-?\\d+))*$");
    private final static CommandManager MAIN_MANAGER = new CommandManager(new ArrayList<>(Arrays.asList(new ResetCommand(),
            new LockCommand(),
            new LaunchCommand(),
            new CloseCommand(),
            new ActivateCommand(),
            new OptionCommand(),
            new SleepCommand(),
            new ChatMessageCommand(),
            new OpenToLanCommand(),
            new WaitCommand(),
            new LogCommand(),
            new PlaysoundCommand(),
            new OpenFileCommand())));

    static {
        if (Julti.VERSION.equals("DEV")) {
            MAIN_MANAGER.registerCommand(new TestCommand());
        }
    }

    public final List<Command> commands;

    public CommandManager(List<Command> commands) {
        this.commands = commands;
    }

    public static CommandManager getMainManager() {
        return MAIN_MANAGER;
    }

    public static List<MinecraftInstance> getInstances(String instancesArg) {
        if (!INSTANCES_ARG_PATTERN.matcher(instancesArg).matches()) {
            return Collections.emptyList();
        }
        List<MinecraftInstance> out = new ArrayList<>();
        List<MinecraftInstance> allInstances = InstanceManager.getInstanceManager().getInstances();

        for (String instanceArg : instancesArg.split(",")) {
            if (instanceArg.startsWith("~")) {
                out.add(ResetHelper.getManager().getRelativeInstance(Integer.parseInt(instanceArg.substring(1))));
            } else {
                out.add(allInstances.get((Integer.parseInt(instanceArg) - 1) % allInstances.size()));
            }
        }
        out.removeIf(Objects::isNull);
        return out;
    }

    public static String combineArgs(String[] args) {
        return combineArgs(args, " ");
    }

    public static String combineArgs(String[] args, String separator) {
        StringBuilder out = new StringBuilder();
        for (String arg : args) {
            if (!out.toString().isEmpty()) {
                out.append(separator);
            }
            out.append(arg);
        }
        return out.toString();
    }

    public static String[] withoutFirst(String[] args) {
        return Arrays.copyOfRange(args, 1, args.length);
    }

    public void registerCommand(Command command) {
        this.commands.add(command);
    }

    public void runCommand(String command) {
        this.runCommand(command, CancelRequesters.NEVER_CANCEL_REQUESTER);
    }

    public void runCommand(String command, CancelRequester cancelRequester) {
        if (command.isEmpty()) {
            return;
        }
        this.runCommand(command.trim().split(" "), cancelRequester);
    }

    public void runCommand(String[] commandWords, CancelRequester cancelRequester) {
        if (commandWords[0].equals("help") || commandWords[0].equals("?")) {
            log(Level.INFO, "Commands:\n\n" + this.getDescriptions(true));
            return;
        }
        for (Command command : this.commands) {
            if (!command.getName().equals(commandWords[0])) {
                continue;
            }
            String[] args = withoutFirst(commandWords);
            if (args.length < command.getMinArgs() || args.length > command.getMaxArgs()) {
                log(Level.ERROR, "Command failed: Incorrect amount of arguments!");
                cancelRequester.cancel();
                return;
            }
            try {
                command.run(args, cancelRequester);
            } catch (Exception e) {
                cancelRequester.cancel();
                log(Level.ERROR, "Command failed:\n" + e);
            }
            return;
        }
        log(Level.ERROR, "Command does not exist.");
        cancelRequester.cancel();
    }

    public String getDescriptions(boolean separateDescriptions) {
        StringBuilder out = new StringBuilder();
        for (Command command : this.commands) {
            if (separateDescriptions) {
                out.append("\n");
            }
            out.append("\n").append(command.helpDescription());
        }
        return out.toString().trim();
    }
}
