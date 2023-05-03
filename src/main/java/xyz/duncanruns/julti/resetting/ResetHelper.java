package xyz.duncanruns.julti.resetting;

import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.util.SoundUtil;

import java.awt.*;
import java.util.List;

public class ResetHelper {

    public static ResetManager getManager() {
        switch (JultiOptions.getInstance().resetMode) {
            case 1:
                return WallResetManager.getInstance();
            case 2:
                return DynamicWallResetManager.getInstance();
            default:
                return MultiResetManager.getInstance();
        }
    }

    public static void run(String hotkeyCode, Point mousePosition) {
        switch (hotkeyCode) {
            case "reset":
                playActionSounds(getManager().doReset());
                break;
            case "bgReset":
                playActionSounds(getManager().doBGReset());
                break;
            case "wallReset":
                playActionSounds(getManager().doWallFullReset());
                break;
            case "wallSingleReset":
                playActionSounds(getManager().doWallSingleReset(mousePosition));
                break;
            case "wallLock":
                playActionSounds(getManager().doWallLock(mousePosition));
                break;
            case "wallPlay":
                playActionSounds(getManager().doWallPlay(mousePosition));
                break;
            case "wallFocusReset":
                playActionSounds(getManager().doWallFocusReset(mousePosition));
                break;
            case "wallPlayLock":
                playActionSounds(getManager().doWallPlayLock(mousePosition));
                break;
        }
    }

    private static void playActionSounds(List<ActionResult> actionResults) {
        JultiOptions options = JultiOptions.getInstance();

        // Reset Sounds
        int instancesReset = (int) actionResults.stream().filter(actionResult -> actionResult.equals(ActionResult.INSTANCE_RESET)).count();
        if (instancesReset > 1) {
            SoundUtil.playSound(options.multiResetSound, options.multiResetVolume);
        } else if (instancesReset == 1) {
            SoundUtil.playSound(options.singleResetSound, options.singleResetVolume);
        }

        // Lock Sound
        if (actionResults.contains(ActionResult.INSTANCE_LOCKED)) {
            SoundUtil.playSound(options.lockSound, options.lockVolume);
        }

        // Play Sound
        if (actionResults.contains(ActionResult.INSTANCE_ACTIVATED)) {
            SoundUtil.playSound(options.playSound, options.playVolume);
        }
    }
}
