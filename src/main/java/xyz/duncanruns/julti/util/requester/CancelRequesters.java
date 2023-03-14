package xyz.duncanruns.julti.util.requester;

public class CancelRequesters {
    public static final CancelRequester ALWAYS_CANCEL_REQUESTER = new CancelRequester.FakeCancelRequester(true);
    public static final CancelRequester NEVER_CANCEL_REQUESTER = new CancelRequester.FakeCancelRequester(false);
}
