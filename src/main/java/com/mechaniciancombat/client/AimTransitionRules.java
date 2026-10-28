package com.mechaniciancombat.client;

/** Held-use behavior as the shoulder camera changes ownership. */
final class AimTransitionRules {
    static boolean requiresRelease(int previous, int current, boolean rolling) {
        if (current == 0 || current == previous) return false;
        if (rolling) return false;
        if ((previous == 2 || previous == 4 || previous == 5) && current == 12) return false;
        return true;
    }
    private AimTransitionRules() {}
}
