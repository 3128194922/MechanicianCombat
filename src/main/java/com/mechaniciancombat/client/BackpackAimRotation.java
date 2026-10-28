package com.mechaniciancombat.client;

import org.joml.Quaternionf;
import software.bernie.geckolib.cache.object.GeoBone;

final class BackpackAimRotation {
    static void apply(GeoBone bone, Quaternionf rotation) {
        Quaternionf q = new Quaternionf(rotation).normalize();
        float x = (float) Math.atan2(2.0F * (q.w * q.x + q.y * q.z),
                1.0F - 2.0F * (q.x * q.x + q.y * q.y));
        float y = (float) Math.asin(Math.max(-1.0F, Math.min(1.0F,
                2.0F * (q.w * q.y - q.z * q.x))));
        float z = (float) Math.atan2(2.0F * (q.w * q.z + q.x * q.y),
                1.0F - 2.0F * (q.y * q.y + q.z * q.z));
        // GeckoLib composes rotations in Z-Y-X order and expects radians.
        bone.updateRotation(x, y, z);
    }
    private BackpackAimRotation() {}
}
