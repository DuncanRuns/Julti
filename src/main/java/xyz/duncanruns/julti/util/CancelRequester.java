package xyz.duncanruns.julti.util;

public class CancelRequester {
    public static final CancelRequester ALWAYS_CANCEL_REQUESTER = new FakeCancelRequester(true);
    public static final CancelRequester NEVER_CANCEL_REQUESTER = new FakeCancelRequester(false);
    private volatile boolean cancelRequested = false;

    public boolean isCancelRequested() {
        return cancelRequested;
    }

    public boolean cancel() {
        if (!cancelRequested) {
            cancelRequested = true;
            return true;
        }
        return false;
    }

    private static class FakeCancelRequester extends CancelRequester {
        private final boolean cancel;

        private FakeCancelRequester(boolean cancel) {
            this.cancel = cancel;
        }

        @Override
        public boolean isCancelRequested() {
            return cancel;
        }

        @Override
        public boolean cancel() {
            return false;
        }
    }
}
