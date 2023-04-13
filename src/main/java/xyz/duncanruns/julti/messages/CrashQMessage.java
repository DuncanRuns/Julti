package xyz.duncanruns.julti.messages;

public class CrashQMessage extends QMessage {
    private final Throwable throwable;

    public CrashQMessage(Throwable throwable) {
        this.throwable = throwable;
    }

    public Throwable getThrowable() {
        return this.throwable;
    }
}
