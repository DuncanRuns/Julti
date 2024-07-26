package xyz.duncanruns.julti.management;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.instance.InstanceState;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.plugin.PluginEvents;
import xyz.duncanruns.julti.resetting.ResetHelper;
import xyz.duncanruns.julti.util.DoAllFastUtil;
import xyz.duncanruns.julti.util.MouseUtil;
import xyz.duncanruns.julti.util.SleepBGUtil;
import xyz.duncanruns.julti.win32.User32;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static xyz.duncanruns.julti.util.SleepUtil.sleep;

public final class InstanceManager {
    private static final InstanceManager INSTANCE = new InstanceManager();
    private final ArrayList<MinecraftInstance> instances = new ArrayList<>();

    private boolean utilityMode = false;
    private boolean instancesMissing = true;

    private InstanceManager() {
    }

    public static InstanceManager getInstanceManager() {
        return INSTANCE;
    }

    public List<MinecraftInstance> getInstances() {
        return this.instances;
    }

    public void onOptionsLoad() {
        this.loadInstances();
        this.checkOpenedInstances();
    }

    public void checkOpenedInstances() {
        Set<MinecraftInstance> replacements = InstanceChecker.getInstanceChecker().getAllOpenedInstances();

        // For each of Julti's instances
        AtomicBoolean instancesFound = new AtomicBoolean(false);
        for (int i = 0; i < this.instances.size(); i++) {
            // If the instance is not missing a window, skip
            if (!this.instances.get(i).checkWindowMissing()) {
                continue;
            }
            // Get the path and store a final i
            Path path = this.instances.get(i).getPath();
            final int finalI = i;
            // Look through the potential replacements, if any match the path, replace Julti's instance with it
            replacements.stream().filter(instance -> instance.getPath().equals(path)).findAny().ifPresent(instance -> {
                this.instances.set(finalI, instance);
                instance.discoverInformation();
                instancesFound.set(true);
                Julti.log(Level.INFO, "Found instance: " + instance.getName());
            });
        }

        if (instancesFound.get() && !this.checkInstancesMarkedMissing()) {
            this.onAllInstancesFound();
        }

        // Recheck missing instances
        this.checkInstancesMarkedMissing();
    }

    private void onAllInstancesFound() {
        this.renameWindows();
        SleepBGUtil.disableLock();
        DoAllFastUtil.doAllFast(MinecraftInstance::discoverInformation);
        this.instances.forEach(instance -> {
            instance.activate(true);
            MouseUtil.clickTopLeft(instance.getHwnd());
            sleep(50);
        });
        OBSStateManager.getOBSStateManager().tryOutputLSInfo();
        ResetHelper.getManager().reload();
        PluginEvents.RunnableEventType.ALL_INSTANCES_FOUND.runAll();
    }


    private boolean checkInstancesMarkedMissing() {
        boolean instancesMissing = false;
        for (MinecraftInstance instance : this.instances) {
            if (instance.isWindowMarkedMissing()) {
                instancesMissing = true;
                break;
            }
        }
        this.instancesMissing = instancesMissing;
        return this.instancesMissing;
    }

    public void runChecks(){
        if (this.utilityMode) {
            this.runChecksUtility();
        } else {
            this.runChecksRegular();
        }
    }

    public void tick(long cycles) {
        if (JultiOptions.getJultiOptions().utilityMode != this.utilityMode) {
            this.reloadUtilityMode();
        }
        if (this.utilityMode) {
            this.tickUtility(cycles);
        } else {
            this.tickRegular(cycles);
        }
    }

    private void reloadUtilityMode() {
        this.utilityMode = JultiOptions.getJultiOptions().utilityMode;
        if (this.utilityMode) {
            this.instances.clear();
        } else {
            this.loadInstances();
        }
        this.runChecks();
    }

