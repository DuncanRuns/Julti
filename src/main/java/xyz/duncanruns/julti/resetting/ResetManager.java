package xyz.duncanruns.julti.resetting;

import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.affinity.AffinityManager;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.management.ActiveWindowTracker;
import xyz.duncanruns.julti.management.InstanceManager;
import xyz.duncanruns.julti.management.OBSStateManager;
import xyz.duncanruns.julti.util.KeyboardUtil;
import xyz.duncanruns.julti.util.MouseUtil;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.Collections;
import java.util.List;

public abstract class ResetManager {

    public List<ActionResult> doReset() {
        String toCopy = JultiOptions.getInstance().clipboardOnReset;
        if (!toCopy.isEmpty()) {
            KeyboardUtil.copyToClipboard(toCopy);
        }
        return Collections.emptyList();
    }

    public List<ActionResult> doBGReset() {
        return Collections.emptyList();
    }

    public List<ActionResult> doWallFullReset() {
        return Collections.emptyList();
    }

    public List<ActionResult> doWallSingleReset() {
        return Collections.emptyList();
    }

    public List<ActionResult> doWallLock() {
        return Collections.emptyList();
    }

    public List<ActionResult> doWallFocusReset() {
        return Collections.emptyList();
    }

    public List<ActionResult> doWallPlay() {
        return Collections.emptyList();
    }

    public List<ActionResult> doWallPlayLock() {
        return Collections.emptyList();
    }

    public void notifyPreviewLoaded(MinecraftInstance instance) {
        JultiOptions options = JultiOptions.getInstance();
        if (options.useAffinity) {
            AffinityManager.ping();
            AffinityManager.ping(options.affinityBurst + 1);
        }
    }

    public void notifyWorldLoaded(MinecraftInstance instance) {
        if (JultiOptions.getInstance().useAffinity) {
            AffinityManager.ping();
        }
    }

    public List<MinecraftInstance> getLockedInstances() {
        return Collections.emptyList();
    }

    @Nullable
    protected MinecraftInstance getHoveredWallInstance() {
        JultiOptions options = JultiOptions.getInstance();
        Point point = MouseUtil.getMousePos();
        Rectangle bounds = ActiveWindowTracker.getActiveWindowBounds();
        Dimension sceneSize = OBSStateManager.getInstance().getOBSSceneSize();
        if (sceneSize == null) {
            sceneSize = new Dimension(options.playingWindowSize[0], options.playingWindowSize[1]);
        }
        point.translate(-bounds.x, -bounds.y);
        Point posOnScene = new Point(point);
        if (!sceneSize.equals(bounds.getSize())) {
            posOnScene.x = posOnScene.x * sceneSize.width / bounds.width;
            posOnScene.y = posOnScene.y * sceneSize.height / bounds.height;
        }

        for (MinecraftInstance instance : InstanceManager.getManager().getInstances()) {
            if (this.getInstancePosition(instance, sceneSize).contains(posOnScene)) {
                return instance;
            }
        }
        return null;
    }

    /**
     * Gives the position that the instance should appear on a wall.
     * By default, it uses a basic wall layout determined by automatically calculating a wall size, or using the
     * user's override settings.
     *
     * @param instance the instance to get the position of
     *
     * @return the position of the instance
     */
    public Rectangle getInstancePosition(MinecraftInstance instance, Dimension sceneSize) {
        List<MinecraftInstance> instances = InstanceManager.getManager().getInstances();

        JultiOptions options = JultiOptions.getInstance();
        int totalRows;
        int totalColumns;

        if (options.autoCalcWallSize) {
            totalRows = (int) Math.ceil(Math.sqrt(instances.size()));
            totalColumns = (int) Math.ceil(instances.size() / (float) totalRows);
        } else {
            totalRows = options.overrideRowsAmount;
            totalColumns = options.overrideColumnsAmount;
        }

        int instanceInd = instances.indexOf(instance);

        Dimension size = sceneSize == null ? OBSStateManager.getInstance().getOBSSceneSize() : sceneSize;

        // Using floats here so there won't be any gaps in the wall after converting back to int
        float iWidth = size.width / (float) totalColumns;
        float iHeight = size.height / (float) totalRows;

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

    public Rectangle getInstancePosition(MinecraftInstance instance) {
        return this.getInstancePosition(instance, null);
    }

    public void tick() {
    }

    public void onMissingInstancesUpdate() {
    }

    public boolean resetInstance(MinecraftInstance instance) {
        instance.reset();
        return true;
    }

    public boolean resetInstance(MinecraftInstance instance, boolean bypassConditions) {
        instance.reset();
        return true;
    }

    public boolean lockInstance(MinecraftInstance instance) {
        if (JultiOptions.getInstance().prepareWindowOnLock) {
            // We use doLater because this is a laggy method that isn't incredibly important.
            Julti.doLater(instance::ensurePlayingWindowState);
        }
        return false;
    }

    public MinecraftInstance getRelativeInstance(int offset) {
        MinecraftInstance selectedInstance = InstanceManager.getManager().getSelectedInstance();
        List<MinecraftInstance> instances = InstanceManager.getManager().getInstances();
        int startIndex = selectedInstance == null ? -1 : instances.indexOf(selectedInstance);
        return instances.get((startIndex + offset) % instances.size());
    }

    public void reload() {
    }
}
