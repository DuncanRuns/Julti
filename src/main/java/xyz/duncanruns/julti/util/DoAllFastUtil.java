package xyz.duncanruns.julti.util;

import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.management.InstanceManager;

import java.util.List;
import java.util.function.Consumer;

/**
 * Execute operations on a list of objects or all minecraft instances in parallel.
 */
public class DoAllFastUtil {

    public static void doAllFast(Consumer<MinecraftInstance> consumer) {
        List<MinecraftInstance> instances = InstanceManager.getManager().getInstances();
        doAllFast(instances, consumer);
    }

    public static <T> void doAllFast(List<T> items, Consumer<T> consumer) {
        Thread[] threads = new Thread[items.size()];
        int i = 0;
        for (T item : items) {
            Thread thread = threads[i++] = new Thread(() -> consumer.accept(item), "do-fast-util");
            thread.start();
        }
        try {
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
