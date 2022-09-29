package xyz.duncanruns.julti.instance;

import java.util.regex.Pattern;

// Maintains the most accurate possible original title of a minecraft window to obtain version info.
public class WindowTitleInfo {
    private static final Pattern MC_PATTERN = Pattern.compile("^Minecraft\\*? 1\\.[1-9]\\d*(\\.[1-9]\\d*)?( .*)?$");
    private static final String DEFAULT_TITLE = "Minecraft* 1.16.1";

    private String title;

    public WindowTitleInfo(String title) {
        this.title = title;
    }

    public WindowTitleInfo() {
        this.title = "None";
    }

    public void provide(String title) {
        if (waiting() && MC_PATTERN.matcher(title).matches()) {
            this.title = title;
        }
    }

    public boolean waiting() {
        return !MC_PATTERN.matcher(this.title).matches();
    }

    public Version getVersion() {
        String[] nums = getOriginalTitle().split(" ")[1].split("\\.");
        if (nums.length > 2) {
            return new Version(Integer.parseInt(nums[1]), Integer.parseInt(nums[2]));
        } else {
            return new Version(Integer.parseInt(nums[1]), 0);
        }
    }

    public String getOriginalTitle() {
        if (!waiting()) {
            return this.title;
        }
        return DEFAULT_TITLE;
    }

    public static class Version {
        private final int major;
        private final int minor;

        private Version(int major, int minor) {
            this.major = major;
            this.minor = minor;
        }

        public int getMajor() {
            return major;
        }

        public int getMinor() {
            return minor;
        }
    }
}
