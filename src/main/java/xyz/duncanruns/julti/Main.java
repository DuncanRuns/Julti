package xyz.duncanruns.julti;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;
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

        try {
            while (running) {
                String input = scanner.nextLine();
                if (Objects.equals(input, "stop")) {
                    running = false;
                } else {
                    julti.runCommand(input);
                }
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