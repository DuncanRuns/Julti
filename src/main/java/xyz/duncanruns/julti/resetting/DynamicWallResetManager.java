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
        this.refreshDisplayInstances();
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

        if (this.displayInstances.size() != numToDisplay) {
            this.displayInstances = new ArrayList<>();
            for (int i = 0; i < numToDisplay; i++) { this.displayInstances.add(null); }
        }

        for (int i = 0; i < numToDisplay; i++) {
            MinecraftInstance instance = this.displayInstances.get(i);
            if (instance != null && (!this.displayInstances.get(i).hasWindowQuick() || getLockedInstances().contains(this.displayInstances.get(i)))) {
                this.displayInstances.set(i, null);
            }
        }

        if (!this.displayInstances.contains(null)) return;

        List<MinecraftInstance> instancePool = new ArrayList<>(this.instanceManager.getInstances());
        instancePool.removeIf(instance -> getLockedInstances().contains(instance));
        instancePool.removeIf(instance -> this.displayInstances.contains(instance));
        instancePool.sort((o1, o2) -> o2.getWallSortingNum() - o1.getWallSortingNum());

        while (this.displayInstances.contains(null)) {
            if (instancePool.isEmpty()) break;
            MinecraftInstance removed = instancePool.remove(0);
            removed.updateTimeLastAppeared();
            this.displayInstances.set(this.displayInstances.indexOf(null), removed);
        }
    }

    @Override
    public List<ActionResult> doWallFullReset() {
        List<ActionResult> actionResults = new ArrayList<>();
        if (this.isFirstReset && !(actionResults = super.doWallFullReset()).isEmpty()) {
            this.isFirstReset = false;
            return actionResults;
        }
        if (!this.julti.isWallActive()) {
            return actionResults;
        }
        // instead of using the displayInstances, we use "all instances that are also found in displayInstances", which
        // uses an instance's equals() method to match instance paths so that if displayInstances has an abandoned
        // instance object, it can still be used.
        List<MinecraftInstance> resettable = this.instanceManager.getInstances().stream().filter(instance -> this.displayInstances.contains(instance)).collect(Collectors.toList());
        // Do special reset so that display instances don't get replaced because it will be filled with null anyway
        for (MinecraftInstance instance : resettable) {
            if (resetNoWallUpdate(instance)) actionResults.add(ActionResult.INSTANCE_RESET);
        }
        if (JultiOptions.getInstance().useAffinity) {
            AffinityManager.ping(this.julti);
        }
        // Fill display with null then refresh to ensure good order
        Collections.fill(this.displayInstances, null);
        this.refreshDisplayInstances();
        // Return true if something has happened: instances were reset OR the display was updated
        return actionResults;
    }

    @Override
    public List<ActionResult> doWallFocusReset() {
        if (!this.julti.isWallActive()) { return Collections.emptyList(); }
        // Regular play instance method
        MinecraftInstance clickedInstance = this.getHoveredWallInstance();
        if (clickedInstance == null) { return Collections.emptyList(); }
        List<ActionResult> actionResults = new ArrayList<>(playInstanceFromWall(clickedInstance));

        // Reset all others
        for (MinecraftInstance instance : this.instanceManager.getInstances()) {
            if (getLockedInstances().contains(instance) || (!this.displayInstances.contains(instance))) continue;
            if (!instance.equals(clickedInstance)) {
                if (resetInstance(instance)) actionResults.add(ActionResult.INSTANCE_RESET);
            }
        }
        if (JultiOptions.getInstance().useAffinity) {
            AffinityManager.ping(this.julti);
        }
        return actionResults;
    }

    @Override
    public void onMissingInstancesUpdate() {
        super.onMissingInstancesUpdate();
        this.displayInstances.clear();
        this.refreshDisplayInstances();
    }

    @Override
    public boolean resetInstance(MinecraftInstance instance, boolean bypassConditions) {
        if (super.resetInstance(instance, bypassConditions)) {
            if (this.displayInstances.contains(instance)) {
                this.displayInstances.set(this.displayInstances.indexOf(instance), null);
            }
            this.refreshDisplayInstances();
            return true;
        }
        return false;
    }

    @Override
    public boolean lockInstance(MinecraftInstance instance) {
        // If super.lockInstance is true then it actually got locked and checked to unsquish
        if (super.lockInstance(instance)) {
            boolean ignored = Collections.replaceAll(this.displayInstances, instance, null);
            if (JultiOptions.getInstance().dwReplaceLocked) {
                this.refreshDisplayInstances();
            }
            return true;
        }
        return false;
    }

    protected boolean resetNoWallUpdate(MinecraftInstance instance) {
        return super.resetInstance(instance, false);
    }

    @Override
    public void notifyInstanceAvailable(MinecraftInstance instance) {
        if (this.displayInstances.contains(instance)) return;
        for (MinecraftInstance replaceCandidateInstance : this.displayInstances) {
            if (replaceCandidateInstance != null && !replaceCandidateInstance.isAvailable()) {
                boolean ignored = Collections.replaceAll(this.displayInstances, replaceCandidateInstance, instance);
                return;
            }
        }
    }

    @Override
    public Rectangle getInstancePosition(MinecraftInstance instance, Dimension sceneSize) {
        JultiOptions options = JultiOptions.getInstance();

        if (sceneSize != null) { sceneSize = new Dimension(sceneSize); }
        else { sceneSize = new Dimension(1920, 1080); }

        if (getLockedInstances().contains(instance)) {
            return this.getLockedInstancePosition(instance, sceneSize);
        } else if (!this.displayInstances.contains(instance)) {
            return new Rectangle(sceneSize.width, 0, 100, 100);
        }

        int totalRows = 2;
        int totalColumns = 2;
        if (!options.autoCalcWallSize) {
            totalRows = options.overrideRowsAmount;
            totalColumns = options.overrideColumnsAmount;
        }

        int instanceInd = this.displayInstances.indexOf(instance);

        Dimension dwInnerSize = sceneSize;
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

        int instanceIndex = this.getLockedInstances().indexOf(instance);

        Dimension dwInnerSize = sceneSize == null ? this.julti.getOBSSceneSize() : sceneSize;
        int lockedInstanceHeight = (int) ((options.lockedInstanceSpace / 100) * dwInnerSize.height);
        dwInnerSize.height = dwInnerSize.height - lockedInstanceHeight;

        Dimension lockedInstanceSize = new Dimension((int) (dwInnerSize.width * (options.lockedInstanceSpace / 100)), lockedInstanceHeight);

        return new Rectangle(lockedInstanceSize.width * instanceIndex, dwInnerSize.height, lockedInstanceSize.width, lockedInstanceSize.height);
    }

    @Override
    public MinecraftInstance getRelativeInstance(int offset) {
        MinecraftInstance selectedInstance = this.instanceManager.getSelectedInstance();
        List<MinecraftInstance> instances = new ArrayList<>(this.displayInstances);
        int startIndex = selectedInstance == null ? -1 : instances.indexOf(selectedInstance);
        return instances.get((startIndex + offset) % instances.size());
    }
}
