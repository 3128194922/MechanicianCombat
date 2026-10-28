package com.mechaniciancombat.client;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;

/** The aim bone and its children must use the same orientation as the separately rendered turret. */
public final class BackpackAimRotationContract {
    public static void main(String[] args) {
        for (Quaternionf target : new Quaternionf[]{
                new Quaternionf().rotationZYX(.2F, -.35F, .6F),
                new Quaternionf().rotationTo(new Vector3f(0, 0, -1), new Vector3f(.5F, .3F, -.8F).normalize())}) {
            GeoBone bone = new GeoBone(null, "C3", false, 0D, false, false);
            BackpackAimRotation.apply(bone, target);
            Quaternionf actual = new Quaternionf().rotationZYX(bone.getRotZ(), bone.getRotY(), bone.getRotX());
            for (Vector3f axis : new Vector3f[]{new Vector3f(1, 0, 0), new Vector3f(0, 1, 0), new Vector3f(0, 0, 1)}) {
                Vector3f expected = target.transform(new Vector3f(axis));
                Vector3f result = actual.transform(new Vector3f(axis));
                if (result.distance(expected) > 1.0E-4F) {
                    throw new AssertionError("turret orientation expected=" + expected + " actual=" + result);
                }
            }
        }
        System.out.println("BackpackAimRotationContract: 2 orientations passed");
    }
    private BackpackAimRotationContract() {}
}
