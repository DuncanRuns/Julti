package xyz.duncanruns.julti.cancelrequester;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class CancelRequesterManager {
    private List<NamedCancelRequester> requesters = new ArrayList<>();

    public synchronized void cancelAll() {
        this.requesters.forEach(CancelRequester::cancel);
        this.requesters = new LinkedList<>();
    }

    public synchronized CancelRequester createNew(String name) {
        // Clear list of any stale requesters
        this.requesters.removeIf(CancelRequester::isCancelRequested);
        // Add the new one and return
        NamedCancelRequester cr = new NamedCancelRequester(name);
        this.requesters.add(cr);
        return cr;
    }

    public synchronized boolean isActive(String name) {
        // Clear list of any stale requesters
        this.requesters.removeIf(CancelRequester::isCancelRequested);
        return this.requesters.stream().anyMatch(r -> r.getName().equals(name));
    }

    private static class NamedCancelRequester extends CancelRequester {
        private final String name;

        NamedCancelRequester(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }
    }
}
