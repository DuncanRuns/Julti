package xyz.duncanruns.julti.command;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.cancelrequester.CancelRequester;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static xyz.duncanruns.julti.Julti.log;

/**
 * @author Aeroshide
 */
public class OpenFileCommand extends Command {

    @Override
    public String helpDescription() {
        return "openfile [path] - Opens a jar, ahk, bat, or exe file";
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
            throw new CommandFailedException("File not found: " + realPath);
        }

        String fileName = realPath.getFileName().toString();
        String pathExt = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.') + 1) : "";

        switch (pathExt.toLowerCase()) {
            case "jar":
                Path javaExe = Paths.get(System.getProperty("java.home")).resolve("bin").resolve("javaw.exe").toAbsolutePath();
                processBuilder = new ProcessBuilder(javaExe.toString(), "-jar", realPath.toString());
                break;
            case "ahk":
                // TODO: custom ahk install location
                processBuilder = new ProcessBuilder("C:\\Program Files\\AutoHotkey\\AutoHotkey.exe", realPath.toString());
                break;
            case "exe":
            case "bat":
                processBuilder = new ProcessBuilder(realPath.toString());
                break;
            default:
                throw new CommandFailedException("File type \"" + pathExt + "\" not supported!");
        }

        try {
            processBuilder.directory(realPath.getParent().toFile()).start();
            log(Level.INFO, "Opened file " + fileName);
        } catch (IOException e) {
            throw new CommandFailedException(e);
        }
    }

}
