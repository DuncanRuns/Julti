package xyz.duncanruns.julti.command;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.cancelrequester.CancelRequester;
import xyz.duncanruns.julti.util.SoundUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PlaysoundCommand extends Command {
    @Override
    public String helpDescription() {
        return "playsound [volume] [sound] - Plays a sound in the .Julti/sounds folder at the specified volume (A file path may also be specified)";
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
        return "playsound";
    }

    @Override
    public void run(String[] args, Julti julti, CancelRequester cancelRequester) {
        float volume = Float.parseFloat(args[0]);
        String soundString = CommandManager.combineArgs(CommandManager.withoutFirst(args));

        Path pathFromSoundsFolder = JultiOptions.getJultiDir().resolve("sounds").resolve(soundString);
        if (Files.isRegularFile(pathFromSoundsFolder)) {
            SoundUtil.playSound(pathFromSoundsFolder.toFile(), volume);
            return;
        }
        Path wholePath = Paths.get(soundString);
        if (Files.isRegularFile(wholePath)) {
            SoundUtil.playSound(wholePath.toFile(), volume);
            return;
        }
        log(Level.ERROR, "Playsound file location could not be determined!");
    }
}
