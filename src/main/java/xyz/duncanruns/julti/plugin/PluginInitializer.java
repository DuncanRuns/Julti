package xyz.duncanruns.julti.plugin;

public interface PluginInitializer {
    void initialize();

    default String getMenuButtonName() {
        return "Open Config";
    }

    void onMenuButtonPress();

    default boolean hasMenuButton() {
        return true;
    }

    default boolean showsInPluginsMenu() {
        return true;
    }
}
