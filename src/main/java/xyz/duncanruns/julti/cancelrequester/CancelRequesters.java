package xyz.duncanruns.julti.cancelrequester;

public final class CancelRequesters {
    public static final CancelRequester ALWAYS_CANCEL_REQUESTER = new CancelRequester.FakeCancelRequester(true);
    public static final CancelRequester NEVER_CANCEL_REQUESTER = new CancelRequester.FakeCancelRequester(false);

    private CancelRequesters() {
    }
}
