package xyz.duncanruns.julti.command;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.cancelrequester.CancelRequester;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static xyz.duncanruns.julti.Julti.log;

public class OpenCommand extends Command {

    @Override
    public String helpDescription() {
        return "openfile [path] - opens a specific file / app";
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
        return "openfile";
    }

    @Override
    public void run(String[] args, CancelRequester cancelRequester) {
        String path = CommandManager.combineArgs(args).replace("\"", "");
        ProcessBuilder processBuilder;

        Path realPath = Paths.get(path).toAbsolutePath();
        if (!Files.isRegularFile(realPath)) {
            throw new RuntimeException();
        }

        String fileName = realPath.getFileName().toString();
        String pathExt = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.') + 1) : "";

        // this is a dumb implementation, please someone smarter than me do something about this
        switch (pathExt.toLowerCase()) {
            case "jar":
                processBuilder = new ProcessBuilder("java", "-jar", path);
                break;
            case "ahk":
                processBuilder = new ProcessBuilder("C:\\Program Files\\AutoHotkey\\AutoHotkey.exe", path);
                break;
            default:
                processBuilder = new ProcessBuilder(path);
                break;
        }

        try {
            log(Level.INFO, "Application opened");
            processBuilder.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
