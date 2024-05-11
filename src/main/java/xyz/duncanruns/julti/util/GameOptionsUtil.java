package xyz.duncanruns.julti.util;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public final class GameOptionsUtil {
    private GameOptionsUtil() {
    }

    private static String getOptionFromString(String optionName, String optionsString) {
        String[] lines = optionsString.trim().split("\\n");
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i];

            String[] optionKeyValArr = line.split(":");
            String lineKey = optionKeyValArr[0];

            if (lineKey.endsWith("§r")) {
                lineKey = lineKey.split("§")[0];
            }

            if (Objects.equals(optionName, lineKey)) {
                if (optionKeyValArr.length < 2) {
                    continue;
                }
                return optionKeyValArr[1];
            }
        }
        return null;
    }

    public static boolean tryGetBoolOption(Path instancePath, String optionName, boolean tryUseSS) {
        return Objects.equals(tryGetOption(instancePath, optionName, tryUseSS), "true");
    }

    public static String tryGetOption(Path instancePath, String optionName, boolean tryUseSS) {
        // This should prevent any crazy out of pocket bullshits like 1 in a million parsing error situations
        try {
            return getOption(instancePath, optionName, tryUseSS);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getOption(Path instancePath, String optionName, boolean tryUseSS) {
        if (tryUseSS) {
            String out = tryGetStandardOption(instancePath, optionName);
            if (out != null) {
                return out;
            }
        }

        Path path = instancePath.resolve("options.txt");

        if (!Files.exists(path)) {
            return null;
        }

        String out;
        try {
            out = FileUtil.readString(path);
        } catch (IOException e) {
            // This should never be reached but nullability helps out surely
            return null;
        }

        return getOptionFromString(optionName, out).trim();
    }

    public static String tryGetStandardOption(Path instancePath, String optionName) {
        try {
            return getStandardOption(instancePath, optionName);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getStandardOption(Path instancePath, String optionName) {
        String out = getStandardOption(optionName, instancePath.resolve("config").resolve("standardoptions.txt"));
        if (out != null) {
            return out.trim();
        }
        return null;
    }

    /**
     * Determines the gui scale that actually gets used during resets on this instance.
     */
    public static int getResettingGuiScale(Path instancePath, int resettingWidth, int resettingHeight) {
        // Get values
        int guiScale = 0;
        try {
            guiScale = Integer.parseInt(tryGetOption(instancePath, "guiScale", true));
        } catch (NumberFormatException e) {
            Julti.log(Level.WARN, "Invalid guiScale value found in options:\n" + ExceptionUtil.toDetailedString(e));
        }
        boolean forceUnicodeFont = Objects.equals(tryGetOption(instancePath, "forceUnicodeFont", true), "true");

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

    public static String getStandardOption(String optionName, Path path) {
        try {
            if (!Files.exists(path)) {
                return null;
            }
        } catch (StackOverflowError e) {
            Julti.log(Level.ERROR, "Error reading standardoptions.txt! Press Plugins > Open Standard Manager > Yes to try and fix your standardoptions.txt.");
            return null;
        }

        String out;
        try {
            out = FileUtil.readString(path).trim();
        } catch (IOException e) {
            // This should never be reached
            return null;
        }

        if (!new File(out).exists()) {
            Julti.log(Level.ERROR, path + " contains an invalid character or path.");
            return null;
        }

        if (!out.contains("\n")) {
            if (out.endsWith(".txt")) {
                if (out.contains(path.toString())) {
                    Julti.log(Level.ERROR, path + " contains a path to itself! StandardSettings will not work.\r\n" +
                            "To fix this:\r\n" +
                            "- Press Instance Utilities > Close All Instances\r\n" +
                            "- Delete standardoptions.txt on your desktop AND in instance 1's .minecraft/config folder\r\n" +
                            "- Launch and close instance 1\r\n" +
                            "- In Julti, press Plugins > Open Standard Manager > Yes > OK > Yes > Apply to All Instances, to fix your standardoptions.txt"
                    );
                    return null;
                }
                return getStandardOption(optionName, Paths.get(out));
            }
        }

        return getOptionFromString(optionName, out);
    }

    public static Integer getKey(Path instancePath, String optionsValue, boolean pre113) {

        if (pre113) {
            return getKeyPre113(instancePath, optionsValue);
        }

        String out = tryGetOption(instancePath, optionsValue, true);
        Integer vkFromMCTranslation = MCKeyUtil.getVkFromMCTranslation(out);
        if (vkFromMCTranslation == null && out != null) {
            // out != null, meaning there is a value there, but the value isn't valid because it doesn't match any existing keys
            Julti.log(Level.WARN, "INVALID KEY IN OPTIONS: " + out);
        } else if (vkFromMCTranslation != null) {
            return vkFromMCTranslation;
        }


        // Try again without standard settings
        out = tryGetOption(instancePath, optionsValue, false);
        vkFromMCTranslation = MCKeyUtil.getVkFromMCTranslation(out);
        if (vkFromMCTranslation == null && out != null) {
            // out != null, meaning there is a value there, but the value isn't valid because it doesn't match any existing keys
            Julti.log(Level.WARN, "INVALID KEY IN OPTIONS: " + out);
        }
        return vkFromMCTranslation; // null is a valid return value
    }

    private static Integer getKeyPre113(Path instancePath, String optionsValue) {
        String out = tryGetOption(instancePath, optionsValue, true);
        Integer vkFromLWJGL = MCKeyUtil.getVkFromLWJGL(out);
        if (vkFromLWJGL == null && out != null) {
            // out != null, meaning there is a value there, but the value isn't valid because it doesn't match any existing keys
            Julti.log(Level.WARN, "INVALID KEY IN OPTIONS: " + out);
        } else if (vkFromLWJGL != null) {
            return vkFromLWJGL;
        }


        // Try again without standard settings
        out = tryGetOption(instancePath, optionsValue, false);
        vkFromLWJGL = MCKeyUtil.getVkFromLWJGL(out);
        if (vkFromLWJGL == null && out != null) {
            // out != null, meaning there is a value there, but the value isn't valid because it doesn't match any existing keys
            Julti.log(Level.WARN, "INVALID KEY IN OPTIONS: " + out);
        }
        return vkFromLWJGL; // null is a valid return value
    }

}