package xyz.duncanruns.julti.script;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.util.ExceptionUtil;
import xyz.duncanruns.julti.util.FileUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class CustomizableManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static JsonObject json = new JsonObject();
    private static final Path STORAGE_PATH = ScriptManager.SCRIPTS_FOLDER.resolve("customizable.storage");

    public static void load() {
        if (Files.exists(STORAGE_PATH)) {
            String s;
            try {
                s = FileUtil.readString(STORAGE_PATH);
                json = GSON.fromJson(s, JsonObject.class);
            } catch (IOException e) {
                Julti.log(Level.ERROR, "Failed to load customizable storage: " + ExceptionUtil.toDetailedString(e));
            }
        }
    }

    private static void save() {
        try {
            FileUtil.writeString(STORAGE_PATH, GSON.toJson(json));
        } catch (IOException e) {
            Julti.log(Level.ERROR, "Failed to save customizable storage: " + ExceptionUtil.toDetailedString(e));
        }
    }

    public static CustomizableType getType(String typeString) {
        String[] s = typeString.split(" ");
        if (s.length == 0) {
            return CustomizableType.STRING;
        }
        switch (s[0]) {
            case "int":
                return CustomizableType.INTEGER;
            case "boolean":
                return CustomizableType.BOOLEAN;
            case "float":
                return CustomizableType.FLOAT;
            default:
                return CustomizableType.STRING;
        }
    }

    public static boolean isValid(String typeString, Object value) {
        CustomizableType type = getType(typeString);
        // Check conversion
        try {
            switch (type) {
                case BOOLEAN:
                    Boolean.parseBoolean(value.toString());
                    break;
                case INTEGER:
                    Long.parseLong(value.toString());
                    break;
                case FLOAT:
                    Double.parseDouble(value.toString());
                    break;
            }
        } catch (Exception e) {
            return false;
        }
        // Any other checks?
        if (!type.canHaveBounds) {
            return true;
        }
        // Check bounds
        String[] s = typeString.split(" ");
        if (s.length <= 1) {
            return true;
        }
        String boundString = s[1];
        if (!boundString.contains("..")) {
            // Invalid type lol
            return false;
        }
        if (boundString.equals("..")) {
            return true;
        }
        try {
            String minString = boundString.substring(0, boundString.indexOf(".."));
            double min = minString.isEmpty() ? Double.NEGATIVE_INFINITY : Double.parseDouble(minString);
            String maxString = boundString.substring(boundString.indexOf("..") + 2);
            double max = maxString.isEmpty() ? Double.POSITIVE_INFINITY : Double.parseDouble(maxString);
            double numVal = Double.parseDouble(value.toString());
            return numVal >= min && numVal <= max;
        } catch (Exception e) {
            return false;
        }
    }

    public static void set(String scriptName, String varName, Object val, CustomizableType type) {
        if (!json.has(scriptName)) {
            json.add(scriptName, new JsonObject());
        }
        JsonObject scriptSpace = json.getAsJsonObject(scriptName);
        switch (type) {
            case INTEGER:
                scriptSpace.addProperty(varName, Long.parseLong(val.toString()));
                break;
            case STRING:
                scriptSpace.addProperty(varName, val.toString());
                break;
            case FLOAT:
                scriptSpace.addProperty(varName, Double.parseDouble(val.toString()));
                break;
            case BOOLEAN:
                scriptSpace.addProperty(varName, Boolean.parseBoolean(val.toString()));
                break;
        }
        save();
    }

    public static Optional<Object> get(String scriptName, String varName, CustomizableType type) {
        if (!json.has(scriptName)) {
            return Optional.empty();
        }
        JsonObject scriptSpace = json.getAsJsonObject(scriptName);
        if (!scriptSpace.has(varName)) {
            return Optional.empty();
        }
        JsonElement element = scriptSpace.get(varName);
        switch (type) {
            case FLOAT:
                return Optional.of(element.getAsDouble());
            case INTEGER:
                return Optional.of(element.getAsLong());
            case BOOLEAN:
                return Optional.of(element.getAsBoolean());
            default:
                return Optional.of(element.getAsString());
        }
    }

    public enum CustomizableType {
        INTEGER(true),
        FLOAT(true),
        BOOLEAN(false),
        STRING(false);
        private final boolean canHaveBounds;

        CustomizableType(boolean canHaveBounds) {
            this.canHaveBounds = canHaveBounds;
        }
    }
}
