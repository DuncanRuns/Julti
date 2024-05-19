package xyz.duncanruns.julti.cancelrequester;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CancelRequesterManager {
    private List<NamedCancelRequester> requesters = new ArrayList<>();

    private void update() {
        // Clear list of any stale requesters
        this.requesters.removeIf(CancelRequester::isCancelRequested);
    }

    public synchronized int cancelAll() {
        this.update();
        int out = this.requesters.size();
        this.requesters.forEach(CancelRequester::cancel);
        this.requesters = new LinkedList<>();
        return out;
    }

    public synchronized CancelRequester createNew(String name) {
        this.update();
        // Add the new one and return
        NamedCancelRequester cr = new NamedCancelRequester(name);
        this.requesters.add(cr);
        return cr;
    }

    public synchronized boolean isActive(String name) {
        this.update();
        return this.requesters.stream().anyMatch(r -> r.getName().equals(name));
    }

    public synchronized void remove(String name) {
        this.update();
        this.requesters.removeIf(r -> r.getName().equals(name));
    }

    public synchronized Set<String> getAllActive() {
        this.update();
        return this.requesters.stream().map(NamedCancelRequester::getName).collect(Collectors.toSet());
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
