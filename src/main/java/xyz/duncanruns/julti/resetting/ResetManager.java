package xyz.duncanruns.julti.resetting;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.duncanruns.julti.AffinityManager;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.instance.InstanceManager;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.util.KeyboardUtil;
import xyz.duncanruns.julti.util.LogReceiver;
import xyz.duncanruns.julti.util.MouseUtil;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.Collections;
import java.util.List;

public abstract class ResetManager {
    private static final Logger LOGGER = LogManager.getLogger("ResetManager");

    protected final InstanceManager instanceManager;
    protected final Julti julti;

    public ResetManager(Julti julti) {
        this.julti = julti;
        this.instanceManager = julti.getInstanceManager();
    }

    public static void log(Level level, String message) {
        LOGGER.log(level, message);
        LogReceiver.receive(level, message);
    }

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
            AffinityManager.ping(this.julti);
            AffinityManager.ping(this.julti, options.affinityBurst + 1);
        }
    }

    public void notifyWorldLoaded(MinecraftInstance instance) {
        if (JultiOptions.getInstance().useAffinity) {
            AffinityManager.ping(this.julti);
        }
    }

    public void notifyInstanceAvailable(MinecraftInstance instance) {
    }

    public boolean shouldDirtCover(MinecraftInstance instance) {
        if (JultiOptions.getInstance().autoResetForBeach) {
            return instance.hasPreviewEverStarted() && (((!this.getLockedInstances().contains(instance)) && (!instance.isPreviewLoaded()) && (!instance.isWorldLoaded())) || instance.shouldDirtCover());
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
        Rectangle bounds = this.julti.getWallBounds();
        Dimension sceneSize = this.julti.getOBSSceneSize();
        if (sceneSize == null) {
            sceneSize = new Dimension(options.windowSize[0], options.windowSize[1]);
        }
        point.translate(-bounds.x, -bounds.y);
        Point posOnScene = new Point(point);
        if (!sceneSize.equals(bounds.getSize())) {
            posOnScene.x = posOnScene.x * sceneSize.width / bounds.width;
            posOnScene.y = posOnScene.y * sceneSize.height / bounds.height;
        }

        for (MinecraftInstance instance : this.instanceManager.getInstances()) {
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
     * @return the position of the instance
     */
    public Rectangle getInstancePosition(MinecraftInstance instance, Dimension sceneSize) {
        List<MinecraftInstance> instances = this.instanceManager.getInstances();

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

        Dimension size = sceneSize == null ? this.julti.getOBSSceneSize() : sceneSize;

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
        instance.reset(this.instanceManager.getInstances().size() == 1);
        return true;
    }

    public boolean resetInstance(MinecraftInstance instance, boolean bypassConditions) {
        instance.reset(this.instanceManager.getInstances().size() == 1);
        return true;
    }

    public boolean lockInstance(MinecraftInstance instance) {
        if (JultiOptions.getInstance().unsquishOnLock) {
            new Thread(() -> instance.ensureWindowState(false, false), "unsquisher").start();
        }
        return false;
    }

    public MinecraftInstance getRelativeInstance(int offset) {
        MinecraftInstance selectedInstance = this.instanceManager.getSelectedInstance();
        List<MinecraftInstance> instances = this.instanceManager.getInstances();
        int startIndex = selectedInstance == null ? -1 : instances.indexOf(selectedInstance);
        return instances.get((startIndex + offset) % instances.size());
    }
}
