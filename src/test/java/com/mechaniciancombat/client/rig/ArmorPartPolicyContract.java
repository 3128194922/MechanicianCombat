package com.mechaniciancombat.client.rig;

public final class ArmorPartPolicyContract {
    public static void main(String[] args) {
        expect("Head", ArmorPartPolicy.part("electrum", "head", "HEAD"));
        expect("RightArm", ArmorPartPolicy.part("electrum", "arm_right", "CHEST"));
        expect("LeftBoot", ArmorPartPolicy.part("electrum", "leg_left", "FEET"));
        expect(null, ArmorPartPolicy.part("electrum", "chest", "LEGS"));
        expect("body", ArmorPartPolicy.part("toolbelt", "chest", "LEGS"));
        expect(null, ArmorPartPolicy.part("toolbelt", "leg_left", "LEGS"));
        expect("head", ArmorPartPolicy.part("humanoid", "head", "HEAD"));
        expect("leftArm", ArmorPartPolicy.part("humanoid", "arm_left", "CHEST"));
    }
    private static void expect(String expected, String actual) {
        if (!java.util.Objects.equals(expected, actual)) throw new AssertionError(expected + " != " + actual);
    }
}
