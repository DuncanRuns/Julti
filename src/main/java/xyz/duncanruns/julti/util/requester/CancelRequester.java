package xyz.duncanruns.julti.util.requester;

public class CancelRequester {
    private volatile boolean cancelRequested = false;

    public boolean isCancelRequested() {
        return this.cancelRequested;
    }

    public boolean cancel() {
        if (!this.cancelRequested) {
            return this.cancelRequested = true;
        }
        return false;
    }

    static class FakeCancelRequester extends CancelRequester {
        private final boolean cancel;

        FakeCancelRequester(boolean cancel) {
            this.cancel = cancel;
        }

        @Override
        public boolean isCancelRequested() {
            return this.cancel;
        }

        @Override
        public boolean cancel() {
            return false;
        }
    }
}
