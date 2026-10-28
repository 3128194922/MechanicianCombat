package com.mechaniciancombat.client;

import java.util.HashSet;
import java.util.Set;
import java.util.function.IntPredicate;

/** Tracks the first visible emission independently for each player. */
final class VentEmissionTracker {
    enum Event { OFF, WAIT, START, ACTIVE }
    private final Set<Integer> started = new HashSet<>();

    Event update(int playerId, boolean venting, boolean locatorReady) {
        if (!venting) {
            started.remove(playerId);
            return Event.OFF;
        }
        if (!locatorReady) return Event.WAIT;
        return started.add(playerId) ? Event.START : Event.ACTIVE;
    }

    void clear() { started.clear(); }
    void removeIf(IntPredicate predicate) { started.removeIf(id -> predicate.test(id)); }
}
