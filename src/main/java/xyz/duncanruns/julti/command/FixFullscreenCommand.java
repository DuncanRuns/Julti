package xyz.duncanruns.julti.command;

import com.sun.jna.platform.win32.WinDef;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.cancelrequester.CancelRequester;
import xyz.duncanruns.julti.instance.KeyPresser;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.management.ActiveWindowManager;
import xyz.duncanruns.julti.management.InstanceManager;
import xyz.duncanruns.julti.util.GameOptionsUtil;
import xyz.duncanruns.julti.util.MCVersionUtil;
import xyz.duncanruns.julti.util.SleepUtil;
import xyz.duncanruns.julti.util.WindowStateUtil;
import xyz.duncanruns.julti.win32.User32;

import java.util.List;

public class FixFullscreenCommand extends Command {
    public static boolean couldBeFullscreen(MinecraftInstance instance) {
        if (MCVersionUtil.isOlderThan(instance.getVersionString(), "1.16") || MCVersionUtil.isNewerThan(instance.getVersionString(), "1.18.2")) {
            return WindowStateUtil.isHwndBorderless(instance.getHwnd());
        } else {
            return GameOptionsUtil.tryGetBoolOption(instance.getPath(), "fullscreen", false);
        }
    }

    @Override
    public String helpDescription() {
        return "fixfs - attempts to fix unminimized/fullscreen issues";
    }

    @Override
    public int getMinArgs() {
        return 0;
    }

    @Override
    public int getMaxArgs() {
        return 0;
    }

    @Override
    public String getName() {
        return "fixfs";
    }

    @Override
    public void run(String[] args, CancelRequester cancelRequester) {
        List<MinecraftInstance> instances;
        Julti julti = Julti.getJulti();
        synchronized (julti) {
            instances = InstanceManager.getInstanceManager().getInstances();
        }

        for (MinecraftInstance instance : instances) {
            if (instance.isWindowMarkedMissing()) {
                continue;
            }
            WinDef.HWND hwnd = instance.getHwnd();
            if (!User32.INSTANCE.IsIconic(hwnd)) {
                continue;
            }
            synchronized (julti) {
                WindowStateUtil.restoreHwnd(hwnd);
                SleepUtil.sleep(300);
                ActiveWindowManager.activateHwnd(hwnd);
                Integer fullscreenKey = instance.getGameOptions().fullscreenKey;
                if (fullscreenKey == null) {
                    Julti.log(Level.WARN, "Instance " + instance + " does not have a fullscreen key, so unfullscreening cannot be attempted!");
                    continue;
                }
                do {
                    SleepUtil.sleep(300);
                    KeyPresser keyPresser = instance.getKeyPresser();
                    keyPresser.pressKey(fullscreenKey);
                    SleepUtil.sleep(300);
                } while (couldBeFullscreen(instance));
            }
        }
    }
}
