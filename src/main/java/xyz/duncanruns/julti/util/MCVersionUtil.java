package xyz.duncanruns.julti.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MCVersionUtil {
    // A map to help convert snapshots to a comparable version string
    // The integer arrays will always contain 3 integers, the first being the year the snapshot should be from, then the minimum and maximum week number
    // Snapshots for major version updates should be converted to a .99 of the former major version
    // Snapshots for minor version updates should be rounded to the latter minor version
    private static final Map<int[], String> SNAP_MAP = getSnapMap();
    private static final Pattern VERSION_PATTERN = Pattern.compile("^1\\.\\d+(\\.\\d+)?");
    private static final Pattern SNAPSHOT_VERSION_PATTERN = Pattern.compile("^(\\d\\d)w0?(\\d+).+"); // Does not exactly match, but is useful for matching groups

    private MCVersionUtil() {
    }

    private static Map<int[], String> getSnapMap() {
        Map<int[], String> map = new HashMap<>();

        // TODO: older snapshots
        map.put(new int[]{20, 6, 22}, "1.15.99"); // 1.16 snapshots, including infinity april fools
        map.put(new int[]{20, 27, 30}, "1.16.2"); // 1.16.2 snapshots
        map.put(new int[]{20, 45, 51}, "1.16.99"); // 1.17 snapshots
        map.put(new int[]{21, 3, 20}, "1.16.99"); // 1.17 snapshots
        map.put(new int[]{21, 37, 44}, "1.17.99"); // 1.18 snapshots
        map.put(new int[]{22, 3, 7}, "1.18.2"); // 1.18.2 snapshots
        map.put(new int[]{22, 11, 19}, "1.18.99"); // 1.19 snapshots, including one block at a time april fools
        map.put(new int[]{22, 24, 24}, "1.19.1"); // 1.19.1 snapshot
        map.put(new int[]{22, 42, 46}, "1.19.3"); // 1.19.3 snapshots
        map.put(new int[]{23, 3, 7}, "1.19.4"); // 1.19.4 snapshots
        map.put(new int[]{23, 12, 52}, "1.19.99"); // 1.20 snapshots, including voting april fools

        return Collections.unmodifiableMap(map);
    }

    private static int[] getVersionInts(String versionString) {
        Matcher m;

        // Check for stable releases, pre releases, and release candidates
        m = VERSION_PATTERN.matcher(versionString);
        if (m.matches()) {
            String[] sInts = m.group().split("\\.");
            int[] ints = new int[sInts.length];
            Arrays.setAll(ints, i -> Integer.parseInt(sInts[i]));
            return ints.length == 3 ? ints : new int[]{ints[0], ints[1], 0};
        }

        // Check for snapshots
        m = SNAPSHOT_VERSION_PATTERN.matcher(versionString);
        if (m.matches()) {
            int year = Integer.parseInt(m.group(1));
            int week = Integer.parseInt(m.group(2));
            for (Map.Entry<int[], String> entry : SNAP_MAP.entrySet()) {
                int[] searchInts = entry.getKey();
                if (year == searchInts[0] && week >= searchInts[1] && week <= searchInts[2]) {
                    return getVersionInts(entry.getValue());
                }
            }
        }
        return null;
    }

    /**
     * versionY should not refer to a snapshot, to check for equaling an exact snapshot, use {@link String#equals(Object) String.equals()}.
     *
     * @return true if versionX is older than versionY, otherwise false
     */
    public static boolean isOlderThan(String versionX, String versionY) {
        return (!isLooselyEqual(versionX, versionY)) && (!isNewerThan(versionX, versionY));
    }

    /**
     * versionY should not refer to a snapshot, to check for equaling an exact snapshot, use {@link String#equals(Object) String.equals()}.
     *
     * @return true if versionX is newer than versionY, otherwise false
     */
    public static boolean isNewerThan(String versionX, String versionY) {
        int[] intsX = getVersionInts(versionX);
        int[] intsY = getVersionInts(versionY);

        for (int i = 0; i < 3; i++) {
            if (intsX[i] > intsY[i]) {
                return true;
            } else if (intsX[i] < intsY[i]) {
                return false;
            }
        }
        return false;
    }

    /**
     * <li>2 stable releases are not loosely equals (ie. 1.14.3 != 1.14.4)</li>
     * <li>2 snapshots for the same version are loosely equal (ie. 22w43a = 22w46a)</li>
     * <li>A snapshot for a minor release equals the minor release (ie. 22w06a = 1.18.2</li>
     * <li>A snapshot for a major release never equals any stable release (ie. 21w42a != 1.18)</li>
     * <p>
     * Equal checking should just use {@link String#equals(Object) String.equals()}, this method does not work for checking exact snapshots.
     *
     * @return true if versionX is the same version as versionY, otherwise false
     */
    private static boolean isLooselyEqual(String versionX, String versionY) {
        return Arrays.equals(getVersionInts(versionX), getVersionInts(versionY));
    }
}
