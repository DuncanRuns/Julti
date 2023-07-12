package xyz.duncanruns.julti.instance;

import java.util.ArrayList;

public class Scheduler {
    private final ArrayList<ScheduledRunnable> scheduledRunnables = new ArrayList<>();

    public void checkSchedule() {
        long currentTime = System.currentTimeMillis();
        for (ScheduledRunnable sr : new ArrayList<>(this.scheduledRunnables)) {
            if (currentTime >= sr.timeToRun) {
                sr.runnable.run();
                this.scheduledRunnables.remove(sr);
            }
        }
    }

    public void schedule(Runnable runnable, int delayMillis) {
        this.scheduledRunnables.add(new ScheduledRunnable(runnable, System.currentTimeMillis() + delayMillis));
    }

    public void clear() {
        this.scheduledRunnables.clear();
    }

    private static class ScheduledRunnable {
        public final Runnable runnable;
        public final long timeToRun;

        private ScheduledRunnable(Runnable runnable, long timeToRun) {
            this.runnable = runnable;
            this.timeToRun = timeToRun;
        }
    }
}
