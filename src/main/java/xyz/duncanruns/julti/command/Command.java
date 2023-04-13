package xyz.duncanruns.julti.command;

import xyz.duncanruns.julti.cancelrequester.CancelRequester;
import xyz.duncanruns.julti.cancelrequester.CancelRequesters;

public abstract class Command {

    public abstract String helpDescription();

    public abstract int getMinArgs();

    public abstract int getMaxArgs();

    public abstract String getName();

    public final void run(String[] args) {
        this.run(args, CancelRequesters.NEVER_CANCEL_REQUESTER);
    }

    public abstract void run(String[] args, CancelRequester cancelRequester);
}
