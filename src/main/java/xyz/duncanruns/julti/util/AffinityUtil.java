package xyz.duncanruns.julti.util;


import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import xyz.duncanruns.julti.instance.MinecraftInstance;

import java.util.List;
import java.util.Set;

public final class AffinityUtil {
    private static final int availableThreads = Runtime.getRuntime().availableProcessors();

    private static final int playThreads = availableThreads;
    public static final long playBitMask = (0b1L << playThreads) - 1;

    private static final int highThreads = (int) (availableThreads * 0.9);
    public static final long highBitMask = (0b1L << highThreads) - 1;

    private static final int lockThreads = highThreads;
    public static final long lockBitMask = (0b1L << lockThreads) - 1;

    private static final int midThreads = (int) (availableThreads * 0.7);
    public static final long midBitMask = (0b1L << midThreads) - 1;

    private static final int lowThreads = (int) (availableThreads * 0.5);
    public static final long lowBitMask = (0b1L << lowThreads) - 1;

    private static final int superLowThreads = (int) (availableThreads * 0.2);
    public static final long superLowBitMask = (0b1L << superLowThreads) - 1;

    private AffinityUtil() {
    }

    public static void setPlayingAffinities(MinecraftInstance toPlay, List<MinecraftInstance> instances) {
        for (MinecraftInstance instance : instances) {
            if (toPlay.equals(instance)) setAffinity(instance, playBitMask);
            else {
                if (instance.isWorldLoaded()) setAffinity(instance, superLowBitMask);
                else setAffinity(instance, lowBitMask);
            }
        }
    }

    public static void setAffinity(MinecraftInstance instance, long mask) {
        new Thread(() -> setAffinityInternal(instance, mask)).start();
    }

    private static void setAffinityInternal(MinecraftInstance instance, long mask) {
        WinNT.HANDLE h = Kernel32.INSTANCE.OpenProcess(0x0200, false, instance.getPid());
        Kernel32.INSTANCE.SetProcessAffinityMask(h, new BaseTSD.ULONG_PTR(mask));
        Kernel32.INSTANCE.CloseHandle(h);
    }

    public static void setAffinities(List<MinecraftInstance> instances, Set<MinecraftInstance> lockedInstances) {
        for (MinecraftInstance instance : instances) {
            if (instance.isWorldLoaded() && !lockedInstances.contains(instance)) {
                setAffinity(instance, lowBitMask);
            } else if (!(instance.isWorldLoaded() || instance.isPreviewLoaded())) {
                setAffinity(instance, highBitMask);
            } else if (lockedInstances.contains(instance)) {
                setAffinity(instance, lockBitMask);
            } else if (instance.isPreviewLoaded()) {
                setAffinity(instance, midBitMask);
            }
        }
    }

    public static void main(String[] args) {
        MinecraftInstance instance = new MinecraftInstance(HwndUtil.getAllMinecraftHwnds().get(0));
        setAffinity(instance, 0b1);
    }
}
