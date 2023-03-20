package xyz.duncanruns.julti.instance;

public class MCVersion {
    private final int major;
    private final int minor;

    MCVersion(int major, int minor) {
        this.major = major;
        this.minor = minor;
    }

    public int getMajor() {
        return this.major;
    }

    public int getMinor() {
        return this.minor;
    }

    @Override
    public String toString() {
        return String.format("1.%d.%d", this.major, this.minor);
    }
}
