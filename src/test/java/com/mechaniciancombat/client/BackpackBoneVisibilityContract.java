package com.mechaniciancombat.client;

/** Auto launcher components must remain visible in both idle and target-tracking passes. */
public final class BackpackBoneVisibilityContract {
    public static void main(String[] args) {
        expect("auto_C3", true, false, false, true);
        for (String bone : new String[]{"auto_ejector", "auto_box", "auto_sight", "auto_barrel"}) {
            expect(bone, true, false, true, true);
        }
        expect("auto_C3", true, false, true, false);
        expect("C3", true, false, false, false);
        System.out.println("BackpackBoneVisibilityContract: 7 scenarios passed");
    }

    private static void expect(String name, boolean auto, boolean summon, boolean suppress, boolean expected) {
        boolean actual = BackpackBoneVisibility.draw(name, auto, summon, suppress);
        if (actual != expected) throw new AssertionError(name + " suppressed=" + suppress + " expected=" + expected);
    }
    private BackpackBoneVisibilityContract() {}
}
