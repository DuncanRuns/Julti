package xyz.duncanruns.julti.util;

import com.google.common.net.UrlEscapers;
import com.google.gson.JsonElement;
import xyz.duncanruns.julti.script.ScriptManager;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

public class OfficialScriptsUtil {
    public static Set<String> retrieveOfficialScriptFileNames() throws IOException {
        return GrabUtil.grabJson("https://raw.githubusercontent.com/DuncanRuns/Julti-Scripts/main/scripts.json")
                .getAsJsonArray("scripts").asList().stream()
                .map(JsonElement::getAsString)
                .filter(s -> s.endsWith(".txt") || s.endsWith(".lua"))
                .collect(Collectors.toSet());
    }

    public static void downloadScript(String scriptFileName) throws IOException {
        String scriptName = scriptFileName.substring(0, scriptFileName.length() - 4);
        ScriptManager.writeScript(scriptName, GrabUtil.grab("https://raw.githubusercontent.com/DuncanRuns/Julti-Scripts/main/scripts/" + UrlEscapers.urlPathSegmentEscaper().escape(scriptFileName)), scriptFileName.endsWith(".txt"));
        ScriptManager.reload();
    }
}
