package xyz.duncanruns.julti;

import com.formdev.flatlaf.FlatDarkLaf;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.duncanruns.julti.gui.JultiGUI;
import xyz.duncanruns.julti.util.LogReceiver;

import java.util.Objects;
import java.util.Scanner;

public class Main {

    private static final Logger LOGGER = LogManager.getLogger("Main");

    public static void main(String[] args) {
        FlatDarkLaf.setup();
        JultiOptions.getInstance().tryLoad();
        Julti julti = new Julti();
        JultiGUI gui = new JultiGUI(julti);
        julti.start();
        gui.requestFocus();
        // Command line included in GUI
        // runJultiCLI(julti);
    }

    public static void runJultiCLI(Julti julti) {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        try {
            while (running) {
                String input = scanner.nextLine();
                // Allow staring with /
                if (input.startsWith("/")) input = input.substring(1);
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
        LogReceiver.receive(level, message);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }
}