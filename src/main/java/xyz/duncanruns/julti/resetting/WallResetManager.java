package xyz.duncanruns.julti.resetting;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.AffinityManager;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.util.SleepBGUtil;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WallResetManager extends ResetManager {

    private final List<MinecraftInstance> lockedInstances = new ArrayList<>();

    public WallResetManager(Julti julti) {
        super(julti);
    }

    @Override
    public boolean doReset() {
        log(Level.DEBUG, "Reset key received");

        List<MinecraftInstance> instances = instanceManager.getInstances();
        // Return if no instances
        if (instances.size() == 0) {
            return false;
        }

        log(Level.DEBUG, "There are is at least 1 instance");

        // Get selected instance, return if no selected instance
        MinecraftInstance selectedInstance = instanceManager.getSelectedInstance();
        if (selectedInstance == null) {
            return false;
        }
        log(Level.DEBUG, "There is an instance selected");

        // if there is only a single instance, reset it and return.
        if (instances.size() == 1) {
            selectedInstance.reset(true);
            return true;
        }

        log(Level.DEBUG, "There is more than 1 instance");

        // Only place leaveInstance is used, but it is a big method
        leaveInstance(selectedInstance, instances);

        log(Level.DEBUG, "leaveInstance() ran");

        super.doReset();

        return true;
    }

    @Override
    public boolean doBGReset() {
        MinecraftInstance selectedInstance = instanceManager.getSelectedInstance();
        if (selectedInstance == null) return false;
        resetNonLockedExcept(selectedInstance);
        if (JultiOptions.getInstance().useAffinity) {
            AffinityManager.ping(julti);
        }
        return true;
    }

    @Override
    public boolean doWallFullReset() {
        log(Level.DEBUG, "Full reset key was received");
        if (!julti.isWallActive()) {
            log(Level.DEBUG, "Wall was not active, cancelling full reset");
            return false;
        }
        List<MinecraftInstance> lockedInstances = new ArrayList<>(getLockedInstances());
        for (MinecraftInstance instance : instanceManager.getInstances()) {
            if (lockedInstances.contains(instance)) continue;
            resetInstance(instance);
        }
        if (JultiOptions.getInstance().useAffinity) {
            AffinityManager.ping(julti);
        }
        return true;
    }

    @Override
    public boolean doWallSingleReset() {
        if (!julti.isWallActive()) {
            return false;
        }
        MinecraftInstance selectedInstance = getHoveredWallInstance();
        if (selectedInstance == null)
            return false;
        if (JultiOptions.getInstance().useAffinity) {
            AffinityManager.ping(julti);
        }
        return resetInstance(selectedInstance);
    }

    @Override
    public boolean doWallLock() {
        if (!julti.isWallActive()) {
            return false;
        }
        MinecraftInstance clickedInstance = getHoveredWallInstance();
        if (clickedInstance == null) return false;
        lockInstance(clickedInstance);
        if (JultiOptions.getInstance().useAffinity) {
            AffinityManager.ping(julti);
        }
        return true;
    }

    @Override
    public boolean doWallFocusReset() {
        if (!julti.isWallActive()) {
            return false;
        }
        // Regular play instance method
        MinecraftInstance clickedInstance = getHoveredWallInstance();
        if (clickedInstance == null) return false;
        playInstanceFromWall(clickedInstance);

        // Reset all others
        resetNonLockedExcept(clickedInstance);
        if (JultiOptions.getInstance().useAffinity) {
            AffinityManager.ping(julti);
        }
        return true;
    }

    protected void playInstanceFromWall(MinecraftInstance instance) {
        if (JultiOptions.getInstance().wallLockInsteadOfPlay && !instance.isWorldLoaded()) {
            lockInstance(instance);
            return;
        }

        instance.activate(instanceManager.getInstances().indexOf(instance) + 1);
        julti.switchScene(instance);
        unlockInstance(instance);
        SleepBGUtil.enableLock();
    }

    @Override
    public boolean doWallPlay() {
        if (!julti.isWallActive()) {
            return false;
        }
        MinecraftInstance clickedInstance = getHoveredWallInstance();
        if (clickedInstance == null) return false;
        playInstanceFromWall(clickedInstance);
        if (JultiOptions.getInstance().useAffinity) {
            AffinityManager.ping(julti);
        }
        return true;
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

    protected void resetNonLockedExcept(MinecraftInstance clickedInstance) {
        for (MinecraftInstance instance : instanceManager.getInstances()) {
            if (instance.equals(clickedInstance) || lockedInstances.contains(instance)) continue;
            resetInstance(instance);
        }
    }

    protected boolean resetInstance(MinecraftInstance instance) {
        return resetInstance(instance, false);
    }

    public void leaveInstance(MinecraftInstance selectedInstance, List<MinecraftInstance> instances) {
        JultiOptions options = JultiOptions.getInstance();

        boolean resetFirst = options.coopMode || selectedInstance.isFullscreen();

        // Reset all after playing mode
        if (options.wallResetAllAfterPlaying) {
            leaveInstanceRAAPMode(instances, resetFirst);
            return;
        }

        // Get next instance
        MinecraftInstance nextInstance = getNextPlayableLockedInstance(options.returnToWallIfNoneLoaded);

        // Unlock instance
        unlockInstance(selectedInstance);

        if (resetFirst) {
            resetInstance(selectedInstance, true);
            sleep(100);
        }
        activateNextInstance(instances, nextInstance);
        if (!resetFirst)
            resetInstance(selectedInstance, true);
    }

    private void leaveInstanceRAAPMode(List<MinecraftInstance> instances, boolean resetFirst) {
        if (resetFirst) {
            instances.forEach(instance -> instance.reset(instances.size() == 1));
            sleep(100);
        }
        julti.focusWall();
        if (!resetFirst)
            instances.forEach(instance -> instance.reset(instances.size() == 1));
        // Clear out locked instances since all instances reset.
        lockedInstances.clear();
        julti.switchToWallScene();
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

    protected boolean resetInstance(MinecraftInstance instance, boolean bypassConditions) {
        unlockInstance(instance);
        if (bypassConditions || shouldResetInstance(instance)) {
            instance.reset(instanceManager.getInstances().size() == 1);
            return true;
        }
        return false;
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
     */
    private void activateNextInstance(List<MinecraftInstance> instances, @Nullable MinecraftInstance nextInstance) {
        JultiOptions options = JultiOptions.getInstance();
        if (!options.wallBypass || nextInstance == null) {
            // No more instances to play
            julti.focusWall();
            julti.switchToWallScene();
            if (options.autoResetForBeach) {
                if (options.autoCheckAllOnWall)
                    instances.stream().filter(instance -> instance.getBiome().equals("beach")).forEach(this::lockInstance);
                instances.stream().filter(instance -> !lockedInstances.contains(instance)).forEach(instance -> resetInstance(instance, true));
            }
        } else {
            unlockInstance(nextInstance);
            nextInstance.activate(instances.indexOf(nextInstance) + 1);
            julti.switchScene(nextInstance);
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

        // Preview is loaded
        if (instance.isPreviewLoaded()) {
            // Return true if cooldown has passed, otherwise return false
            return System.currentTimeMillis() - instance.getTimeLastAppeared() > JultiOptions.getInstance().wallResetCooldown;
        }
        // At this point, neither the preview nor world is loaded, which is a small space of time, if the time this is happening exceeds 5 seconds (or 20 seconds for non-wp), allow resetting in case the instance is stuck
        return System.currentTimeMillis() - instance.getLastResetPress() > (instance.isUsingWorldPreview() ? 5_000 : 20_000);
    }

    protected void lockInstance(MinecraftInstance instance) {
        if (!lockedInstances.contains(instance))
            lockedInstances.add(instance);
    }
}
