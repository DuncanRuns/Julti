package xyz.duncanruns.julti.instance;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.instance.InstanceState.InWorldState;
import xyz.duncanruns.julti.util.ExceptionUtil;
import xyz.duncanruns.julti.util.FileUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class StateTracker {
    private final Path path;
    private final Runnable onStateChange;

    private boolean fileExists = false;
    private long mTime = 0L;

    private InstanceState instanceState = InstanceState.TITLE;
    private InstanceState.InWorldState inWorldState = InWorldState.UNPAUSED;
    private byte loadingPercent = 0;

    private final long[] lastStartArr;
    private final long[] lastOccurrenceArr;


    public StateTracker(Path path, Runnable onStateChange) {
        this.path = path;
        this.onStateChange = onStateChange;

        int totalStates = InstanceState.values().length;
        this.lastStartArr = new long[totalStates];
        this.lastOccurrenceArr = new long[totalStates];
        for (int i = 0; i < totalStates; i++) {
            this.lastStartArr[i] = this.lastOccurrenceArr[i] = 0L;
        }
    }

    private void update() throws IOException {
        boolean doOnStateChange = true;

        // Check existence
        if (!this.fileExists) {
            if (Files.exists(this.path)) {
                doOnStateChange = false;
                this.fileExists = true;
            } else {
                return;
            }
        }

        // Check for modification
        long newMTime = Files.getLastModifiedTime(this.path).toMillis();
        if (this.mTime == newMTime) {
            return;
        }

        // Store previous state
        InstanceState previousState = this.instanceState;

        if (!this.trySetStatesFromFile()) {
            return;
        }

        this.mTime = newMTime;

        long time = System.currentTimeMillis();

        if (previousState != this.instanceState && this.onStateChange != null) {
            // Set the last occurrence of the previous state to now, and the last start of the current state to now.
            this.lastOccurrenceArr[previousState.ordinal()] = time;
            this.lastStartArr[this.instanceState.ordinal()] = time;

            if (doOnStateChange) {
                this.onStateChange.run();
            }
        }
    }

    private boolean trySetStatesFromFile() throws IOException {
        // Read
        String out = FileUtil.readString(this.path);

        // Couldn't get output or output is empty (?)
        if (out.isEmpty()) {
            return false;
        }

        // Check for literal states
        switch (out) {
            case "waiting":
                this.setState(InstanceState.WAITING);
                return true;
            case "title":
                this.setState(InstanceState.TITLE);
                return true;
            case "inworld,paused":
                this.setState(InstanceState.INWORLD);
                this.inWorldState = InWorldState.PAUSED;
                return true;
            case "inworld,unpaused":
                this.setState(InstanceState.INWORLD);
                this.inWorldState = InWorldState.UNPAUSED;
                return true;
            case "inworld,gamescreenopen":
                this.setState(InstanceState.INWORLD);
                this.inWorldState = InWorldState.GAMESCREENOPEN;
                return true;
        }
        // Literal failed, should be generating/previewing

        String[] args = out.split(",");


        // Get previewing vs generating
        // Checking if the previous state was previewing fixes a bug where world preview states "generating" at around 98%
        if (this.instanceState == InstanceState.PREVIEWING || args[0].equals("previewing")) {
            this.setState(InstanceState.PREVIEWING);
        } else if (args[0].equals("generating")) {
            this.setState(InstanceState.GENERATING);
        } else {
            Julti.log(Level.ERROR, "Invalid state in " + this.path + ": \"" + out + "\"");
            return false;
        }

        if (args.length > 1) {
            // Get loading percent
            this.loadingPercent = Byte.parseByte(args[1]);
        } else {
            Julti.log(Level.ERROR, "Invalid state in " + this.path + ": \"" + out + "\"");
            return false;
        }
        return true;
    }

    private void setState(InstanceState state) {
        this.instanceState = state;
    }

    public boolean tryUpdate() {
        try {
            this.update();
            return true;
        } catch (IOException e) {
            Julti.log(Level.ERROR, "Error during state checking:\n" + ExceptionUtil.toDetailedString(e));
            return false;
        }
    }

    public long getLastOccurrenceOf(InstanceState state) {
        if (this.instanceState.equals(state)) {
            return System.currentTimeMillis();
        }
        return this.lastOccurrenceArr[state.ordinal()];
    }

    public long getLastStartOf(InstanceState state) {
        return this.lastStartArr[state.ordinal()];
    }

    /**
     * This method should only be used by MinecraftInstance, please refer to {@link MinecraftInstance#shouldCoverWithDirt()}.
     *
     * @return
     */
    public boolean shouldCoverWithDirt() {
        if (!JultiOptions.getJultiOptions().doDirtCovers || this.isCurrentState(InstanceState.TITLE)) {
            return false;
        }
        return (this.isCurrentState(InstanceState.WAITING) || this.isCurrentState(InstanceState.GENERATING));
    }

    /**
     * This method should only be used by MinecraftInstance, please refer to {@link MinecraftInstance#shouldFreeze()}.
     *
     * @return
     */
    public boolean shouldFreeze() {
        JultiOptions options = JultiOptions.getJultiOptions();

        if (!options.useFreezeFilter ||
                this.isCurrentState(InstanceState.TITLE) ||
                this.isCurrentState(InstanceState.WAITING) ||
                this.isCurrentState(InstanceState.GENERATING)) {
            return false;
        }

        return (int) this.getLoadingPercent() >= options.freezePercent;
    }

    public boolean isResettable() {
        long currentTime = System.currentTimeMillis();
        // Must be in preview or in the world, and the cooldown must have passed
        return (
                this.isCurrentState(InstanceState.INWORLD) || this.isCurrentState(InstanceState.PREVIEWING) || this.isCurrentState(InstanceState.TITLE) || (JultiOptions.getJultiOptions().allowResetDuringGenerating && this.isCurrentState(InstanceState.GENERATING))
        ) && (
                currentTime - this.getLastOccurenceOfNonResettable() > JultiOptions.getJultiOptions().wallResetCooldown
        );
    }

    private long getLastOccurenceOfNonResettable() {
        return this.getLastOccurrenceOf(JultiOptions.getJultiOptions().allowResetDuringGenerating ? InstanceState.WAITING : InstanceState.GENERATING);
    }

    public InstanceState getInstanceState() {
        return this.instanceState;
    }

    public boolean isCurrentState(InstanceState state) {
        return this.instanceState == state;
    }

    public byte getLoadingPercent() {
        return this.loadingPercent;
    }

    public InWorldState getInWorldType() {
        return this.inWorldState;
    }

    public int getResetSortingNum() {
        if (this.isCurrentState(InstanceState.WAITING)) {
            return -1;
        }
        if (this.isCurrentState(InstanceState.TITLE)) {
            return 10000000;
        }
        int i = 0;
        if (this.isCurrentState(InstanceState.PREVIEWING)) {
            i += 1100000;
        } else if (this.isCurrentState(InstanceState.INWORLD)) {
            i += 2200000;
        }
        if (this.isCurrentState(InstanceState.PREVIEWING) || this.isCurrentState(InstanceState.GENERATING)) {
            i += 10000 * this.loadingPercent;
        }
        i += (System.currentTimeMillis() - this.getLastOccurrenceOf(InstanceState.WAITING));
        return Math.min(10000000, i);
    }

    @Override
    public String toString() {
        return "StateTracker{" +
                "path=" + this.path +
                ", instanceState=" + this.instanceState +
                ", inWorldState=" + this.inWorldState +
                ", fileExists=" + this.fileExists +
                ", mTime=" + this.mTime +
                ", loadingPercent=" + this.loadingPercent +
                ", lastStartArr=" + Arrays.toString(this.lastStartArr) +
                ", lastOccurrenceArr=" + Arrays.toString(this.lastOccurrenceArr) +
                '}';
    }
}
