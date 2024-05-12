package xyz.duncanruns.julti.util;

import org.apache.commons.io.IOUtils;
import org.kohsuke.github.AbuseLimitHandler;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.RateLimitHandler;
import xyz.duncanruns.julti.script.ScriptManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

public class OfficialScriptsUtil {
    public static Set<String> retrieveOfficialScriptFileNames() throws IOException {
        return GitHubUtil.getGitHub()
                .getRepository("DuncanRuns/Julti-Scripts")
                .getDirectoryContent("scripts")
                .stream()
                .map(GHContent::getName)
                .filter(s -> s.endsWith(".txt") || s.endsWith(".lua"))
                .collect(Collectors.toSet());
    }

    public static void downloadScript(String scriptFileName) throws IOException {
        String scriptName = scriptFileName.substring(0, scriptFileName.length() - 4);
        ScriptManager.writeScript(scriptName, IOUtils.toString(
                GitHubUtil.getGitHub()
                        .getRepository("DuncanRuns/Julti-Scripts")
                        .getFileContent("scripts/" + scriptFileName)
                        .read(),
                StandardCharsets.UTF_8
        ), scriptFileName.endsWith(".txt"));
        ScriptManager.reload();
    }
}
