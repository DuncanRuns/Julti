package xyz.duncanruns.julti.instance;

import com.sun.jna.platform.win32.WinDef;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.duncanruns.julti.util.HwndUtil;
import xyz.duncanruns.julti.util.LogReceiver;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class InstanceManager {
    private static final Logger LOGGER = LogManager.getLogger("InstanceManager");
    private final CopyOnWriteArrayList<InstanceHolder> instanceHolders;

    public InstanceManager(List<Path> instancePaths) {
        this();
        for (Path path : instancePaths) {
            this.instanceHolders.add(new InstanceHolder(path));
        }
    }

    public InstanceManager() {
        this.instanceHolders = new CopyOnWriteArrayList<>();
    }

    public MinecraftInstance getSelectedInstance() {
        WinDef.HWND hwnd = HwndUtil.getCurrentHwnd();
        List<MinecraftInstance> instances = this.getInstances();
        for (MinecraftInstance instance : instances) {
            if (instance.hasWindow() && instance.getHwnd().equals(hwnd)) {
                return instance;
            }
        }
        return null;
    }

    public synchronized List<MinecraftInstance> getInstances() {
        List<MinecraftInstance> instances = new ArrayList<>();
        this.instanceHolders.forEach(instanceHolder -> instances.add(instanceHolder.instance));
        return Collections.unmodifiableList(instances);
    }

    public synchronized void redetectInstances() {
        List<MinecraftInstance> newInstances = getAllConfirmedOpenedInstances();

        // Sort instances
        newInstances.sort(Comparator.comparingInt(MinecraftInstance::getNameSortingNum));

        // Switch found instances into list
        List<InstanceHolder> newHolders = new ArrayList<>();
        newInstances.forEach(instance -> newHolders.add(new InstanceHolder(instance)));
        this.instanceHolders.clear();
        this.instanceHolders.addAll(newHolders);

        // Rename windows to instance numbers
        this.renameWindows();

        // Output instances to log
        this.logInstances();
    }

    private static List<MinecraftInstance> getAllConfirmedOpenedInstances() {
        // Get all instances that actually are mc
        return getAllOpenInstances().stream().filter(MinecraftInstance::isActuallyMC).collect(Collectors.toList());
    }

    public void renameWindows() {
        int i = 0;
        log(Level.INFO, "Renaming windows...");
        for (MinecraftInstance instance : this.getInstances()) {
            instance.setWindowTitle("Minecraft* - Instance " + (++i));
        }
    }

    private void logInstances() {
        int i = 0;
        for (MinecraftInstance instance : this.getInstances()) {
            log(Level.INFO, "Instance " + (++i) + ": " + instance.getName());
        }
    }

    private static List<MinecraftInstance> getAllOpenInstances() {
        List<MinecraftInstance> instanceList = new ArrayList<>();
        for (WinDef.HWND hwnd : HwndUtil.getAllMinecraftHwnds()) {
            MinecraftInstance instance = new MinecraftInstance(hwnd);
            instanceList.add(instance);
        }
        return instanceList;
    }

    public static void log(Level level, String message) {
        LOGGER.log(level, message);
        LogReceiver.receive(level, message);
    }

    /**
     * Manages missing instances
     *
     * @param onInstanceLoad a method to be called and given an instance upon its initial loading
     * @return true if any instances go missing or get found, otherwise false
     */
    public boolean manageMissingInstances(Consumer<MinecraftInstance> onInstanceLoad) {
        AtomicBoolean out = new AtomicBoolean(false);
        this.instanceHolders.stream().filter(InstanceHolder::justWentMissing).map(instanceHolder -> instanceHolder.instance).forEach(instance -> {
            log(Level.WARN, "Instance is missing: " + instance);
            out.set(true);
        });
        List<InstanceHolder> holdersWithMissing = this.instanceHolders.stream().filter(InstanceHolder::isMissing).collect(Collectors.toList());
        if (out.get() || !holdersWithMissing.isEmpty()) {
            // relevantPaths contains paths of instances that are missing
            List<Path> relevantPaths = holdersWithMissing.stream().map(instanceHolder -> instanceHolder.path).collect(Collectors.toList());
            // Get all opened instances, and if their path is in the relevantPaths list, insert itself into any instance holders with a matching path.
            getAllConfirmedOpenedInstances().stream().filter(instance -> relevantPaths.contains(instance.getInstancePath())).forEach(instance -> {
                holdersWithMissing.stream().filter(instanceHolder -> instanceHolder.path.equals(instance.getInstancePath())).forEach(instanceHolder -> instanceHolder.setInstance(instance));
                log(Level.INFO, "Found instance: " + instance.getName());
                onInstanceLoad.accept(instance);
                out.set(true);
            });
        }
        return out.get();
    }

    public void clearAllWorlds() {
        new Thread(() -> {
            for (MinecraftInstance instance : this.getInstances()) {
                log(Level.INFO, "Clearing worlds for " + instance + "...");
                instance.tryClearWorlds();
            }
            log(Level.INFO, "Done clearing worlds!");
        }, "world-clearing").start();
    }

    public synchronized void removeInstance(MinecraftInstance instance) {
        this.instanceHolders.removeIf(instanceHolder -> instanceHolder.path.equals(instance.getInstancePath()));
    }

    public synchronized void resetInstanceData() {
        this.instanceHolders.forEach(InstanceHolder::resetInstanceData);
    }

    private static class InstanceHolder {
        private final Path path;
        private MinecraftInstance instance;

        public InstanceHolder(MinecraftInstance instance) {
            this.path = instance.getInstancePath();
            this.instance = instance;
        }

        public InstanceHolder(Path path) {
            this.path = path;
            this.instance = new MinecraftInstance(path);
            this.instance.justWentMissing();
        }

        public boolean isMissing() {
            return !this.instance.hasWindow();
        }

        public boolean justWentMissing() {
            return this.instance.justWentMissing();
        }

        public void resetInstanceData() {
            this.setInstance(new MinecraftInstance(this.path));
            this.instance.justWentMissing();
        }

        public void setInstance(MinecraftInstance instance) {
            this.instance.markReplaced();
            this.instance = instance;
        }
    }
}
