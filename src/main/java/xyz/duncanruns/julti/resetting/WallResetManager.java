package xyz.duncanruns.julti.resetting;

import xyz.duncanruns.julti.AffinityManager;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.util.MouseUtil;
import xyz.duncanruns.julti.util.SleepBGUtil;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.List;
import java.util.*;

public class WallResetManager extends ResetManager {
    private final Set<MinecraftInstance> lockedInstances = new HashSet<>();

    public WallResetManager(Julti julti) {
        super(julti);
    }

    @Override
    public boolean doReset() {

        List<MinecraftInstance> instances = instanceManager.getInstances();
        // Return if no instances
        if (instances.size() == 0) {
            return false;
        }

        // Get selected instance, return if no selected instance
        MinecraftInstance selectedInstance = instanceManager.getSelectedInstance();
        if (selectedInstance == null) {
            return false;
        }
        // if there is only a single instance, reset it and return.
        if (instances.size() == 1) {
            selectedInstance.reset(true);
            return true;
        }

        // Only place leaveInstance is used, but it is a big method
        leaveInstance(selectedInstance, instances);

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
        if (!julti.isWallActive()) {
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

    private void playInstanceFromWall(MinecraftInstance instance) {
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
    public Set<MinecraftInstance> getLockedInstances() {
        return Collections.unmodifiableSet(lockedInstances);
    }

    @Nullable
    private MinecraftInstance getHoveredWallInstance() {
        Point point = MouseUtil.getMousePos();
        int screenX = point.x;
        int screenY = point.y;

        Rectangle bounds = julti.getWallBounds();
        if (bounds == null) return null;
        Point windowPos = bounds.getLocation();


        int x = screenX - windowPos.x;
        int y = screenY - windowPos.y;

        if (!bounds.contains(screenX, screenY)) return null;

        List<MinecraftInstance> instances = julti.getInstanceManager().getInstances();

        int totalRows;
        int totalColumns;

        JultiOptions options = JultiOptions.getInstance();
        if (!options.autoCalcWallSize) {
            totalRows = options.overrideRowsAmount;
            totalColumns = options.overrideColumnsAmount;
        } else {
            totalRows = (int) Math.ceil(Math.sqrt(instances.size()));
            totalColumns = (int) Math.ceil(instances.size() / (float) totalRows);
        }

        final int iWidth = bounds.width / totalColumns;
        final int iHeight = bounds.height / totalRows;

        int row = y / iHeight;
        int column = x / iWidth;
        int instanceIndex = row * totalColumns + column;

        if (instanceIndex >= instances.size()) return null;

        return instances.get(instanceIndex);
    }

    private void resetNonLockedExcept(MinecraftInstance clickedInstance) {
        for (MinecraftInstance instance : instanceManager.getInstances()) {
            if (instance.equals(clickedInstance) || lockedInstances.contains(instance)) continue;
            resetInstance(instance);
        }
    }

    private boolean resetInstance(MinecraftInstance instance) {
        return resetInstance(instance, false);
    }

    public void leaveInstance(MinecraftInstance selectedInstance, List<MinecraftInstance> instances) {
        JultiOptions options = JultiOptions.getInstance();

        boolean resetFirst = options.coopMode || selectedInstance.isFullscreen();

        // Reset all after playing mode
        if (options.wallResetAllAfterPlaying) {
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

    private static void sleep(long sleepTime) {
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
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

    private boolean resetInstance(MinecraftInstance instance, boolean bypassConditions) {
        unlockInstance(instance);
        if (bypassConditions || shouldResetInstance(instance)) {
            instance.reset(instanceManager.getInstances().size() == 1);
            return true;
        }
        return false;
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
        // Preview never started; first reset
        if (!instance.hasPreviewEverStarted()) return true;

        // World is loaded
        if (instance.isWorldLoaded()) return true;

        // Preview is loaded
        if (instance.isPreviewLoaded()) {
            // Return true if cooldown has passed, otherwise return false
            return System.currentTimeMillis() - instance.getLastPreviewStart() > JultiOptions.getInstance().wallResetCooldown;
        }
        // At this point, neither the preview nor world is loaded, which is a small space of time, if the time this is happening exceeds 5 seconds, allow resetting in case the instance is stuck
        return System.currentTimeMillis() - instance.getLastResetPress() > 5000;
    }

    private void lockInstance(MinecraftInstance instance) {
        lockedInstances.add(instance);
    }
}
