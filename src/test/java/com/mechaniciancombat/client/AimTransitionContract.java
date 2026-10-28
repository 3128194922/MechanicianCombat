package com.mechaniciancombat.client;

public final class AimTransitionContract {
    public static void main(String[] args) {
        check(!AimTransitionRules.requiresRelease(12, 4, true), "entering roll must not require releasing use");
        check(!AimTransitionRules.requiresRelease(12, 2, true), "entering roll boost must not require releasing use");
        check(!AimTransitionRules.requiresRelease(12, 1, true), "sprinting while rolling must not require releasing use");
        check(!AimTransitionRules.requiresRelease(5, 12, true), "using the gun in recovery must not cancel held aim");
        check(AimTransitionRules.requiresRelease(12, 2, false), "unrelated camera transitions retain their behavior");
        check(!AimTransitionRules.requiresRelease(5, 12, false), "leaving recovery must not cancel held aim");
        check(!AimTransitionRules.requiresRelease(2, 12, false), "boost expiry must not cancel held aim");
        check(AimTransitionRules.requiresRelease(12, 9, false), "overheat must retain its held-key transition rule");
        check(AimTransitionRules.requiresRelease(12, 7, false), "sliding must retain its held-key transition rule");
        check(!AimTransitionRules.requiresRelease(12, 12, false), "unchanged state must not release aim");
    }
    private static void check(boolean result, String message) {
        if (!result) throw new AssertionError(message);
    }
}
