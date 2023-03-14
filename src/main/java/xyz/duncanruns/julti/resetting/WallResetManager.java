package xyz.duncanruns.julti.resetting;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.AffinityManager;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.util.SleepBGUtil;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class WallResetManager extends ResetManager {
    private final List<MinecraftInstance> lockedInstances = new ArrayList<>();

    public WallResetManager(Julti julti) {
        super(julti);
    }

    @Override
    public List<ActionResult> doReset() {
        log(Level.DEBUG, "Reset key received");

        List<MinecraftInstance> instances = this.instanceManager.getInstances();
        // Return if no instances
        if (instances.size() == 0) {
            return Collections.emptyList();
        }

        log(Level.DEBUG, "There are is at least 1 instance");

        // Get selected instance, return if no selected instance
        MinecraftInstance selectedInstance = this.instanceManager.getSelectedInstance();
        if (selectedInstance == null) {
            return Collections.emptyList();
        }
        log(Level.DEBUG, "There is an instance selected");

        // if there is only a single instance, reset it and return.
        if (instances.size() == 1) {
            selectedInstance.reset(true);
            return Collections.singletonList(ActionResult.INSTANCE_RESET);
        }

        log(Level.DEBUG, "There is more than 1 instance");

        // Only place leaveInstance is used, but it is a big method
        List<ActionResult> out = this.leaveInstance(selectedInstance, instances);

        log(Level.DEBUG, "leaveInstance() ran");

        super.doReset();

        return out;
    }

    @Override
    public List<ActionResult> doBGReset() {
        MinecraftInstance selectedInstance = this.instanceManager.getSelectedInstance();
        if (selectedInstance == null) { return Collections.emptyList(); }
        List<ActionResult> out = this.resetNonLockedExcept(selectedInstance);
        if (JultiOptions.getInstance().useAffinity) {
            AffinityManager.ping(this.julti);
        }
        return out;
    }

    @Override
    public List<ActionResult> doWallFullReset() {
        log(Level.DEBUG, "Full reset key was received");

        if (!this.julti.isWallActive()) {
            log(Level.DEBUG, "Wall was not active, cancelling full reset");
            return Collections.emptyList();
        }

        List<MinecraftInstance> lockedInstances = new ArrayList<>(this.getLockedInstances());
        List<ActionResult> actionResults = new ArrayList<>();
        for (MinecraftInstance instance : this.instanceManager.getInstances()) {
            if (lockedInstances.contains(instance)) { continue; }
            if (resetInstance(instance)) { actionResults.add(ActionResult.INSTANCE_RESET); }
        }
        if (JultiOptions.getInstance().useAffinity) {
            AffinityManager.ping(this.julti);
        }
        return actionResults;
    }

    @Override
    public List<ActionResult> doWallSingleReset() {
        if (!this.julti.isWallActive()) {
            return Collections.emptyList();
        }
        MinecraftInstance selectedInstance = getHoveredWallInstance();
        if (selectedInstance == null)
            return Collections.emptyList();
        if (JultiOptions.getInstance().useAffinity) {
            AffinityManager.ping(this.julti);
        }
        return this.resetInstance(selectedInstance) ? Collections.singletonList(ActionResult.INSTANCE_RESET) : Collections.emptyList();
    }

    @Override
    public List<ActionResult> doWallLock() {
        if (!this.julti.isWallActive()) {
            return Collections.emptyList();
        }
        MinecraftInstance clickedInstance = this.getHoveredWallInstance();
        if (clickedInstance == null) { return Collections.emptyList(); }
        boolean out = this.lockInstance(clickedInstance);
        if (JultiOptions.getInstance().useAffinity) {
            AffinityManager.ping(this.julti);
        }
        return out ? Collections.singletonList(ActionResult.INSTANCE_LOCKED) : Collections.emptyList();
    }

    @Override
    public List<ActionResult> doWallFocusReset() {
        if (!this.julti.isWallActive()) {
            return Collections.emptyList();
        }

        // Regular play instance method
        MinecraftInstance clickedInstance = this.getHoveredWallInstance();
        if (clickedInstance == null) { return Collections.emptyList(); }

        List<ActionResult> actionResults = new ArrayList<>(this.playInstanceFromWall(clickedInstance));

        // Reset all others
        actionResults.addAll(this.resetNonLockedExcept(clickedInstance));
        if (JultiOptions.getInstance().useAffinity) {
            AffinityManager.ping(this.julti);
        }
        return actionResults;
    }

    protected List<ActionResult> playInstanceFromWall(MinecraftInstance instance) {
        if (JultiOptions.getInstance().wallLockInsteadOfPlay && !instance.isWorldLoaded()) {
            return this.lockInstance(instance) ? Collections.singletonList(ActionResult.INSTANCE_LOCKED) : Collections.emptyList();
        }

        instance.activate(this.instanceManager.getInstances().indexOf(instance) + 1);
        this.julti.switchScene(instance);
        this.unlockInstance(instance);
        SleepBGUtil.enableLock();
        return Collections.singletonList(ActionResult.INSTANCE_ACTIVATED);
    }

    @Override
    public List<ActionResult> doWallPlay() {
        if (!this.julti.isWallActive()) {
            return Collections.emptyList();
        }
        MinecraftInstance clickedInstance = getHoveredWallInstance();
        if (clickedInstance == null) return Collections.emptyList();
        List<ActionResult> out = playInstanceFromWall(clickedInstance);
        if (JultiOptions.getInstance().useAffinity) {
            AffinityManager.ping(this.julti);
        }
        return out;
    }

    @Override
    public List<ActionResult> doWallPlayLock() {
        if (!this.julti.isWallActive()) return Collections.emptyList();
        if (this.lockedInstances.isEmpty()) return Collections.emptyList();
        List<MinecraftInstance> instancePool = new ArrayList<>(this.lockedInstances);
        instancePool.sort((o1, o2) -> o2.getWallSortingNum() - o1.getWallSortingNum());
        List<ActionResult> out = this.playInstanceFromWall(instancePool.get(0));
        if (JultiOptions.getInstance().useAffinity) {
            AffinityManager.ping(this.julti);
        }
        return out;
    }

    @Override
    public void notifyPreviewLoaded(MinecraftInstance instance) {
        super.notifyPreviewLoaded(instance);
        JultiOptions options = JultiOptions.getInstance();
        if (options.autoResetForBeach) {
            if (!(instance.getBiome().equals("beach"))) {
                if (options.autoResetBackground || this.julti.isWallActive())
                    this.resetInstance(instance, true);
            } else {
                if ((!options.autoCheckAllOnWall) || this.julti.getInstanceManager().getSelectedInstance() == null) {
                    this.lockInstance(instance);
                }
            }
        }
    }

    @Override
    public List<MinecraftInstance> getLockedInstances() {
        return Collections.unmodifiableList(this.lockedInstances);
    }

    @Override
    public void onMissingInstancesUpdate() {
        super.onMissingInstancesUpdate();
        this.lockedInstances.clear();
    }

    public boolean resetInstance(MinecraftInstance instance) {
        return this.resetInstance(instance, false);
    }

    public boolean resetInstance(MinecraftInstance instance, boolean bypassConditions) {
        this.unlockInstance(instance);
        if (bypassConditions || this.shouldResetInstance(instance)) {
            instance.reset(this.instanceManager.getInstances().size() == 1);
            return true;
        }
        return false;
    }

    @Override
    public boolean lockInstance(MinecraftInstance instance) {
        if (!this.lockedInstances.contains(instance)) {
            this.lockedInstances.add(instance);
            // Calling super.lockInstance to do unsquish check
            super.lockInstance(instance);
            return true;
        }
        return false;
    }

    private List<ActionResult> resetNonLockedExcept(MinecraftInstance clickedInstance) {
        List<ActionResult> actionResults = new ArrayList<>();
        for (MinecraftInstance instance : this.instanceManager.getInstances()) {
            if (instance.equals(clickedInstance) || this.lockedInstances.contains(instance)) { continue; }
            if (this.resetInstance(instance)) { actionResults.add(ActionResult.INSTANCE_RESET); }
        }
        return actionResults;
    }

    public List<ActionResult> leaveInstance(MinecraftInstance selectedInstance, List<MinecraftInstance> instances) {
        JultiOptions options = JultiOptions.getInstance();

        boolean resetFirst = options.coopMode || selectedInstance.isFullscreen();

        // Reset all after playing mode
        if (options.wallResetAllAfterPlaying) {
            return this.leaveInstanceRAAPMode(instances, resetFirst);
        }

        // Get next instance
        MinecraftInstance nextInstance = this.getNextPlayableLockedInstance(options.returnToWallIfNoneLoaded);

        // Unlock instance
        this.unlockInstance(selectedInstance);

        if (resetFirst) {
            this.resetInstance(selectedInstance, true);
            sleep();
        }
        boolean nextInstanceFound = this.activateNextInstance(instances, nextInstance);
        if (!resetFirst) {
            this.resetInstance(selectedInstance, true);
        }

        // We can confidently return that an instance was reset, but not necessarily that an instance was activated.
        return nextInstanceFound ? Arrays.asList(ActionResult.INSTANCE_RESET, ActionResult.INSTANCE_ACTIVATED) : Collections.singletonList(ActionResult.INSTANCE_RESET);
    }

    private List<ActionResult> leaveInstanceRAAPMode(List<MinecraftInstance> instances, boolean resetFirst) {
        List<ActionResult> actionResults = new ArrayList<>();
        if (resetFirst) {
            instances.forEach(instance -> {
                instance.reset(instances.size() == 1);
                actionResults.add(ActionResult.INSTANCE_RESET);
            });
            sleep();
        }
        this.julti.focusWall();
        if (!resetFirst) {
            instances.forEach(instance -> {
                instance.reset(instances.size() == 1);
                actionResults.add(ActionResult.INSTANCE_RESET);
            });
        }
        // Clear out locked instances since all instances reset.
        this.lockedInstances.clear();
        return actionResults;
    }

    @Nullable
    private MinecraftInstance getNextPlayableLockedInstance(boolean onlyConsiderLoaded) {
        if (this.lockedInstances.isEmpty()) { return null; }

        // Return any loaded instances
        for (MinecraftInstance instance : this.lockedInstances) {
            if (instance.isWorldLoaded()) {
                return instance;
            }
        }

        // Return null if only considering loaded instances
        if (onlyConsiderLoaded) { return null; }

        // Just return any instance otherwise
        return this.lockedInstances.iterator().next();
    }

    private void unlockInstance(MinecraftInstance nextInstance) {
        lockedInstances.remove(nextInstance);
    }

    private static void sleep() {
        try { Thread.sleep(100); }
        catch (InterruptedException e) { throw new RuntimeException(e); }
    }

    /**
     * @param instances    Minecraft instances involved
     * @param nextInstance The next potential instance.
     * @return true if an instance was activated, otherwise false
     */
    private boolean activateNextInstance(List<MinecraftInstance> instances, @Nullable MinecraftInstance nextInstance) {
        JultiOptions options = JultiOptions.getInstance();
        if (!options.wallBypass || nextInstance == null) {
            // No more instances to play
            this.julti.focusWall();
            if (options.autoResetForBeach) {
                if (options.autoCheckAllOnWall){
                    instances.stream().filter(instance -> instance.getBiome().equals("beach")).forEach(this::lockInstance);
                }
                instances.stream().filter(instance -> !this.lockedInstances.contains(instance)).forEach(instance -> this.resetInstance(instance, true));
            }
            return false;
        } else {
            this.unlockInstance(nextInstance);
            nextInstance.activate(instances.indexOf(nextInstance) + 1);
            this.julti.switchScene(nextInstance);
            return true;
        }
    }

    private boolean shouldResetInstance(MinecraftInstance instance) {
        if (instance.isUsingWorldPreview()) {
            // Preview never started; first reset
            if (!instance.hasPreviewEverStarted()) { return true; }
        } else {
            // World never loaded, first reset
            if (!instance.hasWorldEverLoaded()) { return true; }
        }

        // World is loaded
        if (instance.isWorldLoaded()) { return true; }

        // Preview is "available" (dirt uncovered or preview loaded depending on user setting)
        if (instance.isAvailable()) {
            // Return true if cooldown has passed, otherwise return false
            return System.currentTimeMillis() - instance.getTimeLastAppeared() > JultiOptions.getInstance().wallResetCooldown;
        }
        // At this point, neither the preview nor world is loaded, which is a small space of time, if the time this is happening exceeds 5 seconds (or 20 seconds for non-wp), allow resetting in case the instance is stuck
        return System.currentTimeMillis() - instance.getLastResetPress() > (instance.isUsingWorldPreview() ? 5_000 : 20_000);
    }
}
