package com.mechaniciancombat.client;

public final class VentEmissionTrackerContract {
    public static void main(String[] args) {
        VentEmissionTracker tracker = new VentEmissionTracker();
        check(tracker.update(7, true, false) == VentEmissionTracker.Event.WAIT,
                "an unavailable locator must keep the burst pending");
        check(tracker.update(7, true, true) == VentEmissionTracker.Event.START,
                "first valid locator must produce initial burst");
        check(tracker.update(7, true, true) == VentEmissionTracker.Event.ACTIVE,
                "continuous vent must not repeat initial burst");
        check(tracker.update(8, true, true) == VentEmissionTracker.Event.START,
                "different players need independent bursts");
        tracker.update(7, false, false);
        check(tracker.update(7, true, true) == VentEmissionTracker.Event.START,
                "new vent cycle must produce another burst");
    }
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
