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

        // Get selected instance and next instance, return if no selected instance,
        // if there is only a single instance, reset it and return.
        MinecraftInstance selectedInstance = instanceManager.getSelectedInstance();
        if (selectedInstance == null) {
            return false;
        }
        if (instances.size() == 1) {
            selectedInstance.reset(true);
            return true;
        }

        onLeaveInstance(selectedInstance, instances);

        super.doReset();

        return true;
    }

    public void onLeaveInstance(MinecraftInstance selectedInstance, List<MinecraftInstance> instances) {
        // If using One At A Time, just reset all instances
        if (JultiOptions.getInstance().wallOneAtATime) {
            julti.focusWall();
            instances.forEach(MinecraftInstance::reset);
            // Clear out locked instances since all instances reset.
            lockedInstances.clear();
            julti.switchToWallScene();
            return;
        }

        resetInstance(selectedInstance);
        lockedInstances.remove(selectedInstance);
        if (lockedInstances.isEmpty()) {
            julti.focusWall();
            julti.switchToWallScene();
        } else {
            MinecraftInstance nextInstance = lockedInstances.iterator().next();
            lockedInstances.remove(nextInstance);
            nextInstance.activate();
            julti.switchScene(nextInstance);
        }
    }

    private boolean resetInstance(MinecraftInstance instance) {
        lockedInstances.remove(instance);
        if (!instance.hasPreviewEverStarted() || instance.isWorldLoaded() || (instance.isPreviewLoaded() && System.currentTimeMillis() - instance.getLastPreviewStart() > JultiOptions.getInstance().wallResetCooldown)) {
            instance.reset();
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
            resetInstance(instance);
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
        return resetInstance(selectedInstance);
    }

    @Override
    public boolean doWallLock() {
        if (!julti.isWallActive()) {
            return false;
        }
        MinecraftInstance clickedInstance = getHoveredWallInstance();
        if (clickedInstance == null) return false;
        lockedInstances.add(clickedInstance);
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
        julti.switchScene(clickedInstance);
        lockedInstances.remove(clickedInstance);
        return true;
    }

    @Override
    public Set<MinecraftInstance> getLockedInstances() {
        return Collections.unmodifiableSet(lockedInstances);
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

        int totalRows = getTotalRows(instances);
        final int totalColumns = (int) Math.ceil(instances.size() / (float) totalRows);

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

    private static int getTotalRows(List<MinecraftInstance> instances) {
        JultiOptions options = JultiOptions.getInstance();

        if (options.overrideRows) {
            return options.overrideRowsAmount;
        }

        return (int) Math.ceil(Math.sqrt(instances.size()));
    }
}
