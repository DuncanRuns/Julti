package xyz.duncanruns.julti.affinity;


import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.instance.InstanceState;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.instance.StateTracker;
import xyz.duncanruns.julti.management.InstanceManager;
import xyz.duncanruns.julti.resetting.ResetHelper;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class AffinityManager {
    private static final Object LOCK = new Object();
    public static final int AVAILABLE_THREADS = Runtime.getRuntime().availableProcessors();
    private static ScheduledExecutorService EXECUTOR = null;
    private static boolean paused = false;


    private AffinityManager() {
    }

    public static void start() {
        stop();
        ScheduledExecutorService executorService = getExecutor();
        executorService.scheduleAtFixedRate(AffinityManager::tick, 100, 100, TimeUnit.MILLISECONDS);
    }

    public static void stop() {
        if (EXECUTOR == null || EXECUTOR.isShutdown() || EXECUTOR.isTerminated()) {
            return;
        }
        EXECUTOR.shutdown();
        EXECUTOR = null;
    }

    public static ScheduledExecutorService getExecutor() {
        if (EXECUTOR == null || EXECUTOR.isShutdown() || EXECUTOR.isTerminated()) {
            EXECUTOR = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("affinity-manager").build());
        }
        return EXECUTOR;
    }

    public static void tick() {
        synchronized (LOCK) {
            if (paused) {
                return;
            }
            setAffinityForAllInstances();
        }
    }

    private static void setAffinityForAllInstances() {
        InstanceManager instanceManager = InstanceManager.getManager();
        JultiOptions options = JultiOptions.getInstance();
        List<MinecraftInstance> instances = instanceManager.getInstances();
        MinecraftInstance selectedInstance = instanceManager.getSelectedInstance();
        List<MinecraftInstance> lockedInstances = ResetHelper.getManager().getLockedInstances();
        if (selectedInstance != null) {
            setAffinity(selectedInstance, options.threadsPlaying);
            for (MinecraftInstance instance : instances) {
                if (!instance.equals(selectedInstance)) {
                    setAffinity(instance, options.threadsBackground);
                }
            }
            return;
        }
        for (MinecraftInstance instance : instances) {
            if (lockedInstances.contains(instance)) {
                setAffinity(instance, options.threadsLocked);
            } else {
                StateTracker stateTracker = instance.getStateTracker();
                if (stateTracker.isCurrentState(InstanceState.WAITING)
                        || stateTracker.isCurrentState(InstanceState.TITLE)
                        || stateTracker.isCurrentState(InstanceState.GENERATING)) {
                    setAffinity(instance, options.threadsPrePreview);
                } else if (System.currentTimeMillis() - instance.getStateTracker().getLastStartOf(InstanceState.PREVIEWING) < options.affinityBurst) {
                    setAffinity(instance, options.threadsStartPreview);
                } else if (stateTracker.isCurrentState(InstanceState.PREVIEWING)) {
                    setAffinity(instance, options.threadsPreview);
                } else {
                    setAffinity(instance, options.threadsWorldLoaded);
                }
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

    public static void release() {
        InstanceManager.getManager().getInstances().forEach(i -> setAffinity(i, AVAILABLE_THREADS));
    }

    public static void ping() {
        getExecutor().execute(AffinityManager::tick);

    }

    public static void ping(int delay) {
        if (delay == 0) {
            ping();
        } else {
            getExecutor().schedule(AffinityManager::tick, delay, TimeUnit.MILLISECONDS);
        }
    }

    public static void jumpAffinity(MinecraftInstance instance) {
        setAffinity(instance, JultiOptions.getInstance().threadsPlaying);
    }

    /**
     * Pause the affinity manager to carry out a specific task, usually instance activation.
     * <p>
     * Should be paired with {@link AffinityManager#unpause()} shortly afterwards.
     */
    public static void pause() {
        synchronized (LOCK) {
            paused = true;
        }
    }

    public static void unpause() {
        synchronized (LOCK) {
            paused = false;
        }
    }
}
