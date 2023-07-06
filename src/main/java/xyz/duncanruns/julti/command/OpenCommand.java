package xyz.duncanruns.julti.command;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.cancelrequester.CancelRequester;

import java.io.IOException;

import static xyz.duncanruns.julti.Julti.log;

public class OpenCommand extends Command {

    @Override
    public String helpDescription() {
        return "open [path] - opens a specific file / app";
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
        return "open";
    }

    @Override
    public void run(String[] args, CancelRequester cancelRequester) {
        String path = args[0].replace("\"", "");
        ProcessBuilder processBuilder;

        String pathExt = "";

        int index = path.lastIndexOf(".");
        if (index != -1) {
            pathExt = path.substring(index + 1);
        }

        // this is a dumb implementation, please someone smarter than me do something about this
        switch (pathExt.toLowerCase()) {
            case "jar":
                processBuilder = new ProcessBuilder("java", "-jar", args[0]);
                break;
            case "ahk":
                processBuilder = new ProcessBuilder("C:\\Program Files\\AutoHotkey\\AutoHotkey.exe", args[0]);
                break;
            default:
                processBuilder = new ProcessBuilder(args[0]);
                break;
        }

        try {
            log(Level.INFO, "Application opened");
            processBuilder.start();
        } catch (IOException e) {
            log(Level.ERROR, "Check that your application is runnable or the path is invalid");
            throw new RuntimeException(e);
        }
    }

}
