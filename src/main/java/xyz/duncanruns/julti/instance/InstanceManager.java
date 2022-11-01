package xyz.duncanruns.julti.instance;

import com.sun.jna.Pointer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.duncanruns.julti.util.HwndUtil;
import xyz.duncanruns.julti.util.LogReceiver;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class InstanceManager {

    private static final Logger LOGGER = LogManager.getLogger("InstanceManager");

    private final List<MinecraftInstance> instances;

    public InstanceManager(List<Path> instancePaths) {
        this();
        for (Path path : instancePaths) {
            MinecraftInstance instance = new MinecraftInstance(path);
            // Eat the missing reports because
            instance.justWentMissing();
            instances.add(instance);
        }
    }

    public InstanceManager() {
        instances = new ArrayList<>();
    }


    @Nullable
    public MinecraftInstance getSelectedInstance() {
        Pointer hwnd = HwndUtil.getCurrentHwnd();
        List<MinecraftInstance> instances = getInstances();
        for (MinecraftInstance instance : instances) {
            if (instance.hasWindow() && instance.getHwnd().equals(hwnd)) {
                return instance;
            }
        }
        return null;
    }

    synchronized public List<MinecraftInstance> getInstances() {
        return Collections.unmodifiableList(new ArrayList<>(instances));
    }

    synchronized public void redetectInstances() {
        // Get all windows that claim to be a minecraft instance wrapped in the MinecraftInstance class
        List<MinecraftInstance> newInstances = getAllOpenInstances();

        // Remove anything that doesn't have a game directory in its command line (remove sus imposters)
        newInstances.removeIf(minecraftInstance -> (!minecraftInstance.isActuallyMC()));

        // Sort instances
        newInstances.sort(Comparator.comparingInt(MinecraftInstance::getSortingNum));

        // Swich found instances into list
        instances.clear();
        instances.addAll(newInstances);

        // Rename windows to instance numbers
        renameWindows();

        // Output instances to log
        logInstances();
    }

    private static List<MinecraftInstance> getAllOpenInstances() {
        List<MinecraftInstance> instanceList = new ArrayList<>();
        for (Pointer hwnd : HwndUtil.getAllMinecraftHwnds()) {
            MinecraftInstance instance = new MinecraftInstance(hwnd);
            instanceList.add(instance);
        }
        return instanceList;
    }

    synchronized public void renameWindows() {
        int i = 0;
        log(Level.INFO, "Renaming windows...");
        for (MinecraftInstance instance : instances) {
            instance.setWindowTitle("Minecraft* - Instance " + (++i));
        }
    }

    private void logInstances() {
        int i = 0;
        for (MinecraftInstance instance : instances) {
            log(Level.INFO, "Instance " + (++i) + ": " + instance.getName());
        }
    }

    public static void log(Level level, String message) {
        LOGGER.log(level, message);
        LogReceiver.receive(level, message);
    }

    public void manageMissingInstances() {
        manageMissingInstances(minecraftInstance -> {
        });
    }

    public void manageMissingInstances(Consumer<MinecraftInstance> onInstanceLoad) {
        // Be careful not to call this too often, as it is slow (10's or 100's of milliseconds)
        List<MinecraftInstance> newMissingInstances = getNewMissingInstances();
        if (!newMissingInstances.isEmpty()) {
            for (MinecraftInstance instance : newMissingInstances) {
                log(Level.WARN, "Instance is missing: " + instance);
            }
        }
        if (hasMissingWindows()) {
            List<MinecraftInstance> newFoundInstances = findMissingWindows();
            if (!newFoundInstances.isEmpty()) {
                for (MinecraftInstance instance : newFoundInstances) {
                    log(Level.INFO, "Found instance: " + instance);
                    onInstanceLoad.accept(instance);
                }
            }
        }
    }

    synchronized private List<MinecraftInstance> getNewMissingInstances() {
        // Returns instances that have gone missing.
        List<MinecraftInstance> newMissingInstances = new ArrayList<>();
        for (MinecraftInstance instance : instances) {
            if (instance.justWentMissing()) newMissingInstances.add(instance);
        }
        return newMissingInstances;
    }

    synchronized public boolean hasMissingWindows() {
        for (MinecraftInstance instance : instances) {
            if (!instance.hasWindow()) {
                return true;
            }
        }
        return false;
    }

    public List<MinecraftInstance> findMissingWindows() {
        // Be careful not to call this too often, as it is slow (10's or 100's of milliseconds)

        List<MinecraftInstance> replacementInstances = new ArrayList<>();
        try {
            if (hasMissingWindows()) {
                List<MinecraftInstance> newInstances = getAllOpenInstances();
                newInstances.removeIf(minecraftInstance -> instances.contains(minecraftInstance));
                // Remove any non-minecrafts; this will also get the instance paths
                newInstances.removeIf(minecraftInstance -> !minecraftInstance.isActuallyMC());

                // Only synchronize after all powershell calls (getting instance paths)
                synchronized (this) {
                    for (MinecraftInstance instanceToReplace : new ArrayList<>(instances)) { // Copy list since instances field is changed inside of loop
                        if (!instanceToReplace.hasWindow() && instanceToReplace.isActuallyMC()) {
                            // instanceToReplace variable name is only accurate inside this if statement.
                            for (MinecraftInstance instanceToUse : newInstances) {
                                if (instanceToUse.getInstancePath().equals(instanceToReplace.getInstancePath())) {
                                    replacementInstances.add(instanceToUse);
                                    instances.set(instances.indexOf(instanceToReplace), instanceToUse);
                                    renameWindows();
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // Evil try-catch because windows may get closed in the miliseconds needed to check instance path
        }
        return replacementInstances;
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder("InstanceManager with " + instances.size() + " instances:");
        for (MinecraftInstance instance : instances) {
            out.append("\n\t").append(instance).append(instance.hasWindow() ? " (opened)" : "(closed)");
        }
        return out.toString();
    }

    public void clearAllWorlds() {
        for (MinecraftInstance instance : new ArrayList<>(instances)) {
            instance.tryClearWorlds();
        }
    }

    synchronized public void removeInstance(MinecraftInstance instance) {
        removeInstanceByIndex(instances.indexOf(instance));
    }

    /**
     * Removes an instance by its index, not its actual number (Instance #1 has index 0).
     *
     * @param ind the instance index
     */
    synchronized public void removeInstanceByIndex(int ind) {
        MinecraftInstance removed = instances.remove(ind);
        log(Level.INFO, "Removed Instance #" + (ind + 1) + ": " + removed.getName());
    }
}
