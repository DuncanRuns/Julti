package xyz.duncanruns.julti.util;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;

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
            if (line.startsWith(optionName + ":")) {
                String[] optionKeyValArr = line.split(":");
                if (optionKeyValArr.length < 2) {
                    continue;
                }
                return optionKeyValArr[1];
            }
        }
        return null;
    }

    public static String tryGetOption(Path instancePath, String optionName, boolean tryUseSS) {

        // This should prevent any crazy out of pocket bullshits like 1 in a million parsing error situations
        try {
            return getOption(instancePath, optionName, tryUseSS);
        } catch (Exception ignored) {
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
            // This should never be reached
            return null;
        }

        return getOptionFromString(optionName, out).trim();
    }

    public static String tryGetStandardOption(Path instancePath, String optionName) {
        try {
            return getStandardOption(instancePath, optionName);
        } catch (Exception ignored) {
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
        } catch (NumberFormatException ignored) {
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
        if (!Files.exists(path)) {
            return null;
        }

        String out;
        try {
            out = FileUtil.readString(path).trim();
        } catch (IOException e) {
            // This should never be reached
            return null;
        }

        if (!out.contains("\n")) {
            if (out.endsWith(".txt")) {
                return getStandardOption(optionName, Paths.get(out));
            }
        }

        return getOptionFromString(optionName, out);
    }

    public static Integer getKey(Path instancePath, String keybindingTranslation) {
        String out = tryGetOption(instancePath, keybindingTranslation, true);
        if (out != null) {
            Integer vkFromMCTranslation = McKeyUtil.getVkFromMCTranslation(out);
            if (vkFromMCTranslation == null) {
                // out != null, meaning there is a value there, but the value isn't valid because it doesn't match any existing keys
                Julti.log(Level.WARN, "INVALID KEY IN OPTIONS: " + out);
            } else {
                return vkFromMCTranslation;
            }
        }

        // Try again without standard settings
        out = tryGetOption(instancePath, keybindingTranslation, false);
        Integer vkFromMCTranslation = McKeyUtil.getVkFromMCTranslation(out);
        if (out != null && vkFromMCTranslation == null) {
            // out != null, meaning there is a value there, but the value isn't valid because it doesn't match any existing keys
            Julti.log(Level.WARN, "INVALID KEY IN OPTIONS: " + out);
        }
        return vkFromMCTranslation; // null is a valid return value
    }

}