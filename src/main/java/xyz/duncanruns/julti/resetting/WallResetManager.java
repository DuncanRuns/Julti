package xyz.duncanruns.julti.resetting;

import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.affinity.AffinityManager;
import xyz.duncanruns.julti.instance.InstanceState;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.management.ActiveWindowManager;
import xyz.duncanruns.julti.management.InstanceManager;
import xyz.duncanruns.julti.util.DoAllFastUtil;
import xyz.duncanruns.julti.util.SleepBGUtil;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.List;
import java.util.*;

import static xyz.duncanruns.julti.util.SleepUtil.sleep;

public class WallResetManager extends ResetManager {
    private static final WallResetManager INSTANCE = new WallResetManager();

    private final List<MinecraftInstance> lockedInstances = new ArrayList<>();

    private MinecraftInstance lastAttemptedJoin = null;
    private long lastAttemptedJoinTime = 0L;

    public static WallResetManager getWallResetManager() {
        return INSTANCE;
    }

    @Override
    public List<ActionResult> doReset() {

        List<MinecraftInstance> instances = InstanceManager.getInstanceManager().getInstances();
        // Return if no instances
        if (instances.isEmpty()) {
            return Collections.emptyList();
        }

        // Get selected instance, return if no selected instance
        MinecraftInstance selectedInstance = InstanceManager.getInstanceManager().getSelectedInstance();
        if (selectedInstance == null) {
            return Collections.emptyList();
        }

        // if there is only a single instance, reset it and return.
        if (instances.size() == 1) {
            selectedInstance.reset();
            return Collections.singletonList(ActionResult.INSTANCE_RESET);
        }

        // Only place leaveInstance is used, but it is a big method
        List<ActionResult> out = this.leaveInstance(selectedInstance, instances);

        super.doReset();

        return out;
    }

    @Override
    public List<ActionResult> doBGReset() {
        MinecraftInstance selectedInstance = InstanceManager.getInstanceManager().getSelectedInstance();
        if (selectedInstance == null) {
            return Collections.emptyList();
        }
        List<ActionResult> out = this.resetNonLockedExcept(selectedInstance);
        if (JultiOptions.getJultiOptions().useAffinity) {
            AffinityManager.ping();
        }
        return out;
    }

    @Override
    public List<ActionResult> doWallFullReset() {
        if (!ActiveWindowManager.isWallActive()) {
            return Collections.emptyList();
        }
        List<MinecraftInstance> lockedInstances = new ArrayList<>(this.getLockedInstances());
        List<ActionResult> actionResults = new ArrayList<>();
        DoAllFastUtil.doAllFast(instance -> {
            if (lockedInstances.contains(instance)) {
                return;//(continue;)
            }
            if (this.resetInstance(instance)) {
                synchronized (actionResults) {
                    actionResults.add(ActionResult.INSTANCE_RESET);
                }
            }
        });

        if (JultiOptions.getJultiOptions().useAffinity) {
            AffinityManager.ping();
        }
        return actionResults;
    }

    @Override
    public List<ActionResult> doWallSingleReset(Point mousePosition) {
        if (!ActiveWindowManager.isWallActive()) {
            return Collections.emptyList();
        }
        MinecraftInstance selectedInstance = this.getHoveredWallInstance(mousePosition);
        if (selectedInstance == null) {
            return Collections.emptyList();
        }
        if (JultiOptions.getJultiOptions().useAffinity) {
            AffinityManager.ping();
        }
        return this.resetInstance(selectedInstance) ? Collections.singletonList(ActionResult.INSTANCE_RESET) : Collections.emptyList();
    }

    @Override
    public List<ActionResult> doWallLock(Point mousePosition) {
        if (!ActiveWindowManager.isWallActive()) {
            return Collections.emptyList();
        }
        MinecraftInstance clickedInstance = this.getHoveredWallInstance(mousePosition);
        if (clickedInstance == null) {
            return Collections.emptyList();
        }
        boolean out = this.lockInstance(clickedInstance);
        if (JultiOptions.getJultiOptions().useAffinity) {
            AffinityManager.ping();
        }
        return out ? Collections.singletonList(ActionResult.INSTANCE_LOCKED) : Collections.emptyList();
    }

    @Override
    public List<ActionResult> doWallFocusReset(Point mousePosition) {
        if (!ActiveWindowManager.isWallActive()) {
            return Collections.emptyList();
        }

        // Regular play instance method
        MinecraftInstance clickedInstance = this.getHoveredWallInstance(mousePosition);
        if (clickedInstance == null) {
            return Collections.emptyList();
        }

        // Get list of instances to reset
        List<MinecraftInstance> toReset = new ArrayList<>(InstanceManager.getInstanceManager().getInstances());
        toReset.removeAll(this.lockedInstances);
        toReset.remove(clickedInstance);

        List<ActionResult> actionResults = new ArrayList<>(this.playInstanceFromWall(clickedInstance, false));

        // Reset all others
        DoAllFastUtil.doAllFast(toReset, instance -> {
            if (this.resetInstance(instance)) {
                synchronized (actionResults) {
                    actionResults.add(ActionResult.INSTANCE_RESET);
                }
            }
        });

        if (JultiOptions.getJultiOptions().useAffinity) {
            AffinityManager.ping();
        }
        return actionResults;
    }

    @Override
    public List<ActionResult> doWallPlay(Point mousePosition) {
        if (!ActiveWindowManager.isWallActive()) {
            return Collections.emptyList();
        }
        MinecraftInstance clickedInstance = this.getHoveredWallInstance(mousePosition);
        if (clickedInstance == null) {
            return Collections.emptyList();
        }


        boolean forceEnter = false;
        long currentTime = System.currentTimeMillis();
        if (Objects.equals(this.lastAttemptedJoin, clickedInstance) && currentTime - this.lastAttemptedJoinTime < 350) {
            forceEnter = true;
        }
        this.lastAttemptedJoin = clickedInstance;
        this.lastAttemptedJoinTime = currentTime;

        List<ActionResult> out = this.playInstanceFromWall(clickedInstance, forceEnter);
        if (JultiOptions.getJultiOptions().useAffinity) {
            AffinityManager.ping();
        }
        return out;
    }

