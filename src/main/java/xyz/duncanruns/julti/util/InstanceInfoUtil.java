package xyz.duncanruns.julti.util;

import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.ptr.IntByReference;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.win32.User32;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A utility class for functions useful for finding the instance path and game version of a Minecraft window launched
 * from the vanilla launcher, MultiMC, or Prism Launcher.
 */
public final class InstanceInfoUtil {
    // Version Patterns
    private static final Pattern VANILLA_VERSION_PATTERN = Pattern.compile(" --version (.+?) ");
    private static final Pattern MULTIMC_VERSION_PATTERN = Pattern.compile("minecraft-(.+)-client.jar");
    // Vanilla Path Patterns
    private static final Pattern VANILLA_PATH_PATTERN = Pattern.compile("--gameDir (.+?) ");
    private static final Pattern VANILLA_PATH_PATTERN_SPACES = Pattern.compile("--gameDir \"(.+?)\"");
    // MultiMC Path Patterns
    private static final Pattern MULTIMC_PATH_PATTERN = Pattern.compile("-Djava\\.library\\.path=(.+?) ");
    private static final Pattern MULTIMC_PATH_PATTERN_SPACES = Pattern.compile("\"-Djava\\.library\\.path=(.+?)\"");


    private InstanceInfoUtil() {
    }

    /**
     * Uses powershell to get the command line of a Minecraft instance and retrieve relevant information about it
     *
     * @param hwnd the window pointer object of the Minecraft instance
     *
     * @return the extracted instance info of the Minecraft instance
     */
    public static FoundInstanceInfo getInstanceInfoFromHwnd(HWND hwnd) {
        // Get command line
        String commandLine = getCommandLine(getPidFromHwnd(hwnd));
        // If no command line, return null
        if (commandLine == null) {
            return null;
        }
        // Check launcher type
        try {
            if (commandLine.contains("--gameDir")) {
                // Vanilla
                return getVanillaInfo(commandLine);
            } else if (commandLine.contains("-Djava.library.path=")) {
                // MultiMC or Prism
                return getMultiMCInfo(commandLine);
            }
        } catch (Exception e) {
            Julti.log(Level.ERROR, "An exception occured while obtaining instance information: " + e);
        }
        // If the command line does not match MultiMC or Vanilla, or if there was an exception, return null
        return null;
    }

    private static String getCommandLine(int pid) {
        try {
            return PowerShellUtil.execute("$proc = Get-CimInstance Win32_Process -Filter \"ProcessId = PIDHERE\";$proc.CommandLine".replace("PIDHERE", String.valueOf(pid)));
        } catch (Exception ignored) {
        }
        return null;
    }

    private static int getPidFromHwnd(HWND hwnd) {
        final IntByReference pidPointer = new IntByReference();
        User32.INSTANCE.GetWindowThreadProcessId(hwnd, pidPointer);
        return pidPointer.getValue();
    }

    private static FoundInstanceInfo getVanillaInfo(String commandLine) throws InvalidPathException {
        // Declare reusable matcher variable
        Matcher matcher;

        // Check for quotation mark to determine matcher
        if (commandLine.contains("--gameDir \"")) {
            matcher = VANILLA_PATH_PATTERN_SPACES.matcher(commandLine);
        } else {
            matcher = VANILLA_PATH_PATTERN.matcher(commandLine);
        }

        // If no matches are found for the path, return null
        if (!matcher.find()) {
            return null;
        }

        // Get the path out of the group
        String pathString = matcher.group(1);

        // Assign the version matcher
        matcher = VANILLA_VERSION_PATTERN.matcher(commandLine);

        // If no matches are found for the version, return null
        if (!matcher.find()) {
            return null;
        }

        // Get the version out of the group
        String versionString = matcher.group(1);

        return new FoundInstanceInfo(versionString, Paths.get(pathString));
    }

    private static FoundInstanceInfo getMultiMCInfo(String commandLine) throws InvalidPathException {
        // Declare reusable matcher variable
        Matcher matcher;

        // Check for quotation mark to determine matcher
        if (commandLine.contains("\"-Djava.library.path=")) {
            matcher = MULTIMC_PATH_PATTERN_SPACES.matcher(commandLine);
        } else {
            matcher = MULTIMC_PATH_PATTERN.matcher(commandLine);
        }

        // If no matches are found for the path, return null
        if (!matcher.find()) {
            return null;
        }

        // Get the natives path out of the group
        String nativesPathString = matcher.group(1);

        // Assign the version matcher
        matcher = MULTIMC_VERSION_PATTERN.matcher(commandLine);

        // If no matches are found for the version, return null
        if (!matcher.find()) {
            return null;
        }

        // Get the version out of the group
        String versionString = matcher.group(1);

        return new FoundInstanceInfo(versionString, Paths.get(nativesPathString).resolveSibling(".minecraft"));
    }

    public static class FoundInstanceInfo {
        public final String versionString;
        public final Path instancePath;

        private FoundInstanceInfo(String versionString, Path instancePath) {
            this.versionString = versionString;
            this.instancePath = instancePath;
        }
    }

}
