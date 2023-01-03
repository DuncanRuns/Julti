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
        if (JultiOptions.getInstance().useAffinity) {
            AffinityManager.ping(julti);
            AffinityManager.ping(julti, 301);
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
        Point point = MouseUtil.getMousePos();
        Rectangle bounds = julti.getWallBounds();
        point.translate(-bounds.x, -bounds.y);

        for (MinecraftInstance instance : instanceManager.getInstances()) {
            if (getInstancePosition(instance, bounds).contains(point)) return instance;
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
    public Rectangle getInstancePosition(MinecraftInstance instance, Rectangle wallBounds) {
        JultiOptions options = JultiOptions.getInstance();
        List<MinecraftInstance> instances = instanceManager.getInstances();

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

        Rectangle bounds = wallBounds == null ? julti.getWallBounds() : wallBounds;

        // Using floats here so there won't be any gaps in the wall after converting back to int
        float iWidth = bounds.width / (float) totalColumns;
        float iHeight = bounds.height / (float) totalRows;

        int row = instanceInd / totalColumns;
        int col = instanceInd % totalColumns;

        return new Rectangle(
                (int) (col * iWidth),
                (int) (row * iHeight),
                (int) ((col + 1) * iWidth),
                (int) ((row + 1) * iHeight)
        );
    }

    public Rectangle getInstancePosition(MinecraftInstance instance) {
        return getInstancePosition(instance, null);
    }
}