    @Override
    public List<ActionResult> doWallPlayLock(Point mousePosition) {
        if (!ActiveWindowManager.isWallActive()) {
            return Collections.emptyList();
        }
        if (this.lockedInstances.isEmpty()) {
            return Collections.emptyList();
        }
        List<MinecraftInstance> instancePool = new ArrayList<>(this.lockedInstances);
        instancePool.sort((o1, o2) -> o2.getResetSortingNum() - o1.getResetSortingNum());
        List<ActionResult> out = this.playInstanceFromWall(instancePool.get(0), false);
        if (JultiOptions.getJultiOptions().useAffinity) {
            AffinityManager.ping();
        }
        return out;
    }

    @Override
    public void notifyPreviewLoaded(MinecraftInstance instance) {
        super.notifyPreviewLoaded(instance);
        JultiOptions options = JultiOptions.getJultiOptions();
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
        if (bypassConditions || instance.isResettable()) {
            instance.reset();
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

    protected List<ActionResult> playInstanceFromWall(MinecraftInstance instance, boolean bypassLoadCheck) {
        JultiOptions options = JultiOptions.getJultiOptions();

        if (!bypassLoadCheck && options.wallLockInsteadOfPlay && !(instance.getStateTracker().isCurrentState(InstanceState.INWORLD))) {
            List<ActionResult> results = new ArrayList<>(this.lockInstance(instance) ? Collections.singletonList(ActionResult.INSTANCE_LOCKED) : Collections.emptyList());

            if (options.wallSmartSwitch) {
                // ! This code is on the edge of dangerous !
                // This can cause infinite recursion if messed with badly
                // This point of the code can only be accessed if the instance parameter has not loaded in generation
                // Take note that the instance passed into playInstanceFromWall must be a loaded instance!
                MinecraftInstance ssInstance = this.getNextPlayableLockedInstance(true);
                if (ssInstance != null) {
                    results.addAll(this.playInstanceFromWall(ssInstance, false));
                }
            }
            return results;

        }

        Julti.getJulti().activateInstance(instance);
        SleepBGUtil.enableLock();
        return Collections.singletonList(ActionResult.INSTANCE_ACTIVATED);
    }

    private List<ActionResult> resetNonLockedExcept(MinecraftInstance clickedInstance) {
        List<ActionResult> actionResults = new ArrayList<>();
        DoAllFastUtil.doAllFast(instance -> {
            if (instance.equals(clickedInstance) || this.lockedInstances.contains(instance)) {
                return;//(continue;)
            }
            if (this.resetInstance(instance)) {
                synchronized (actionResults) {
                    actionResults.add(ActionResult.INSTANCE_RESET);
                }
            }
        });

        return actionResults;
    }

    public List<ActionResult> leaveInstance(MinecraftInstance selectedInstance, List<MinecraftInstance> instances) {
        JultiOptions options = JultiOptions.getJultiOptions();

        boolean resetFirst = options.coopMode && options.wallBypass;

        selectedInstance.ensureNotFullscreen();

        // Unlock instance
        this.unlockInstance(selectedInstance);

        // Get next instance
        MinecraftInstance nextInstance = this.getNextPlayableLockedInstance(options.returnToWallIfNoneLoaded);

        if (resetFirst) {
            this.resetInstance(selectedInstance, true);
            sleep(100);
        }
        boolean nextInstanceFound = this.activateNextInstance(instances, nextInstance);
        if (!resetFirst) {
            this.resetInstance(selectedInstance, true);
        }

        // We can confidently return that an instance was reset, but not necessarily that an instance was activated.
        return nextInstanceFound ? Arrays.asList(ActionResult.INSTANCE_RESET, ActionResult.INSTANCE_ACTIVATED) : Collections.singletonList(ActionResult.INSTANCE_RESET);
    }

    @Nullable
    private MinecraftInstance getNextPlayableLockedInstance(boolean onlyConsiderLoaded) {
        // If empty return null
        if (this.lockedInstances.isEmpty()) {
            return null;
        }

        // Return any loaded instances
        for (MinecraftInstance instance : this.lockedInstances) {
            if (instance.getStateTracker().isCurrentState(InstanceState.INWORLD)) {
                return instance;
            }
        }

        // Return null if only considering loaded instances
        if (onlyConsiderLoaded) {
            return null;
        }

        // Just return any instance otherwise
        return this.lockedInstances.iterator().next();
    }

    private void unlockInstance(MinecraftInstance nextInstance) {
        this.lockedInstances.remove(nextInstance);
    }

    /**
     * @param instances    Minecraft instances involved
     * @param nextInstance The next potential instance.
     *
     * @return true if an instance was activated, otherwise false
     */
    private boolean activateNextInstance(List<MinecraftInstance> instances, @Nullable MinecraftInstance nextInstance) {
        JultiOptions options = JultiOptions.getJultiOptions();
        if (!options.wallBypass || nextInstance == null) {
            // No more instances to play
            Julti.getJulti().focusWall();
            return false;
        } else {
            this.unlockInstance(nextInstance);
            // activate projector - avoid previous instances behind selected instance when using thin BT, etc.
            if (options.activateProjectorOnReset) {
                Julti.getJulti().focusWall(false);
            }
            Julti.getJulti().activateInstance(nextInstance);
            return true;
        }
    }
}