    private void tickUtility(long cycles) {
        this.instances.forEach(MinecraftInstance::checkWindowMissing);
        this.instances.removeIf(MinecraftInstance::isWindowMarkedMissing);
        if (cycles % 5000 == 0 && this.getSelectedInstance() == null) {
            this.runChecksUtility();
        }
        this.instancesMissing = false;
    }

    private void runChecksUtility() {
        InstanceChecker.getInstanceChecker().getAllOpenedInstances().stream().filter(i -> !this.instances.contains(i)).forEach(i -> {
            i.discoverInformation();
            this.instances.add(i);
        });
    }

    private void tickRegular(long cycles) {
        this.instances.forEach(MinecraftInstance::checkWindowMissing);
        if (cycles % 5000 == 0) {
            this.runChecksRegular();
        }
    }

    private void runChecksRegular() {
        if (this.checkInstancesMarkedMissing()) {
            this.checkOpenedInstances();
            return;
        }
        if (this.instances.stream().anyMatch(instance -> instance.getStateTracker().isCurrentState(InstanceState.TITLE))) {
            this.renameWindows();
        }
    }

    public void tickInstances() {
        for (MinecraftInstance instance : this.instances) {
            if (instance.isWindowMarkedMissing()) {
                continue;
            }
            instance.tick();
        }
    }

    public boolean areInstancesMissing() {
        return this.instancesMissing;
    }

    public MinecraftInstance getSelectedInstance() {
        for (MinecraftInstance instance : this.getInstances()) {
            if (ActiveWindowManager.isWindowActive(instance.getHwnd())) {
                return instance;
            }
        }
        return null;
    }

    public int getSize() {
        return this.instances.size();
    }

    public int getInstanceNum(MinecraftInstance instance) {
        return this.getInstanceIndex(instance) + 1;
    }

    public int getInstanceIndex(MinecraftInstance instance) {
        return this.instances.indexOf(instance);
    }

    public void renameWindows() {
        if (JultiOptions.getJultiOptions().preventWindowNaming) {
            return;
        }
        int i = 1;
        for (MinecraftInstance instance : this.instances) {
            if (!instance.isWindowMarkedMissing()) {
                User32.INSTANCE.SetWindowTextA(instance.getHwnd(), "Minecraft* - Instance " + (i++));
            }
        }
    }

    public void removeInstance(MinecraftInstance instance) {
        if (!this.instances.contains(instance)) {
            Julti.log(Level.WARN, "An instance not found in the instances list was requested to be removed! Likely a GUI desync.");
        }
        this.instances.remove(instance);
        this.saveInstances();
        ResetHelper.getManager().reload();
    }

    public void saveInstances() {
        JultiOptions.getJultiOptions().instancePaths = this.instances.stream().map(instance -> instance.getPath().toString()).collect(Collectors.toList());
    }

    public void loadInstances() {
        this.instances.clear();
        JultiOptions.getJultiOptions().instancePaths.forEach(pathString -> this.instances.add(new MinecraftInstance(Paths.get(pathString))));
    }

    public void redetectInstances() {
        Julti.log(Level.DEBUG, "Redetect running...");
        this.instances.clear();
        Julti.log(Level.DEBUG, "Cleared instances");
        this.instances.addAll(InstanceChecker.getInstanceChecker().getAllOpenedInstances());
        Julti.log(Level.DEBUG, "Added instances");
        this.instances.sort(Comparator.comparingInt(MinecraftInstance::getNameSortingNum));
        Julti.log(Level.DEBUG, "Sorted instances");
        this.saveInstances();
        Julti.log(Level.DEBUG, "Saved instances");
        this.onAllInstancesFound();
    }

    public MinecraftInstance getMatchingInstance(MinecraftInstance instance) {
        if (instance == null) {
            return null;
        }
        for (MinecraftInstance newInstance : this.instances) {
            if (newInstance.getPath().equals(instance.getPath())) {
                return newInstance;
            }
        }
        return instance;
    }
}
