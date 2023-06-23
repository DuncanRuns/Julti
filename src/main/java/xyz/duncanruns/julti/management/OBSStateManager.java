package xyz.duncanruns.julti.management;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.resetting.ResetHelper;
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

    public static OBSStateManager getInstance() {
        return INSTANCE;
    }

    public void tryOutputState() {
        JultiOptions options = JultiOptions.getInstance();
        // Lazy try except (I sorry)
        try {
            StringBuilder out = new StringBuilder(this.currentLocation);
            //(lockedInstances.contains(instance) ? 1 : 0) + (resetManager.shouldDirtCover(instance) ? 2 : 0)
            Dimension size = this.getOBSSceneSize();
            if (size == null) {
                size = new Dimension(options.playingWindowSize[0], options.playingWindowSize[1]);
            }
            List<MinecraftInstance> lockedInstances = ResetHelper.getManager().getLockedInstances();
            for (MinecraftInstance instance : InstanceManager.getManager().getInstances()) {
                Rectangle instancePos = ResetHelper.getManager().getInstancePosition(instance, size);
                instancePos = new Rectangle(instancePos.x + options.instanceSpacing, instancePos.y + options.instanceSpacing, instancePos.width - (2 * options.instanceSpacing), instancePos.height - (2 * options.instanceSpacing));
                out.append(";")
                        .append((lockedInstances.contains(instance) ? 1 : 0) + (options.doDirtCovers && instance.shouldCoverWithDirt() ? 2 : 0))
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
        } catch (Exception ignored) {
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
            } catch (Exception ignored) {
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
        JultiOptions options = JultiOptions.getInstance();
        List<MinecraftInstance> instances = InstanceManager.getManager().getInstances();

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
            Julti.log(Level.ERROR, "OBSStateManager: Failed to write loadingsquaresize! " + e);
        }


        if (JultiOptions.getInstance().prepareWindowOnLock) {
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
