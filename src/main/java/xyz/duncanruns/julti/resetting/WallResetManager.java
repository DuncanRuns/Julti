package xyz.duncanruns.julti.resetting;

import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.util.AffinityUtil;

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

        // Get selected instance and next instance, return if no selected instance,
        // if there is only a single instance, reset it and return.
        MinecraftInstance selectedInstance = instanceManager.getSelectedInstance();
        if (selectedInstance == null) {
            return false;
        }
        if (instances.size() == 1) {
            selectedInstance.reset(true);
            if (JultiOptions.getInstance().useAffinity)
                AffinityUtil.setAffinity(selectedInstance, AffinityUtil.playBitMask);
            return true;
        }

        onLeaveInstance(selectedInstance, instances);

        super.doReset();

        return true;
    }

    public void onLeaveInstance(MinecraftInstance selectedInstance, List<MinecraftInstance> instances) {
        // If using One At A Time, just reset all instances
        JultiOptions options = JultiOptions.getInstance();
        if (options.useAffinity)
            AffinityUtil.setAffinities(instances, lockedInstances);
        if (options.wallResetAllAfterPlaying) {
            julti.focusWall();
            instances.forEach(instance -> instance.reset(instances.size() == 1));
            // Clear out locked instances since all instances reset.
            lockedInstances.clear();
            if (options.useAffinity)
                AffinityUtil.setAffinities(instances, lockedInstances);
            julti.switchToWallScene();
            return;
        }

        resetInstance(selectedInstance, lockedInstances.isEmpty());
        lockedInstances.remove(selectedInstance);
        if (!options.wallBypass || lockedInstances.isEmpty()) {
            julti.focusWall();
            julti.switchToWallScene();
            if (options.useAffinity)
                AffinityUtil.setAffinities(instances, lockedInstances);
        } else {
            MinecraftInstance nextInstance = lockedInstances.iterator().next();
            lockedInstances.remove(nextInstance);
            nextInstance.activate();
            AffinityUtil.setPlayingAffinities(nextInstance, instances);
            julti.switchScene(nextInstance);
        }
    }

    private boolean resetInstance(MinecraftInstance instance, boolean willBeOnWall) {
        lockedInstances.remove(instance);
        if (!instance.hasPreviewEverStarted() || instance.isWorldLoaded() || (instance.isPreviewLoaded() && System.currentTimeMillis() - instance.getLastPreviewStart() > JultiOptions.getInstance().wallResetCooldown)) {
            instance.reset(instanceManager.getInstances().size() == 1);
            if (JultiOptions.getInstance().useAffinity) {
                if (willBeOnWall) AffinityUtil.setAffinity(instance, AffinityUtil.highBitMask);
                else AffinityUtil.setAffinity(instance, AffinityUtil.lowBitMask);
            }
            return true;
        }
        return false;
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
        lockedInstances.add(clickedInstance);
        if (JultiOptions.getInstance().useAffinity)
            AffinityUtil.setAffinity(clickedInstance, AffinityUtil.lockBitMask);
        return true;
    }

    @Override
    public boolean doWallPlay() {
        if (!julti.isWallActive()) {
            return false;
        }
        MinecraftInstance clickedInstance = getHoveredWallInstance();
        if (clickedInstance == null) return false;
        clickedInstance.activate();
        AffinityUtil.setPlayingAffinities(clickedInstance, instanceManager.getInstances());
        julti.switchScene(clickedInstance);
        lockedInstances.remove(clickedInstance);
        return true;
    }

    @Override
    public Set<MinecraftInstance> getLockedInstances() {
        return Collections.unmodifiableSet(lockedInstances);
    }

    @Override
    public void notifyPreviewLoaded(final MinecraftInstance instance) {
        if (JultiOptions.getInstance().useAffinity)
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    if (instanceManager.getSelectedInstance() != null)
                        AffinityUtil.setAffinity(instance, AffinityUtil.lowBitMask);
                    else AffinityUtil.setAffinity(instance, AffinityUtil.midBitMask);
                }
            }, 400);
    }

    @Override
    public void notifyWorldLoaded(final MinecraftInstance instance) {
        if (JultiOptions.getInstance().useAffinity)
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    MinecraftInstance selectedInstance = instanceManager.getSelectedInstance();
                    if (instance.equals(selectedInstance))
                        AffinityUtil.setAffinity(instance, AffinityUtil.playBitMask);
                    else if (selectedInstance != null)
                        AffinityUtil.setAffinity(instance, AffinityUtil.superLowBitMask);
                    else if (lockedInstances.contains(instance))
                        AffinityUtil.setAffinity(instance, AffinityUtil.lockBitMask);
                    else
                        AffinityUtil.setAffinity(instance, AffinityUtil.lowBitMask);
                }
            }, 400);
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
}
