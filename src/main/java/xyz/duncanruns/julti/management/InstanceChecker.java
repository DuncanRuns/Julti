package xyz.duncanruns.julti.management;

import com.sun.jna.platform.win32.WinDef.HWND;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.util.InstanceInfoUtil;
import xyz.duncanruns.julti.util.WindowTitleUtil;
import xyz.duncanruns.julti.win32.User32;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * The instance checker will run checks for Minecraft instances every second while Julti has missing instances.
 * When it finds new instances, it will send an InstancesFoundQMessage to Julti.
 * Additionally, it will remember all opened Minecraft instances on the computer for whenever needed (redetect instances).
 */
public class InstanceChecker {
    private static final InstanceChecker INSTANCE = new InstanceChecker();

    private final Set<MinecraftInstance> openedInstances = new HashSet<>();
    private Set<HWND> lastCheckedWindows = new HashSet<>();


    private InstanceChecker() {
    }

    public static InstanceChecker getInstanceChecker() {
        return INSTANCE;
    }

    /**
     * Checks for new windows, then checks if they are minecraft windows and gets their path.
     * <p>
     * A general expected execution time is as follows:
     * <li>0.8ms-5ms when there are new non-Minecraft windows</li>
     * <li>300ms or more when there are new Minecraft windows</li>
     * <li>Otherwise 0.5ms-1.2ms</li>
     */
    private void runChecks() {
        AtomicBoolean foundAny = new AtomicBoolean(false);

        Set<HWND> checkedWindows = new HashSet<>();

        Julti.log(Level.DEBUG, "InstanceChecker: Running InstanceChecker checks...");

        User32.INSTANCE.EnumWindows((hWnd, arg) -> {

            // Add the window to checked windows
            checkedWindows.add(hWnd);
            // Return if the window was in the last checked windows
            if (this.lastCheckedWindows.contains(hWnd)) {
                return true;
            }
            // Get the title, return if it is not a minecraft title
            String title = WindowTitleUtil.getHwndTitle(hWnd);
            if (!WindowTitleUtil.matchesMinecraft(title)) {
                return true;
            }
            Julti.log(Level.DEBUG, "InstanceChecker: Minecraft title matched: " + title);
            // Get instance info, return if failing to get the path
            InstanceInfoUtil.FoundInstanceInfo instanceInfo = InstanceInfoUtil.getInstanceInfoFromHwnd(hWnd);
            if (instanceInfo == null) {
                Julti.log(Level.DEBUG, "InstanceChecker: FoundInstanceInfo invalid!");
                return true;
            }
            Julti.log(Level.DEBUG, "InstanceChecker: FoundInstanceInfo found.");
            // Create the instance object
            // Add the minecraft instance to the set of opened instances
            this.openedInstances.add(new MinecraftInstance(hWnd, instanceInfo.instancePath, instanceInfo.versionString));
            Julti.log(Level.DEBUG, "InstanceChecker: Added instance to opened instances.");
            foundAny.set(true);

            return true;
        }, null);

        // Remove any opened instance windows that are NOT REAL!!!
        this.openedInstances.removeIf(instance -> !User32.INSTANCE.IsWindow(instance.getHwnd()));
        // Replace the last checked windows set
        this.lastCheckedWindows = checkedWindows;
        Julti.log(Level.DEBUG, "InstanceChecker: Finished checks.");
    }

    public Set<MinecraftInstance> getAllOpenedInstances() {
        this.runChecks();
        // Return a set with lazy copies of the opened instances
        return this.openedInstances.stream().map(MinecraftInstance::createLazyCopy).collect(Collectors.toSet());
    }
}
