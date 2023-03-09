package xyz.duncanruns.julti.resetting;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.AffinityManager;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.util.SleepBGUtil;

import javax.annotation.Nullable;
import java.util.*;

public class WallResetManager extends ResetManager {

    private final List<MinecraftInstance> lockedInstances = new ArrayList<>();

    public WallResetManager(Julti julti) {
        super(julti);
    }

    @Override
    public List<ActionResult> doReset() {
        log(Level.DEBUG, "Reset key received");

        List<MinecraftInstance> instances = instanceManager.getInstances();
        // Return if no instances
        if (instances.size() == 0) {
            return Collections.emptyList();
        }

        log(Level.DEBUG, "There are is at least 1 instance");

        // Get selected instance, return if no selected instance
        MinecraftInstance selectedInstance = instanceManager.getSelectedInstance();
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
        List<ActionResult> out = leaveInstance(selectedInstance, instances);

        log(Level.DEBUG, "leaveInstance() ran");

        super.doReset();

        return out;
    }

    @Override
    public List<ActionResult> doBGReset() {
        MinecraftInstance selectedInstance = instanceManager.getSelectedInstance();
        if (selectedInstance == null) return Collections.emptyList();
        List<ActionResult> out = resetNonLockedExcept(selectedInstance);
        if (JultiOptions.getInstance().useAffinity) {
            AffinityManager.ping(julti);
        }
        return out;
    }

    @Override
    public List<ActionResult> doWallFullReset() {
        log(Level.DEBUG, "Full reset key was received");

        if (!julti.isWallActive()) {
            log(Level.DEBUG, "Wall was not active, cancelling full reset");
            return Collections.emptyList();
        }
        List<MinecraftInstance> lockedInstances = new ArrayList<>(getLockedInstances());
        List<ActionResult> actionResults = new ArrayList<>();
        for (MinecraftInstance instance : instanceManager.getInstances()) {
            if (lockedInstances.contains(instance)) continue;
            if (resetInstance(instance)) actionResults.add(ActionResult.INSTANCE_RESET);
        }
        if (JultiOptions.getInstance().useAffinity) {
            AffinityManager.ping(julti);
        }
        return actionResults;
    }

    @Override
    public List<ActionResult> doWallSingleReset() {
        if (!julti.isWallActive()) {
            return Collections.emptyList();
        }
        MinecraftInstance selectedInstance = getHoveredWallInstance();
        if (selectedInstance == null)
            return Collections.emptyList();
        if (JultiOptions.getInstance().useAffinity) {
            AffinityManager.ping(julti);
        }
        return resetInstance(selectedInstance) ? Collections.singletonList(ActionResult.INSTANCE_RESET) : Collections.emptyList();
    }

    @Override
    public List<ActionResult> doWallLock() {
        if (!julti.isWallActive()) {
            return Collections.emptyList();
        }
        MinecraftInstance clickedInstance = getHoveredWallInstance();
        if (clickedInstance == null) return Collections.emptyList();
        boolean out = lockInstance(clickedInstance);
        if (JultiOptions.getInstance().useAffinity) {
            AffinityManager.ping(julti);
        }
        return out ? Collections.singletonList(ActionResult.INSTANCE_LOCKED) : Collections.emptyList();
    }

    @Override
    public List<ActionResult> doWallFocusReset() {
        if (!julti.isWallActive()) {
            return Collections.emptyList();
        }

        // Regular play instance method
        MinecraftInstance clickedInstance = getHoveredWallInstance();
        if (clickedInstance == null) return Collections.emptyList();

        List<ActionResult> actionResults = new ArrayList<>(playInstanceFromWall(clickedInstance));

        // Reset all others
        actionResults.addAll(resetNonLockedExcept(clickedInstance));
        if (JultiOptions.getInstance().useAffinity) {
            AffinityManager.ping(julti);
        }
        return actionResults;
    }

    protected List<ActionResult> playInstanceFromWall(MinecraftInstance instance) {
        if (JultiOptions.getInstance().wallLockInsteadOfPlay && !instance.isWorldLoaded()) {
            return lockInstance(instance) ? Collections.singletonList(ActionResult.INSTANCE_LOCKED) : Collections.emptyList();
        }

        instance.activate(instanceManager.getInstances().indexOf(instance) + 1);
        julti.switchScene(instance);
        unlockInstance(instance);
        SleepBGUtil.enableLock();
        return Collections.singletonList(ActionResult.INSTANCE_ACTIVATED);
    }

    @Override
    public List<ActionResult> doWallPlay() {
        if (!julti.isWallActive()) {
            return Collections.emptyList();
        }
        MinecraftInstance clickedInstance = getHoveredWallInstance();
        if (clickedInstance == null) return Collections.emptyList();
        List<ActionResult> out = playInstanceFromWall(clickedInstance);
        if (JultiOptions.getInstance().useAffinity) {
            AffinityManager.ping(julti);
        }
        return out;
    }

    @Override
    public List<ActionResult> doWallPlayLock() {
        if (!julti.isWallActive()) return Collections.emptyList();
        if (lockedInstances.isEmpty()) return Collections.emptyList();
        Optional<MinecraftInstance> firstLockedInstanceO = lockedInstances.stream().filter(MinecraftInstance::isWorldLoaded).findAny();
        if (!firstLockedInstanceO.isPresent()) return Collections.emptyList();
        MinecraftInstance firstLockedInstance = firstLockedInstanceO.get();
        List<ActionResult> out = playInstanceFromWall(firstLockedInstance);
        if (JultiOptions.getInstance().useAffinity) {
            AffinityManager.ping(julti);
        }
        return out;
    }

