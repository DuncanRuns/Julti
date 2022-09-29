package xyz.duncanruns.julti;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.options.JultiOptions;

import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class Main {

    private static final Logger LOGGER = LogManager.getLogger("Main");

    public static void main(String[] args) {
        JultiOptions.getInstance().tryLoad();
        Julti julti = new Julti();
        julti.start();
        runJultiCLI(julti);
    }

    public static void runJultiCLI(Julti julti) {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        Random random = new Random();

        try {
            while (running) {
                String[] args = scanner.nextLine().split(" ");
                if (args.length == 1) {
                    String singleCommand = args[0];
                    if (singleCommand.equals("redetect")) {
                        julti.redetectInstances();
                        continue;
                    }
                    if (singleCommand.equals("stop")) {
                        log(Level.INFO, "Stopping...");
                        running = false;
                        continue;
                    }
                    if (singleCommand.equals("resetall")) {
                        log(Level.INFO, "Resetting all instances...");
                        julti.getInstanceManager().getInstances().forEach(MinecraftInstance::reset);
                        continue;
                    }
                    if (singleCommand.equals("activaterandom")) {
                        log(Level.INFO, "Activating random instance...");
                        List<MinecraftInstance> instances = julti.getInstanceManager().getInstances();
                        instances.get(random.nextInt(instances.size())).activate();
                        continue;
                    }
                    if (singleCommand.equals("closeallwindows")) {
                        log(Level.INFO, "Closing windows...");
                        julti.getInstanceManager().getInstances().forEach(MinecraftInstance::closeWindow);
                        continue;
                    }
                    if (singleCommand.equals("list")) {
                        int i = 0;
                        for (MinecraftInstance instance : julti.getInstanceManager().getInstances()) {
                            log(Level.INFO, (++i) + ": " + instance.getName() + " - " + instance.getInstancePath());
                        }
                        continue;
                    }
                }
                log(Level.WARN, "Unknown Command");
            }
        } catch (Exception ignored) {
        }
        julti.stop();
        System.exit(0);

    }

    public static void log(Level level, String message) {
        LOGGER.log(level, message);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }
}