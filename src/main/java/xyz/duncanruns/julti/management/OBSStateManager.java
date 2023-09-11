package xyz.duncanruns.julti.management;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.resetting.ResetHelper;
import xyz.duncanruns.julti.util.ExceptionUtil;
import xyz.duncanruns.julti.util.FileUtil;
import xyz.duncanruns.julti.util.GameOptionsUtil;

import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class OBSStateManager {
    private static final OBSStateManager INSTANCE = new OBSStateManager();
    private static final Path OUT_PATH = JultiOptions.getJultiDir().resolve("state");

    private Dimension obsSceneSize = null;
    private String currentLocation = "W";

    private String lastOut = "";

    public static OBSStateManager getOBSStateManager() {
        return INSTANCE;
    }

    public void tryOutputState() {
        JultiOptions options = JultiOptions.getJultiOptions();
        // Lazy try except (I sorry)
        try {
            StringBuilder out = new StringBuilder(this.currentLocation);
            //(lockedInstances.contains(instance) ? 1 : 0) + (resetManager.shouldDirtCover(instance) ? 2 : 0)
            Dimension size = this.getOBSSceneSize();
            if (size == null) {
                size = new Dimension(options.playingWindowSize[0], options.playingWindowSize[1]);
            }
            List<MinecraftInstance> lockedInstances = ResetHelper.getManager().getLockedInstances();
            for (MinecraftInstance instance : InstanceManager.getInstanceManager().getInstances()) {
                Rectangle instancePos = ResetHelper.getManager().getInstancePosition(instance, size);
                instancePos = new Rectangle(instancePos.x + options.instanceSpacing, instancePos.y + options.instanceSpacing, instancePos.width - (2 * options.instanceSpacing), instancePos.height - (2 * options.instanceSpacing));
                out.append(";")
                        // 0 = default
                        // 1 = locked
                        // 2 = dirt cover
                        // 3 = locked & dirt cover
                        // 4 = freeze filter
                        // 5 = locked & freeze filter
                        // TODO: probably shouldn't happen, 6 = cover & freeze filter
                        .append((lockedInstances.contains(instance) ? 1 : 0) + (options.doDirtCovers && instance.shouldCoverWithDirt() ? 2 : 0) + (options.useFreezeFilter && instance.shouldFreeze() ? 4 : 0))
                        .append(",")
                        .append(instancePos.x)
                        .append(",")
                        .append(instancePos.y)
                        .append(",")
                        .append(instancePos.width)
                        .append(",")
                        .append(instancePos.height);
            }
            String outString = out.toString();
            if (!outString.equals(this.lastOut)) {
                this.lastOut = outString;
                FileUtil.writeString(OUT_PATH, outString);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Dimension getOBSSceneSize() {
        if (this.obsSceneSize != null) {
            return new Dimension(this.obsSceneSize);
        }

        Path scriptSizeOutPath = JultiOptions.getJultiDir().resolve("obsscenesize");
        if (Files.exists(scriptSizeOutPath)) {
            try {
                String[] args = FileUtil.readString(scriptSizeOutPath).trim().split(",");
                this.obsSceneSize = new Dimension(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
            } catch (Exception e) {
                Julti.log(Level.ERROR, "Failed to read obsscenesize file:\n" + ExceptionUtil.toDetailedString(e));
            }
        }
        if (this.obsSceneSize != null) {
            return new Dimension(this.obsSceneSize);
        }
        return null;
    }

    public void setLocation(int instanceNum) {
        this.currentLocation = Integer.toString(instanceNum);
    }

    public void setLocationToWall() {
        this.currentLocation = "W";
    }

    public void tryOutputLSInfo() {
        Julti.log(Level.DEBUG, "OBSStateManager: Trying to output loading square info...");
        JultiOptions options = JultiOptions.getJultiOptions();
        List<MinecraftInstance> instances = InstanceManager.getInstanceManager().getInstances();

        if (instances.size() == 0) {
            Julti.log(Level.DEBUG, "OBSStateManager: No instances, cancelling.");
            return;
        }

        MinecraftInstance instance = instances.get(0);

        int width = options.resettingWindowSize[0];
        int height = options.resettingWindowSize[1];

        if (!options.useBorderless) {
            width -= 16;
            height -= 39;
        }

        int resettingGuiScale = this.getActualGuiScale(instance, width, height);
        int loadingSquareSize = resettingGuiScale * 90;
        // extraHeight is for including the % loaded text above the loading square
        int extraHeight = resettingGuiScale * 19;

        try {
            FileUtil.writeString(JultiOptions.getJultiDir().resolve("loadingsquaresize"), loadingSquareSize + "," + (loadingSquareSize + extraHeight));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        if (JultiOptions.getJultiOptions().prepareWindowOnLock) {
            // Check for alternate square size
            width = options.playingWindowSize[0];
            height = options.playingWindowSize[1];

            if (!options.useBorderless) {
                width -= 16;
                height -= 39;
            }
            int playingGuiScale = this.getActualGuiScale(instance, width, height);

            if (playingGuiScale != resettingGuiScale) {
                Julti.log(Level.WARN, "VERIFICATION WARNING: You have prepare window on lock enabled, and your guiScale options means that the loading square size will change! In standard settings, you should change your guiScale to " + Math.min(resettingGuiScale, playingGuiScale) + ", and guiScaleOnWorldJoin to " + playingGuiScale + ".");
            }
        }
    }

    /**
     * Determines the gui scale that actually gets used during resets on this instance.
     */
    public int getActualGuiScale(MinecraftInstance instance, int resettingWidth, int resettingHeight) {
        // Get values
        int guiScale = 0;
        try {
            String gsOption = GameOptionsUtil.tryGetOption(instance.getPath(), "guiScale", true);
            if (gsOption != null) {
                guiScale = Integer.parseInt(gsOption);
            }
        } catch (NumberFormatException ignored) {
            // Failed to get options, assume 0
        }
        boolean forceUnicodeFont = Objects.equals(GameOptionsUtil.tryGetOption(instance.getPath(), "forceUnicodeFont", true), "true");

        // Minecraft code magic
        int i = 1;
        while ((i != guiScale)
                && (i < resettingWidth)
                && (i < resettingHeight)
                && (resettingWidth / (i + 1) >= 320)
                && (resettingHeight / (i + 1) >= 240)) {
            ++i;
        }
        if (forceUnicodeFont && i % 2 != 0) {
            ++i;
        }

        return i;
    }

}
