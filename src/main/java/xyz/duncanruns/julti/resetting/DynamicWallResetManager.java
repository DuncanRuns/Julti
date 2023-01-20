package xyz.duncanruns.julti.resetting;

import xyz.duncanruns.julti.AffinityManager;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.instance.MinecraftInstance;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DynamicWallResetManager extends WallResetManager {

    private List<MinecraftInstance> displayInstances = new ArrayList<>();
    private boolean isFirstReset = true;

    public DynamicWallResetManager(Julti julti) {
        super(julti);
        refreshDisplayInstances();
    }

    public void refreshDisplayInstances() {
        JultiOptions options = JultiOptions.getInstance();

        int totalRows = 2;
        int totalColumns = 2;
        if (!options.autoCalcWallSize) {
            totalRows = options.overrideRowsAmount;
            totalColumns = options.overrideColumnsAmount;
        }

        int numToDisplay = totalColumns * totalRows;

        if (displayInstances.size() != numToDisplay) {
            displayInstances = new ArrayList<>();
            for (int i = 0; i < numToDisplay; i++) displayInstances.add(null);
        }

        for (int i = 0; i < numToDisplay; i++) {
            MinecraftInstance instance = displayInstances.get(i);
            if (instance != null && ((!displayInstances.get(i).hasWindowQuick()) || getLockedInstances().contains(displayInstances.get(i)))) {
                displayInstances.set(i, null);
            }
        }

        if (!displayInstances.contains(null)) return;

        List<MinecraftInstance> instancePool = new ArrayList<>(instanceManager.getInstances());
        instancePool.removeIf(instance -> getLockedInstances().contains(instance));
        instancePool.removeIf(instance -> displayInstances.contains(instance));
        instancePool.sort((o1, o2) -> o2.getWallSortingNum() - o1.getWallSortingNum());

        while (displayInstances.contains(null)) {
            if (instancePool.isEmpty()) break;
            MinecraftInstance removed = instancePool.remove(0);
            removed.updateTimeLastAppeared();
            displayInstances.set(displayInstances.indexOf(null), removed);
        }
    }

    @Override
    public boolean doWallFullReset() {
        if (isFirstReset && super.doWallFullReset()) {
            isFirstReset = false;
            return true;
        }
        if (!julti.isWallActive()) {
            return false;
        }
        // instead of using the displayInstances, we use "all instances that are also found in displayInstances", which
        // uses an instance's equals() method to match instance paths so that if displayInstances has an abandoned
        // instance object, it can still be used.
        List<MinecraftInstance> resettable = instanceManager.getInstances().stream().filter(instance -> displayInstances.contains(instance)).collect(Collectors.toList());
        resettable.forEach(this::resetNoWallUpdate);
        if (JultiOptions.getInstance().useAffinity) {
            AffinityManager.ping(julti);
        }
        Collections.fill(displayInstances, null);
        refreshDisplayInstances();
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
        for (MinecraftInstance instance : instanceManager.getInstances()) {
            if (getLockedInstances().contains(instance) || (!displayInstances.contains(instance))) continue;
            if (!instance.equals(clickedInstance)) {
                resetInstance(instance);
            }
        }
        if (JultiOptions.getInstance().useAffinity) {
            AffinityManager.ping(julti);
        }
        return true;
    }

    @Override
    protected boolean resetInstance(MinecraftInstance instance, boolean bypassConditions) {
        if (super.resetInstance(instance, bypassConditions)) {
            if (displayInstances.contains(instance)) {
                displayInstances.set(displayInstances.indexOf(instance), null);
            }
            refreshDisplayInstances();
            return true;
        }
        return false;
    }

    @Override
    protected void lockInstance(MinecraftInstance instance) {
        super.lockInstance(instance);
        Collections.replaceAll(displayInstances, instance, null);
        if (JultiOptions.getInstance().dwReplaceLocked) {
            refreshDisplayInstances();
        }
    }

    @Override
    public void onMissingInstancesUpdate() {
        super.onMissingInstancesUpdate();
        displayInstances.clear();
        refreshDisplayInstances();
    }

    protected boolean resetNoWallUpdate(MinecraftInstance instance) {
        return super.resetInstance(instance, false);
    }

    @Override
    public void notifyDirtUncover(MinecraftInstance instance) {
        if (displayInstances.contains(instance)) return;
        for (MinecraftInstance replaceCandidateInstance : displayInstances) {
            if (replaceCandidateInstance.shouldDirtCover()) {
                Collections.replaceAll(displayInstances, replaceCandidateInstance, instance);
                instance.updateTimeLastAppeared();
                return;
            }
        }
    }

    @Override
    public Rectangle getInstancePosition(MinecraftInstance instance, Dimension sceneSize) {
        JultiOptions options = JultiOptions.getInstance();

        if (sceneSize != null) sceneSize = new Dimension(sceneSize);
        else sceneSize = new Dimension(1920, 1080);

        if (getLockedInstances().contains(instance)) {
            return getLockedInstancePosition(instance, sceneSize);
        } else if (!displayInstances.contains(instance)) {
            return new Rectangle(sceneSize.width, 0, 100, 100);
        }

        int totalRows = 2;
        int totalColumns = 2;
        if (!options.autoCalcWallSize) {
            totalRows = options.overrideRowsAmount;
            totalColumns = options.overrideColumnsAmount;
        }

        int instanceInd = displayInstances.indexOf(instance);

        Dimension dwInnerSize = sceneSize == null ? julti.getOBSSceneSize() : sceneSize;
        dwInnerSize.height = dwInnerSize.height - (int) ((options.lockedInstanceSpace / 100) * dwInnerSize.height);

        // Using floats here so there won't be any gaps in the wall after converting back to int
        float iWidth = dwInnerSize.width / (float) totalColumns;
        float iHeight = dwInnerSize.height / (float) totalRows;

        int row = instanceInd / totalColumns;
        int col = instanceInd % totalColumns;

        int x = (int) (col * iWidth);
        int y = (int) (row * iHeight);
        return new Rectangle(
                x,
                y,
                (int) ((col + 1) * iWidth) - x,
                (int) ((row + 1) * iHeight) - y
        );
    }

    private Rectangle getLockedInstancePosition(MinecraftInstance instance, Dimension sceneSize) {
        JultiOptions options = JultiOptions.getInstance();

        int instanceIndex = getLockedInstances().indexOf(instance);

        Dimension dwInnerSize = sceneSize == null ? julti.getOBSSceneSize() : sceneSize;
        int lockedInstanceHeight = (int) ((options.lockedInstanceSpace / 100) * dwInnerSize.height);
        dwInnerSize.height = dwInnerSize.height - lockedInstanceHeight;

        Dimension lockedInstanceSize = new Dimension((int) (dwInnerSize.width * (options.lockedInstanceSpace / 100)), lockedInstanceHeight);

        return new Rectangle(lockedInstanceSize.width * instanceIndex, dwInnerSize.height, lockedInstanceSize.width, lockedInstanceSize.height);
    }
}
