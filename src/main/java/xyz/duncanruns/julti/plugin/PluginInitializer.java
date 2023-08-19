package xyz.duncanruns.julti.plugin;

public interface PluginInitializer {
    void initialize();

    default String getMenuButtonName() {
        return "Open Config";
    }

    void onMenuButtonPress();
}
