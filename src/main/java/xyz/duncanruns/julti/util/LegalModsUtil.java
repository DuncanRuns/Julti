package xyz.duncanruns.julti.util;

import com.google.gson.JsonElement;
import org.apache.logging.log4j.Level;
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
        return GrabUtil.grabJson("https://raw.githubusercontent.com/tildejustin/mcsr-meta/schema-6/mods.json")
                .getAsJsonArray("mods").asList().stream()
                .map(JsonElement::getAsJsonObject)
                .map(j -> j.get("modid").getAsString().replaceAll("[-_]", "")).collect(Collectors.toSet());
    }
}
