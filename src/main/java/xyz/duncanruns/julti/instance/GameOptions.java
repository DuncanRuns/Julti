package xyz.duncanruns.julti.instance;

import xyz.duncanruns.julti.util.FabricJarUtil.FabricJarInfo;

import java.lang.reflect.Field;
import java.util.List;

public class GameOptions {
    public Integer createWorldKey = null;
    public Integer leavePreviewKey = null;
    public Integer fullscreenKey = null;
    public Integer chatKey = null;
    public Boolean pauseOnLostFocus = null;
    public Boolean f1SS = null;
    public Boolean f3PauseOnWorldLoad = null;
    public Boolean hasSeedQueue = null;

    public List<FabricJarInfo> jars;

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder("Game Options{");
        for (Field field : this.getClass().getFields()) {
            try {
                out.append("\n    ").append(field.getName()).append(" = ").append(field.get(this));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        out.append("\n}");
        return out.toString();
    }
}