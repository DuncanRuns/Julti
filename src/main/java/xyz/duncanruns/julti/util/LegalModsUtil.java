package xyz.duncanruns.julti.util;

import org.apache.logging.log4j.Level;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GitHub;
import xyz.duncanruns.julti.Julti;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class LegalModsUtil {
    private static Set<String> legalMods = Arrays.stream(new String[]{
            // Start with default legal mods in case updating fails
            "anchiale", "antigone", "antiresourcereload", "atum", "biomethreadlocalfix", "chunkcacher", "chunkumulator", "dynamicfps", "extra-options", "fabricproxy-lite", "fastreset", "forceport", "krypton", "lazydfu", "lazystronghold", "lithium", "motiono", "no-paus", "optifabric", "phosphor", "setspawnmod", "sleepbackground", "sodium", "sodiummac", "speedrunigt", "standardsettings", "starlight", "state-output", "statsperworld", "tabfocus", "voyager", "worldpreview", "z-buffer-fog"
    }).collect(Collectors.toSet());
    private static boolean updated = false;

    public static void updateLegalMods() {
        try {
            legalMods = obtainLegalMods();
        } catch (Exception e) {
            Julti.log(Level.WARN, "Failed to obtain legal mod ids from github!");
        }
        updated = true;
    }

    public static boolean hasUpdated() {
        return updated;
    }

    public static Set<String> getLegalMods() {
        return legalMods;
    }


    private static Set<String> obtainLegalMods() throws IOException {
        return GitHub.connectAnonymously().getRepository("Minecraft-Java-Edition-Speedrunning/legal-mods").getDirectoryContent("legal-mods").stream().map(GHContent::getName).map(String::trim).collect(Collectors.toSet());
    }
}
