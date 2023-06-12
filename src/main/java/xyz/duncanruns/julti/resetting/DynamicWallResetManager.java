package xyz.duncanruns.julti.resetting;

import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.affinity.AffinityManager;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.management.ActiveWindowManager;
import xyz.duncanruns.julti.management.InstanceManager;
import xyz.duncanruns.julti.management.OBSStateManager;
import xyz.duncanruns.julti.util.DoAllFastUtil;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class DynamicWallResetManager extends WallResetManager {
    private static final DynamicWallResetManager INSTANCE = new DynamicWallResetManager();

    private List<Integer> displayInstancesIndices = new ArrayList<>();
    private boolean isFirstReset = true;

    public static ResetManager getInstance() {
        return INSTANCE;
    }

    private List<MinecraftInstance> getDisplayInstances() {
        List<MinecraftInstance> allInstances = InstanceManager.getManager().getInstances();
        return this.displayInstancesIndices.stream().map(i -> i == null ? null : allInstances.get(i)).collect(Collectors.toList());
    }

    private void saveDisplayInstances(List<MinecraftInstance> displayInstances) {
        List<MinecraftInstance> allInstances = InstanceManager.getManager().getInstances();
        this.displayInstancesIndices = displayInstances.stream().map(i -> i == null ? null : allInstances.indexOf(i)).collect(Collectors.toList());
    }

    public void refreshDisplayInstances() {
        JultiOptions options = JultiOptions.getInstance();
        List<MinecraftInstance> displayInstances = this.getDisplayInstances();

        int totalRows = 2;
        int totalColumns = 2;
        if (!options.autoCalcWallSize) {
            totalRows = options.overrideRowsAmount;
            totalColumns = options.overrideColumnsAmount;
        }

        int numToDisplay = totalColumns * totalRows;

        if (displayInstances.size() != numToDisplay) {
            displayInstances = new ArrayList<>();
            for (int i = 0; i < numToDisplay; i++) {
                displayInstances.add(null);
            }
        }

        for (int i = 0; i < numToDisplay; i++) {
            MinecraftInstance instance = displayInstances.get(i);
            if (instance != null && ((displayInstances.get(i).isWindowMarkedMissing()) || this.getLockedInstances().contains(displayInstances.get(i)))) {
                displayInstances.set(i, null);
            }
        }

        if (!displayInstances.contains(null)) {
            this.saveDisplayInstances(displayInstances);
            return;
        }

        List<MinecraftInstance> instancePool = new ArrayList<>(InstanceManager.getManager().getInstances());
        instancePool.removeIf(instance -> this.getLockedInstances().contains(instance));
        instancePool.removeIf(displayInstances::contains);
        instancePool.sort((o1, o2) -> o2.getResetSortingNum() - o1.getResetSortingNum());

        while (displayInstances.contains(null)) {
            if (instancePool.isEmpty()) {
                break;
            }
            MinecraftInstance removed = instancePool.remove(0);
            removed.setVisible();
            displayInstances.set(displayInstances.indexOf(null), removed);
        }
        this.saveDisplayInstances(displayInstances);
    }

    @Override
    public List<ActionResult> doWallFullReset() {
        List<ActionResult> actionResults = new ArrayList<>();
        if (this.isFirstReset && !(actionResults = super.doWallFullReset()).isEmpty()) {
            this.isFirstReset = false;
            return actionResults;
        }
        if (!ActiveWindowManager.isWallActive()) {
            return actionResults;
        }

        List<ActionResult> finalActionResults = actionResults;
        // Do special reset so that display instances don't get replaced because it will be filled with null anyway
        DoAllFastUtil.doAllFast(this.getDisplayInstances().stream().filter(Objects::nonNull).collect(Collectors.toList()), instance -> {
            if (this.resetNoWallUpdate(instance)) {
                synchronized (finalActionResults) {
                    finalActionResults.add(ActionResult.INSTANCE_RESET);
                }
            }
        });

        if (JultiOptions.getInstance().useAffinity) {
            AffinityManager.ping();
        }
        // Fill display with null then refresh to ensure good order
        Collections.fill(this.displayInstancesIndices, null);
        this.refreshDisplayInstances();
        // Return true if something has happened: instances were reset OR the display was updated
        return actionResults;
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
        List<ActionResult> actionResults = new ArrayList<>(this.playInstanceFromWall(clickedInstance));

        // Get list of instances to reset
        List<MinecraftInstance> toReset = this.getDisplayInstances();
        toReset.removeIf(Objects::isNull);
        toReset.remove(clickedInstance);

        // Reset all others
        DoAllFastUtil.doAllFast(toReset, instance -> {
            if (this.resetInstance(instance)) {
                synchronized (actionResults) {
                    actionResults.add(ActionResult.INSTANCE_RESET);
                }
            }
        });

        if (JultiOptions.getInstance().useAffinity) {
            AffinityManager.ping();
        }
        return actionResults;
    }

    @Override
    public void notifyPreviewLoaded(MinecraftInstance instance) {
        List<MinecraftInstance> displayInstances = this.getDisplayInstances();
        if (displayInstances.contains(instance)) {
            return;
        }
        for (MinecraftInstance replaceCandidateInstance : displayInstances) {
            if (replaceCandidateInstance != null && replaceCandidateInstance.shouldCoverWithDirt()) {
                Collections.replaceAll(displayInstances, replaceCandidateInstance, instance);
                this.saveDisplayInstances(displayInstances);
                return;
            }
        }
    }

    @Override
    public void onMissingInstancesUpdate() {
        super.onMissingInstancesUpdate();
        this.displayInstancesIndices.clear();
        this.refreshDisplayInstances();
    }

    @Override
    public boolean resetInstance(MinecraftInstance instance, boolean bypassConditions) {
        List<MinecraftInstance> displayInstances = this.getDisplayInstances();


        if (super.resetInstance(instance, bypassConditions)) {
            if (displayInstances.contains(instance)) {
                this.displayInstancesIndices.set(displayInstances.indexOf(instance), null);
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
            List<MinecraftInstance> displayInstances = this.getDisplayInstances();
            if (Collections.replaceAll(displayInstances, instance, null)) {
                this.saveDisplayInstances(displayInstances);
            }
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
    public Rectangle getInstancePosition(MinecraftInstance instance, Dimension sceneSize) {
        JultiOptions options = JultiOptions.getInstance();

        if (sceneSize != null) {
            sceneSize = new Dimension(sceneSize);
        } else {
            sceneSize = new Dimension(1920, 1080);
        }

        List<MinecraftInstance> displayInstances = this.getDisplayInstances();

        if (this.getLockedInstances().contains(instance)) {
            return this.getLockedInstancePosition(instance, sceneSize);
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

    @Override
    public MinecraftInstance getRelativeInstance(int offset) {
        MinecraftInstance selectedInstance = InstanceManager.getManager().getSelectedInstance();
        List<MinecraftInstance> instances = this.getDisplayInstances();
        int startIndex = selectedInstance == null ? -1 : instances.indexOf(selectedInstance);
        return instances.get((startIndex + offset) % instances.size());
    }

    @Override
    public void reload() {
        this.displayInstancesIndices = new ArrayList<>();
        this.refreshDisplayInstances();
    }

    private Rectangle getLockedInstancePosition(MinecraftInstance instance, Dimension sceneSize) {
        JultiOptions options = JultiOptions.getInstance();

        int instanceIndex = this.getLockedInstances().indexOf(instance);

        Dimension dwInnerSize = sceneSize == null ? OBSStateManager.getInstance().getOBSSceneSize() : sceneSize;
        int lockedInstanceHeight = (int) ((options.lockedInstanceSpace / 100) * dwInnerSize.height);
        dwInnerSize.height = dwInnerSize.height - lockedInstanceHeight;

        Dimension lockedInstanceSize = new Dimension((int) (dwInnerSize.width * (options.lockedInstanceSpace / 100)), lockedInstanceHeight);

        return new Rectangle(lockedInstanceSize.width * instanceIndex, dwInnerSize.height, lockedInstanceSize.width, lockedInstanceSize.height);
    }
}
