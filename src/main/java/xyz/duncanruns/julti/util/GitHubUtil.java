package xyz.duncanruns.julti.util;

import org.kohsuke.github.AbuseLimitHandler;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.RateLimitHandler;
import xyz.duncanruns.julti.JultiOptions;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class GitHubUtil {
    private static final Path TOKEN_PATH = JultiOptions.getJultiDir().resolve("gh_token.txt");

    private GitHubUtil() {
    }

    public static boolean setToken(String token) {
        try {
            FileUtil.writeString(TOKEN_PATH, token);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Nullable
    private static String get() {
        if (!Files.exists(TOKEN_PATH)) {
            return null;
        }
        try {
            return FileUtil.readString(TOKEN_PATH);
        } catch (IOException e) {
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    public static GitHub getGitHub() throws IOException {
        String token = get();
        if (token == null) {
            return new GitHubBuilder().withAbuseLimitHandler(AbuseLimitHandler.FAIL).withRateLimitHandler(RateLimitHandler.FAIL).build();
        } else {
            return new GitHubBuilder().withOAuthToken(token).withAbuseLimitHandler(AbuseLimitHandler.FAIL).withRateLimitHandler(RateLimitHandler.FAIL).build();
        }
    }
}
