package xyz.duncanruns.julti.util;

import com.google.common.net.UrlEscapers;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class GrabUtil {
    private static final Gson GSON = new Gson();

    private GrabUtil() {
    }

    public static String grab(String origin) throws IOException {
        return IOUtils.toString(new BufferedInputStream(new URL(origin).openStream()), StandardCharsets.UTF_8);
    }

    public static JsonObject grabJson(String origin) throws IOException, JsonSyntaxException {
        return GSON.fromJson(grab(origin), JsonObject.class);
    }
}
