package xyz.duncanruns.julti.messages;

/**
 * A "queueable" message object meant for being added to queues for later processing.
 */
public abstract class QMessage {
    private boolean processed = false;
    private boolean failed = false;

    public void markProcessed() {
        this.processed = true;
    }

    public boolean isProcessed() {
        return this.processed;
    }

    public void markFailed() {
        this.failed = true;
    }

    public boolean hasFailed() {
        return this.failed;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
                "processed=" + this.processed +
                ", failed=" + this.failed +
                '}';
    }
}
