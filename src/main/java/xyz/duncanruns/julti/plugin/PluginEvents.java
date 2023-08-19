package xyz.duncanruns.julti.plugin;

import xyz.duncanruns.julti.instance.MinecraftInstance;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public final class PluginEvents {

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
        ALL_INSTANCES_FOUND;

        private final List<Runnable> runnables = new LinkedList<>();

        public void register(Runnable runnable) {
            this.runnables.add(runnable);
        }

        public void runAll() {
            this.runnables.forEach(Runnable::run);
        }
    }

    public enum InstanceEventType {
        // Runs every time the instance is switched to
        ACTIVATE,
        // Runs every time the instance is reset
        RESET,
        // Runs every time the instance's state changes
        STATE_CHANGE;


        private final List<Consumer<MinecraftInstance>> consumers = new LinkedList<>();

        public void register(Consumer<MinecraftInstance> consumer) {
            this.consumers.add(consumer);
        }

        public void runAll(MinecraftInstance instance) {
            this.consumers.forEach(c -> c.accept(instance));
        }
    }
}
