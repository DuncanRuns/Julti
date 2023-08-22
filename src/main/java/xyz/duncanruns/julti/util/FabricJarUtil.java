package xyz.duncanruns.julti.util;

import com.google.gson.Gson;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class FabricJarUtil {
    private static final Gson GSON = new Gson();

    private FabricJarUtil() {
    }

    public static List<FabricJarInfo> getAllJarInfos(Path instancePath) throws IOException {
        // List files in mod folder -> filter for .jar -> map to jar infos -> return
        try (Stream<Path> list = Files.list(instancePath.resolve("mods").toAbsolutePath())) {
            return list.filter(path -> path.getFileName().toString().endsWith(".jar")).map(path -> {
                try {
                    return getJarInfo(path);
                } catch (IOException e) {
                    Julti.log(Level.WARN, "Invalid jar " + path.getFileName() + " found in " + instancePath + ". Exception below:\n" + ExceptionUtil.toDetailedString(e));
                    return null;
                }
            }).filter(Objects::nonNull).collect(Collectors.toList());
        }
    }

    public static FabricJarInfo getJarInfo(List<FabricJarInfo> infos, String id) {
        // Filter for any jars with the correct id
        return infos.stream().filter(info -> id.equals(info.id)).findAny().orElse(null);
    }

    private static FabricJarInfo getJarInfo(Path jarPath) throws IOException {
        return GSON.fromJson(getJarFMJContents(jarPath), FabricJarInfo.class);
    }

    @SuppressWarnings("all") //Suppress the redundant cast warning which resolves an ambiguous case
    private static String getJarFMJContents(Path jarPath) throws IOException {
        try (FileSystem fs = FileSystems.newFileSystem(jarPath, (ClassLoader) null)) {
            Path jsonFilePath = fs.getPath("fabric.mod.json");
            byte[] jsonData = Files.readAllBytes(jsonFilePath);
            return new String(jsonData, StandardCharsets.UTF_8);
        }
    }

    public static class FabricJarInfo {
        public String name = null;
        public String id = null;
        public String version = null;

        @Override
        public String toString() {
            return String.format("%s v%s (ID: %s)", this.name, this.version, this.id);
        }
    }
}
