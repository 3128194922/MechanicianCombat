package com.mechaniciancombat.client.rig;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.core.util.Vec3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.util.RenderUtils;

import java.util.Map;
import java.util.Set;

/** Compare the bridged player part against GeckoLib's actual bone matrices. */
public final class PlayerPoseTransformContract {
    private static final String[] PATH = {"body", "chest", "arm_left"};
    private static final Map<String, BowgunPlayerAnimatorPose.BonePose> BONES = Map.of(
            "body", new BowgunPlayerAnimatorPose.BonePose(2, 3, 4, .35F, -.5F, .2F, 0, 12, 0),
            "chest", new BowgunPlayerAnimatorPose.BonePose(-1, 2, -3, -.4F, .25F, -.3F, 0, 12, 0),
            "arm_left", new BowgunPlayerAnimatorPose.BonePose(1, -2, 1, .7F, -.2F, .4F, -4, 22, 0));
    private static final Vec3f FALLBACK_POSITION = new Vec3f(5, 2, 0);
    private static final Vec3f FALLBACK_ROTATION = new Vec3f(.2F, -.1F, .15F);

    public static void main(String[] args) {
        check(Set.of("body.position"));
        check(Set.of("arm_left.rotation"));
        check(Set.of("body.rotation", "chest.rotation", "arm_left.rotation"));
        check(Set.of("body.position", "body.rotation", "chest.position", "chest.rotation",
                "arm_left.position", "arm_left.rotation"));
        check(Set.of("body.rotation", "chest.rotation", "arm_left.position"));
        System.out.println("PlayerPoseTransformContract: 5 scenarios passed");
    }

    private static void check(Set<String> channels) {
        PoseStack expected = new PoseStack();
        for (String name : PATH) {
            var p = BONES.get(name);
            GeoBone bone = new GeoBone(null, name, false, 0D, false, false);
            bone.updatePivot(p.pivotX(), p.pivotY(), p.pivotZ());
            if (channels.contains(name + ".position")) {
                bone.setPosX(p.x()); bone.setPosY(p.y()); bone.setPosZ(p.z());
            }
            if (channels.contains(name + ".rotation")) {
                bone.updateRotation(p.rotX(), p.rotY(), p.rotZ());
            }
            RenderUtils.prepMatrixForBone(expected, bone);
        }

        // The vanilla left arm pivots at (5, 2, 0); the corresponding geo pivot is (-4, 22, 0).
        // Compare the actual vanilla part pivot in GeckoLib coordinates, not the geo bone pivot.
        Vector3f pivot = new Vector3f(-FALLBACK_POSITION.getX(), 24 - FALLBACK_POSITION.getY(),
                FALLBACK_POSITION.getZ()).div(16);
        Vector3f displacement = expected.last().pose().transformPosition(new Vector3f(pivot)).sub(pivot).mul(16);
        Vec3f position = BowgunPlayerAnimatorPose.transform(PATH, BONES, channels, TransformType.POSITION, FALLBACK_POSITION);
        assertVector("position " + channels, new Vector3f(position.getX() - FALLBACK_POSITION.getX(),
                position.getY() - FALLBACK_POSITION.getY(), position.getZ() - FALLBACK_POSITION.getZ()),
                toPlayer(displacement));

        Vec3f rotation = BowgunPlayerAnimatorPose.transform(PATH, BONES, channels, TransformType.ROTATION, FALLBACK_ROTATION);
        Quaternionf actual = new Quaternionf().rotationZYX(rotation.getZ(), rotation.getY(), rotation.getX());
        Quaternionf fallback = new Quaternionf().rotationZYX(FALLBACK_ROTATION.getZ(), FALLBACK_ROTATION.getY(), FALLBACK_ROTATION.getX());
        for (Vector3f axis : new Vector3f[]{new Vector3f(1, 0, 0), new Vector3f(0, 1, 0), new Vector3f(0, 0, 1)}) {
            Vector3f basis = new Vector3f(axis);
            if (!channels.contains("arm_left.rotation")) fallback.transform(basis);
            Vector3f reference = toPlayer(expected.last().pose().transformDirection(toPlayer(basis)));
            assertVector("rotation " + channels, actual.transform(new Vector3f(axis)), reference);
        }
    }

    private static Vector3f toPlayer(Vector3f v) { return new Vector3f(-v.x, -v.y, v.z); }
    private static void assertVector(String name, Vector3f actual, Vector3f expected) {
        if (actual.distance(expected) > 1.0E-4F) throw new AssertionError(name + ": expected=" + expected + " actual=" + actual);
    }
    private PlayerPoseTransformContract() {}
}
