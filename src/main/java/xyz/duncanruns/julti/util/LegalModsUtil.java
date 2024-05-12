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
    private static Set<String> legalMods = Arrays.stream(
            // Start with default legal mods in case updating fails
            "anchiale, antigone, antiresourcereload, atum, biomethreadlocalfix, chunkcacher, chunkumulator, dynamicfps, extraoptions, fabricproxylite, fastreset, forceport, krypton, lazydfu, lazystronghold, lithium, motiono, nopaus, optifabric, phosphor, setspawnmod, sleepbackground, sodium, sodiummac, speedrunigt, standardsettings, starlight, stateoutput, statsperworld, tabfocus, voyager, worldpreview, zbufferfog"
                    .split(", ")
    ).collect(Collectors.toSet());
    private static boolean updated = false;

    public static void updateLegalMods() {
        try {
            legalMods = obtainLegalMods();
        } catch (Exception e) {
            Julti.log(Level.WARN, "Failed to obtain legal mod ids from github! (GitHub could be rate limiting you!)");
        }
        updated = true;
    }

    public static boolean hasUpdated() {
        return updated;
    }

    public static boolean isLegalMod(String modid) {
        return legalMods.contains(modid.toLowerCase().replaceAll("[-_]", ""));
    }


    private static Set<String> obtainLegalMods() throws IOException {
        return GitHubUtil.getGitHub().getRepository("Minecraft-Java-Edition-Speedrunning/legal-mods").getDirectoryContent("legal-mods").stream().map(GHContent::getName).map(String::trim).map(s -> s.replaceAll("[-_]", "")).collect(Collectors.toSet());
    }
}
