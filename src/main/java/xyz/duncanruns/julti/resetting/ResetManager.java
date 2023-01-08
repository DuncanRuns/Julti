package xyz.duncanruns.julti.resetting;

import xyz.duncanruns.julti.AffinityManager;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.instance.InstanceManager;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.util.KeyboardUtil;
import xyz.duncanruns.julti.util.MouseUtil;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.Collections;
import java.util.List;

public abstract class ResetManager {
    protected final InstanceManager instanceManager;
    protected final Julti julti;

    public ResetManager(Julti julti) {
        this.julti = julti;
        this.instanceManager = julti.getInstanceManager();
    }

    public boolean doReset() {
        String toCopy = JultiOptions.getInstance().clipboardOnReset;
        if (!toCopy.isEmpty()) {
            KeyboardUtil.copyToClipboard(toCopy);
        }
        return true;
    }

    public boolean doBGReset() {
        return false;
    }

    public boolean doWallFullReset() {
        return false;
    }

    public boolean doWallSingleReset() {
        return false;
    }

    public boolean doWallLock() {
        return false;
    }

    public boolean doWallFocusReset() {
        return false;
    }

    public boolean doWallPlay() {
        return false;
    }

    public void notifyPreviewLoaded(MinecraftInstance instance) {
        JultiOptions options = JultiOptions.getInstance();
        if (options.useAffinity) {
            AffinityManager.ping(julti);
            AffinityManager.ping(julti, options.affinityBurst + 1);
        }
    }

    public void notifyWorldLoaded(MinecraftInstance instance) {
        if (JultiOptions.getInstance().useAffinity) {
            AffinityManager.ping(julti);
        }
    }

    public boolean shouldDirtCover(MinecraftInstance instance) {
        if (JultiOptions.getInstance().autoResetForBeach) {
            return instance.hasPreviewEverStarted() && (((!getLockedInstances().contains(instance)) && (!instance.isPreviewLoaded()) && (!instance.isWorldLoaded())) || instance.shouldDirtCover());
        }
        return instance.shouldDirtCover();
    }

    public List<MinecraftInstance> getLockedInstances() {
        return Collections.emptyList();
    }

    @Nullable
    protected MinecraftInstance getHoveredWallInstance() {
        JultiOptions options = JultiOptions.getInstance();
        Point point = MouseUtil.getMousePos();
        Rectangle bounds = julti.getWallBounds();
        Dimension sceneSize = julti.getOBSSceneSize();
        if (sceneSize == null) sceneSize = new Dimension(options.windowSize[0], options.windowSize[1]);
        point.translate(-bounds.x, -bounds.y);
        Point posOnScene = new Point(point);
        if (!sceneSize.equals(bounds.getSize())) {
            posOnScene.x = posOnScene.x * sceneSize.width / bounds.width;
            posOnScene.y = posOnScene.y * sceneSize.height / bounds.height;
        }

        for (MinecraftInstance instance : instanceManager.getInstances()) {
            if (getInstancePosition(instance, sceneSize).contains(point)) return instance;
        }
        return null;
    }

    /**
     * Gives the position that the instance should appear on a wall.
     * By default, it uses a basic wall layout determined by automatically calculating a wall size, or using the
     * user's override settings.
     *
     * @param instance the instance to get the position of
     * @return the position of the instance
     */
    public Rectangle getInstancePosition(MinecraftInstance instance, Dimension sceneSize) {
        List<MinecraftInstance> instances = instanceManager.getInstances();

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

        Dimension size = sceneSize == null ? julti.getOBSSceneSize() : sceneSize;

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
        return getInstancePosition(instance, null);
    }

    public void tick() {
    }
}
