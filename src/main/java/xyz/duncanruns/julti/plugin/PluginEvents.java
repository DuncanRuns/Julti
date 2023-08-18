package xyz.duncanruns.julti.plugin;

import xyz.duncanruns.julti.instance.MinecraftInstance;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public final class PluginEvents {

    private static final HashMap<RunnableEventType, List<Runnable>> RUNNABLE_EVENT_MAP = new HashMap<>();
    private static final HashMap<InstanceEventType, List<Consumer<MinecraftInstance>>> INSTANCE_EVENT_MAP = new HashMap<>();

    static {
        for (RunnableEventType value : RunnableEventType.values()) {
            RUNNABLE_EVENT_MAP.put(value, new LinkedList<>());
        }
        for (InstanceEventType value : InstanceEventType.values()) {
            INSTANCE_EVENT_MAP.put(value, new LinkedList<>());
        }
    }

    private PluginEvents() {
    }

    public static void runEvents(RunnableEventType eventType) {
        RUNNABLE_EVENT_MAP.get(eventType).forEach(Runnable::run);
    }

    public static void runEvents(InstanceEventType eventType, MinecraftInstance instance) {
        INSTANCE_EVENT_MAP.get(eventType).forEach(c -> c.accept(instance));
    }

    public static void registerRunnableEvent(RunnableEventType eventType, Runnable runnable) {
        RUNNABLE_EVENT_MAP.get(eventType).add(runnable);
    }

    public static void registerInstanceEvent(InstanceEventType eventType, Consumer<MinecraftInstance> instanceConsumer) {
        INSTANCE_EVENT_MAP.get(eventType).add(instanceConsumer);
    }

    public enum RunnableEventType {
        // Runs at the start of the main loop tick
        START_TICK,
        // Runs at the end of the main loop tick
        END_TICK,
        // Runs every reload, which is every profile change including the first load
        RELOAD,
        // Runs when Julti is shutting down
        STOP,
        // Runs every time the wall is activated
        WALL_ACTIVATE,
        // Runs when all instances are found
        ALL_INSTANCES_FOUND
    }

    public enum InstanceEventType {
        // Runs every time the instance is switched to
        ACTIVATE,
        // Runs every time the instance is reset
        RESET,
        // Runs every time the instance's state changes
        STATE_CHANGE
    }
}