    @Override
    public void notifyPreviewLoaded(MinecraftInstance instance) {
        super.notifyPreviewLoaded(instance);
        JultiOptions options = JultiOptions.getInstance();
        if (options.autoResetForBeach) {
            if (!(instance.getBiome().equals("beach"))) {
                if (options.autoResetBackground || julti.isWallActive())
                    resetInstance(instance, true);
            } else {
                if ((!options.autoCheckAllOnWall) || julti.getInstanceManager().getSelectedInstance() == null) {
                    lockInstance(instance);
                }
            }
        }
    }

    @Override
    public List<MinecraftInstance> getLockedInstances() {
        return Collections.unmodifiableList(lockedInstances);
    }

    @Override
    public void onMissingInstancesUpdate() {
        super.onMissingInstancesUpdate();
        lockedInstances.clear();
    }

    public boolean resetInstance(MinecraftInstance instance) {
        return resetInstance(instance, false);
    }

    public boolean resetInstance(MinecraftInstance instance, boolean bypassConditions) {
        unlockInstance(instance);
        if (bypassConditions || shouldResetInstance(instance)) {
            instance.reset(instanceManager.getInstances().size() == 1);
            return true;
        }
        return false;
    }

    @Override
    public boolean lockInstance(MinecraftInstance instance) {
        if (!lockedInstances.contains(instance)) {
            lockedInstances.add(instance);
            // Calling super.lockInstance to do unsquish check
            super.lockInstance(instance);
            return true;
        }
        return false;
    }

    private List<ActionResult> resetNonLockedExcept(MinecraftInstance clickedInstance) {
        List<ActionResult> actionResults = new ArrayList<>();
        for (MinecraftInstance instance : instanceManager.getInstances()) {
            if (instance.equals(clickedInstance) || lockedInstances.contains(instance)) continue;
            if (resetInstance(instance)) actionResults.add(ActionResult.INSTANCE_RESET);
        }
        return actionResults;
    }

    public List<ActionResult> leaveInstance(MinecraftInstance selectedInstance, List<MinecraftInstance> instances) {
        JultiOptions options = JultiOptions.getInstance();

        boolean resetFirst = options.coopMode || selectedInstance.isFullscreen();

        // Reset all after playing mode
        if (options.wallResetAllAfterPlaying) {
            return leaveInstanceRAAPMode(instances, resetFirst);
        }

        // Get next instance
        MinecraftInstance nextInstance = getNextPlayableLockedInstance(options.returnToWallIfNoneLoaded);

        // Unlock instance
        unlockInstance(selectedInstance);

        if (resetFirst) {
            resetInstance(selectedInstance, true);
            sleep(100);
        }
        boolean nextInstanceFound = activateNextInstance(instances, nextInstance);
        if (!resetFirst)
            resetInstance(selectedInstance, true);

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
            sleep(100);
        }
        julti.focusWall();
        if (!resetFirst) {
            instances.forEach(instance -> {
                instance.reset(instances.size() == 1);
                actionResults.add(ActionResult.INSTANCE_RESET);
            });
        }
        // Clear out locked instances since all instances reset.
        lockedInstances.clear();
        return actionResults;
    }

    @Nullable
    private MinecraftInstance getNextPlayableLockedInstance(boolean onlyConsiderLoaded) {
        // If empty return null
        if (lockedInstances.isEmpty()) return null;

        // Return any loaded instances
        for (MinecraftInstance instance : lockedInstances) {
            if (instance.isWorldLoaded()) return instance;
        }

        // Return null if only considering loaded instances
        if (onlyConsiderLoaded)
            return null;

        // Just return any instance otherwise
        return lockedInstances.iterator().next();
    }

    private void unlockInstance(MinecraftInstance nextInstance) {
        lockedInstances.remove(nextInstance);
    }

    private static void sleep(long sleepTime) {
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
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
            julti.focusWall();
            if (options.autoResetForBeach) {
                if (options.autoCheckAllOnWall)
                    instances.stream().filter(instance -> instance.getBiome().equals("beach")).forEach(this::lockInstance);
                instances.stream().filter(instance -> !lockedInstances.contains(instance)).forEach(instance -> resetInstance(instance, true));
            }
            return false;
        } else {
            unlockInstance(nextInstance);
            nextInstance.activate(instances.indexOf(nextInstance) + 1);
            julti.switchScene(nextInstance);
            return true;
        }
    }

    private boolean shouldResetInstance(MinecraftInstance instance) {
        if (instance.isUsingWorldPreview()) {
            // Preview never started; first reset
            if (!instance.hasPreviewEverStarted()) return true;
        } else {
            // World never loaded, first reset
            if (!instance.hasWorldEverLoaded()) return true;
        }

        // World is loaded
        if (instance.isWorldLoaded()) return true;

        // Preview is "available" (dirt uncovered or preview loaded depending on user setting)
        if (instance.isAvailable()) {
            // Return true if cooldown has passed, otherwise return false
            return System.currentTimeMillis() - instance.getTimeLastAppeared() > JultiOptions.getInstance().wallResetCooldown;
        }
        // At this point, neither the preview nor world is loaded, which is a small space of time, if the time this is happening exceeds 5 seconds (or 20 seconds for non-wp), allow resetting in case the instance is stuck
        return System.currentTimeMillis() - instance.getLastResetPress() > (instance.isUsingWorldPreview() ? 5_000 : 20_000);
    }
}
