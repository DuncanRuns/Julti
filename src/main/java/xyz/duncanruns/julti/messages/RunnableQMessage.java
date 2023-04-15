package xyz.duncanruns.julti.messages;

public class RunnableQMessage extends QMessage {
    private final Runnable runnable;

    public RunnableQMessage(Runnable runnable) {
        this.runnable = runnable;
    }

    public Runnable getRunnable() {
        return this.runnable;
    }
}
