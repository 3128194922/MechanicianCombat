package com.mechaniciancombat.client.rig;

final class ArmorPartPolicy {
    static String part(String kind, String bone, String slot) {
        if (kind.equals("toolbelt")) return bone.equals("chest") && slot.equals("LEGS") ? "body" : null;
        if (kind.equals("electrum")) return switch (bone) {
            case "head" -> slot.equals("HEAD") ? "Head" : null;
            case "chest" -> slot.equals("CHEST") ? "Body" : null;
            case "arm_left" -> slot.equals("CHEST") ? "LeftArm" : null;
            case "arm_right" -> slot.equals("CHEST") ? "RightArm" : null;
            case "leg_left" -> slot.equals("FEET") ? "LeftBoot" : slot.equals("LEGS") ? "LeftLeg" : null;
            case "leg_right" -> slot.equals("FEET") ? "RightBoot" : slot.equals("LEGS") ? "RightLeg" : null;
            default -> null;
        };
        return switch (bone) {
            case "head" -> "head";
            case "chest" -> "body";
            case "arm_left" -> "leftArm";
            case "arm_right" -> "rightArm";
            case "leg_left" -> "leftLeg";
            case "leg_right" -> "rightLeg";
            default -> null;
        };
    }
    private ArmorPartPolicy() {}
}
