package xyz.duncanruns.julti.instance;

import xyz.duncanruns.julti.util.FabricJarUtil.FabricJarInfo;

import java.util.List;

public class GameOptions {
    public Integer createWorldKey = null;
    public Integer leavePreviewKey = null;
    public Integer fullscreenKey = null;
    public Integer chatKey = null;
    public Boolean pauseOnLostFocus = null;

    public List<FabricJarInfo> jars;
}