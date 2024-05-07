package xyz.duncanruns.julti.affinity;


import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.instance.InstanceState;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.instance.StateTracker;
import xyz.duncanruns.julti.management.InstanceManager;
import xyz.duncanruns.julti.resetting.ResetHelper;
import xyz.duncanruns.julti.util.SleepUtil;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

public final class AffinityManager {
    private static final List<Supplier<Boolean>> LOCK_CONDITIONS = new CopyOnWriteArrayList<>();
    public static final int AVAILABLE_THREADS = Runtime.getRuntime().availableProcessors();

    private AffinityManager() {
    }

    public static void registerLockCondition(Supplier<Boolean> lockCondition) {
        LOCK_CONDITIONS.add(lockCondition);
    }

    public static boolean isEnabled() {
        return LOCK_CONDITIONS.stream().noneMatch(Supplier::get) && JultiOptions.getJultiOptions().useAffinity;
    }

    public static void tick() {
        if (isEnabled()) {
            ping();
        } else {
            release();
        }
    }

    public static void ping() {
        if (isEnabled()) {
            setAffinityForAllInstances();
        }
    }

    private static void setAffinityForAllInstances() {
        InstanceManager instanceManager = InstanceManager.getInstanceManager();
        JultiOptions options = JultiOptions.getJultiOptions();
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
                        || stateTracker.isCurrentState(InstanceState.GENERATING)
                        || instance.isResetPressed()) {
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
        InstanceManager.getInstanceManager().getInstances().forEach(i -> setAffinity(i, AVAILABLE_THREADS));
    }

    public static void ping(int delay) {
        if (delay == 0) {
            ping();
        } else {
            new Thread(() -> {
                SleepUtil.sleep(delay);
                Julti.doLater(AffinityManager::ping);
            }).start();
        }
    }

    public static void jumpPlayingAffinity(MinecraftInstance instance) {
        if (isEnabled()) {
            setAffinity(instance, JultiOptions.getJultiOptions().threadsPlaying);
        }
    }

    public static void jumpPrePreviewAffinity(MinecraftInstance instance) {
        if (isEnabled()) {
            setAffinity(instance, JultiOptions.getJultiOptions().threadsPrePreview);
        }
    }

    @Deprecated
    public static void pause() {
    }

    @Deprecated
    public static void unpause() {
    }

    @Deprecated
    public static void start() {
    }

    @Deprecated
    public static void stop() {
    }
}
