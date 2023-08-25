package xyz.duncanruns.julti.resetting;

import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.util.SoundUtil;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class ResetHelper {

    private static final HashMap<String, Supplier<ResetManager>> RESET_MANAGER_MAP = new HashMap<>();

    static {
        registerResetStyle("Multi", MultiResetManager::getMultiResetManager);
        registerResetStyle("Wall", WallResetManager::getWallResetManager);
        registerResetStyle("Dynamic Wall", DynamicWallResetManager::getDynamicWallResetManager);
    }

    public static boolean registerResetStyle(String name, Supplier<ResetManager> resetManagerSupplier) {
        if (RESET_MANAGER_MAP.containsKey(name)) {
            return false;
        }
        RESET_MANAGER_MAP.put(name, resetManagerSupplier);
        return true;
    }

    public static Set<String> getResetStyles() {
        return RESET_MANAGER_MAP.keySet();
    }

    public static ResetManager getManager() {
        return RESET_MANAGER_MAP.getOrDefault(JultiOptions.getJultiOptions().resetStyle, WallResetManager::getWallResetManager).get();
    }

    public static void run(String hotkeyCode, Point mousePosition) {
        switch (hotkeyCode) {
            case "reset":
                processActionResults(getManager().doReset());
                break;
            case "bgReset":
                processActionResults(getManager().doBGReset());
                break;
            case "wallReset":
                processActionResults(getManager().doWallFullReset());
                break;
            case "wallSingleReset":
                processActionResults(getManager().doWallSingleReset(mousePosition));
                break;
            case "wallLock":
                processActionResults(getManager().doWallLock(mousePosition));
                break;
            case "wallPlay":
                processActionResults(getManager().doWallPlay(mousePosition));
                break;
            case "wallFocusReset":
                processActionResults(getManager().doWallFocusReset(mousePosition));
                break;
            case "wallPlayLock":
                processActionResults(getManager().doWallPlayLock(mousePosition));
                break;
            case "debugHover":
                getManager().doDebugHover(mousePosition);
                break;
        }
    }

    private static void processActionResults(List<ActionResult> actionResults) {
        if (actionResults.isEmpty()) {
            return;
        }

        JultiOptions options = JultiOptions.getJultiOptions();

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
