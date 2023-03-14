package xyz.duncanruns.julti;

import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import xyz.duncanruns.julti.instance.MinecraftInstance;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class AffinityManager {
    public static final int AVAILABLE_THREADS = Runtime.getRuntime().availableProcessors();
    private static ScheduledExecutorService EXECUTOR;

    private AffinityManager() {}

    public static void start(Julti julti) {
        stop();
        ScheduledExecutorService executorService = getExecutor();
        executorService.scheduleAtFixedRate(() -> tick(julti), 100, 100, TimeUnit.MILLISECONDS);
    }

    public static void stop() {
        if (EXECUTOR == null || EXECUTOR.isShutdown() || EXECUTOR.isTerminated()) return;
        EXECUTOR.shutdown();
        EXECUTOR = null;
    }

    public static ScheduledExecutorService getExecutor() {
        if (EXECUTOR == null || EXECUTOR.isShutdown() || EXECUTOR.isTerminated()) {
            EXECUTOR = Executors.newSingleThreadScheduledExecutor();
        }
        return EXECUTOR;
    }

    public static void tick(Julti julti) {
        JultiOptions options = JultiOptions.getInstance();
        List<MinecraftInstance> instances = julti.getInstanceManager().getInstances();
        MinecraftInstance selectedInstance = julti.getInstanceManager().getSelectedInstance();
        List<MinecraftInstance> lockedInstances = julti.getResetManager().getLockedInstances();
        if (selectedInstance != null) {
            setAffinity(selectedInstance, options.threadsPlaying);
            for (MinecraftInstance instance : instances) {
                if (!instance.equals(selectedInstance)) setAffinity(instance, options.threadsBackground);
            }
            return;
        }
        for (MinecraftInstance instance : instances) {
            if (lockedInstances.contains(instance)) {
                setAffinity(instance, options.threadsLocked);
            } else if (!(instance.isWorldLoaded() || instance.isPreviewLoaded())) {
                setAffinity(instance, options.threadsPrePreview);
            } else if (instance.wasPreviewInLastMillis(options.affinityBurst)) {
                setAffinity(instance, options.threadsStartPreview);
            } else if (instance.isPreviewLoaded()) {
                setAffinity(instance, options.threadsPreview);
            } else {
                setAffinity(instance, options.threadsWorldLoaded);
            }
        }
    }

    public static void setAffinity(MinecraftInstance instance, int threads) {
        WinNT.HANDLE h = Kernel32.INSTANCE.OpenProcess(0x0200, false, instance.getPid());
        Kernel32.INSTANCE.SetProcessAffinityMask(h, new BaseTSD.ULONG_PTR(getBitMask(threads)));
        Kernel32.INSTANCE.CloseHandle(h);
    }

    public static long getBitMask(int threads) {
        return (1L << threads) - 1;
    }

    public static void release(Julti julti) {
        julti.getInstanceManager().getInstances().forEach(i -> setAffinity(i, AVAILABLE_THREADS));
    }

    public static void ping(Julti julti) {
        getExecutor().execute(() -> tick(julti));
    }

    public static void ping(Julti julti, int delay) {
        if (delay == 0) {
            ping(julti);
        } else {
            getExecutor().schedule(() -> tick(julti), delay, TimeUnit.MILLISECONDS);
        }
    }
}
