package xyz.duncanruns.julti.resetting;

import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.instance.MinecraftInstance;

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

        onLeaveInstance(selectedInstance, instances);

        super.doReset();

        return true;
    }

    @Override
    public boolean doBGReset() {
        MinecraftInstance selectedInstance = instanceManager.getSelectedInstance();
        if (selectedInstance == null) return false;
        resetNonLockedExcept(selectedInstance);
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
            resetInstance(instance, true);
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
        return resetInstance(selectedInstance, true);
    }

    @Override
    public boolean doWallLock() {
        if (!julti.isWallActive()) {
            return false;
        }
        MinecraftInstance clickedInstance = getHoveredWallInstance();
        if (clickedInstance == null) return false;
        lockInstance(clickedInstance);
        return true;
    }

    private void lockInstance(MinecraftInstance instance) {
        lockedInstances.add(instance);
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
        return true;
    }

    private void playInstanceFromWall(MinecraftInstance instance) {
        if (JultiOptions.getInstance().wallLockInsteadOfPlay && !instance.isWorldLoaded()) {
            lockInstance(instance);
            return;
        }

        instance.activate(instanceManager.getInstances().indexOf(instance) + 1);
        julti.switchScene(instance);
        lockedInstances.remove(instance);
    }

    @Override
    public boolean doWallPlay() {
        if (!julti.isWallActive()) {
            return false;
        }
        MinecraftInstance clickedInstance = getHoveredWallInstance();
        if (clickedInstance == null) return false;
        playInstanceFromWall(clickedInstance);
        return true;
    }

    @Override
    public Set<MinecraftInstance> getLockedInstances() {
        return Collections.unmodifiableSet(lockedInstances);
    }

    @Override
    public void notifyPreviewLoaded(final MinecraftInstance instance) {
    }

    @Override
    public void notifyWorldLoaded(final MinecraftInstance instance) {
    }

    @Nullable
    private MinecraftInstance getHoveredWallInstance() {
        Point point = MouseInfo.getPointerInfo().getLocation();
        int screenX = point.x;
        int screenY = point.y;

        Rectangle bounds = getBounds();
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

    private Rectangle getBounds() {
        return julti.getWallBounds();
    }

    private void resetNonLockedExcept(MinecraftInstance clickedInstance) {
        for (MinecraftInstance instance : instanceManager.getInstances()) {
            if (instance.equals(clickedInstance) || lockedInstances.contains(instance)) continue;
            resetInstance(instance, false);
        }
    }

    public void onLeaveInstance(MinecraftInstance selectedInstance, List<MinecraftInstance> instances) {
        JultiOptions options = JultiOptions.getInstance();

        // Reset all after playing mode
        if (options.wallResetAllAfterPlaying) {
            julti.focusWall();
            instances.forEach(instance -> instance.reset(instances.size() == 1));
            // Clear out locked instances since all instances reset.
            lockedInstances.clear();
            julti.switchToWallScene();
            return;
        }

        // Reset and unlock
        resetInstance(selectedInstance, lockedInstances.isEmpty());
        lockedInstances.remove(selectedInstance);


        MinecraftInstance nextInstance = getNextPlayableLockedInstance(options.returnToWallIfNoneLoaded);
        if (!options.wallBypass || nextInstance == null) {
            // No more instances to play
            julti.focusWall();
            julti.switchToWallScene();
        } else {
            lockedInstances.remove(nextInstance);
            nextInstance.activate(instances.indexOf(nextInstance) + 1);
            julti.switchScene(nextInstance);
        }
    }

    private boolean resetInstance(MinecraftInstance instance, boolean willBeOnWall) {
        lockedInstances.remove(instance);
        if (!instance.hasPreviewEverStarted() || instance.isWorldLoaded() || (instance.isPreviewLoaded() && System.currentTimeMillis() - instance.getLastPreviewStart() > JultiOptions.getInstance().wallResetCooldown)) {
            instance.reset(instanceManager.getInstances().size() == 1);
            return true;
        }
        return false;
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
}
