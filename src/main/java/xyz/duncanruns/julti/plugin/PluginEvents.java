package xyz.duncanruns.julti.plugin;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public final class PluginEvents {

    private static final HashMap<RunnableEventType, List<Runnable>> EVENT_MAP = new HashMap<>();

    static {
        for (RunnableEventType value : RunnableEventType.values()) {
            EVENT_MAP.put(value, new LinkedList<>());
        }
    }

    private PluginEvents() {
    }

    public static void runEvents(RunnableEventType runnableEventType) {
        EVENT_MAP.get(runnableEventType).forEach(Runnable::run);
    }

    public static void registerRunnableEvent(RunnableEventType runnableEventType, Runnable runnable) {
        EVENT_MAP.get(runnableEventType).add(runnable);
    }

    public enum RunnableEventType {
        // Runs at the start of the main loop tick
        START_TICK,
        // Runs at the end of the main loop tick
        END_TICK,
        // Runs every reload, which is every profile change including the first load
        RELOAD,
        // Runs when Julti is shutting down
        STOP
    }
}
