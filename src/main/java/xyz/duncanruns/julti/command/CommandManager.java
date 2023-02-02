package xyz.duncanruns.julti.command;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.util.LogReceiver;

import java.util.*;
import java.util.regex.Pattern;

public class CommandManager {
    private final static Pattern INSTANCES_ARG_PATTERN = Pattern.compile("^(~?-?\\d+)(,(~?-?\\d+))*$");
    private static final Logger LOGGER = LogManager.getLogger("CommandManager");
    public final Command[] commands;

    public CommandManager(Command[] commands) {
        this.commands = commands;
    }

    public static List<MinecraftInstance> getInstances(String instancesArg, Julti julti) {
        if (!INSTANCES_ARG_PATTERN.matcher(instancesArg).matches()) {
            return Collections.emptyList();
        }
        List<MinecraftInstance> out = new ArrayList<>();
        List<MinecraftInstance> allInstances = julti.getInstanceManager().getInstances();

        for (String instanceArg : instancesArg.split(",")) {
            if (instanceArg.startsWith("~")) {
                out.add(julti.getResetManager().getRelativeInstance(Integer.parseInt(instanceArg.substring(1))));
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

    public void runCommand(String command, Julti julti) {
        if (command.isEmpty()) return;
        runCommand(command.trim().split(" "), julti);
    }

    public void runCommand(String[] commandWords, Julti julti) {
        if (commandWords[0].equals("help") || commandWords[0].equals("?")) {
            log(Level.INFO, "Commands:\n\n" + getDescriptions(true));
            return;
        }
        boolean foundCommand = false;
        for (Command command : commands) {
            if (!command.getName().equals(commandWords[0])) continue;
            foundCommand = true;
            String[] args = withoutFirst(commandWords);
            if (args.length < command.getMinArgs() || args.length > command.getMaxArgs()) {
                log(Level.WARN, "Command failed: Incorrect amount of arguments!");
                return;
            }
            try {
                command.run(args, julti);
                return;
            } catch (Exception e) {
                if (e.getClass() == RuntimeException.class) log(Level.ERROR, "Command failed:\n" + e);
                else log(Level.ERROR, "Command failed:\n" + e);
            }
        }
        if (!foundCommand) log(Level.WARN, "Command does not exists.");
    }

    public static void log(Level level, String message) {
        LOGGER.log(level, message);
        LogReceiver.receive(level, message);
    }

    public String getDescriptions(boolean separateDescriptions) {
        StringBuilder out = new StringBuilder();
        for (Command command : commands) {
            if (separateDescriptions) out.append("\n");
            out.append("\n").append(command.helpDescription());
        }
        return out.toString().trim();
    }

    public static String[] withoutFirst(String[] args) {
        return Arrays.copyOfRange(args, 1, args.length);
    }
}
