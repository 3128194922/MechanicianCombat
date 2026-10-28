package com.mechaniciancombat.client;

/** Filters launcher geometry for the current backpack attachment and render pass. */
final class BackpackBoneVisibility {
    static boolean draw(String name, boolean auto, boolean summon, boolean suppressC3) {
        if (("C3".equals(name) || "auto_C3".equals(name)) && suppressC3) return false;
        return !(auto || summon) || !("C1".equals(name) || "C2".equals(name) || "C3".equals(name));
    }
    private BackpackBoneVisibility() {}
}
