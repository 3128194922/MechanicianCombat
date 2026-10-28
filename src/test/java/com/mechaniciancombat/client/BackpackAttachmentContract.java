package com.mechaniciancombat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.util.RenderUtils;

import java.util.List;
import java.util.Map;

/** Vanilla and GeckoLib must orient the same mechanical arm in the same direction. */
public final class BackpackAttachmentContract {
    public static void main(String[] args) {
        for (float lean : new float[]{0, 0.5F}) {
            ModelPart body = new ModelPart(List.of(), Map.of());
            body.setPos(0, lean == 0 ? 0 : 3.2F, 0);
            body.setRotation(lean, .25F * lean, -.2F * lean);

            PoseStack actual = new PoseStack();
            actual.scale(-1, -1, 1); // LivingEntityRenderer's player model basis
            CombatBackpackCurioRenderer.applyBodyAttachment(actual, body);
            PoseStack geoArm = new PoseStack();
            GeoBone arm = new GeoBone(null, "auto_C1", false, 0D, false, false);
            arm.updatePivot(8, -1, 1);
            arm.updateRotation(.4F, -.3F, .7F);
            RenderUtils.prepMatrixForBone(actual, arm);
            RenderUtils.prepMatrixForBone(geoArm, arm);
            Quaternionf bodyRotation = new Quaternionf().rotationZYX(body.zRot, body.yRot, body.xRot);
            for (Vector3f point : new Vector3f[]{new Vector3f(), new Vector3f(.2F, .3F, -.1F),
                    new Vector3f(-.1F, .05F, .4F)}) {
                Vector3f geo = geoArm.last().pose().transformPosition(new Vector3f(point));
                // The asset spans x=0..8, y=-8..0; center it on the torso and
                // put its upper edge at the torso pivot before applying body rotation.
                Vector3f expected = bodyRotation.transform(new Vector3f(.25F - geo.x, -geo.y, .125F + geo.z));
                expected.add(body.x / 16F, body.y / 16F, body.z / 16F);
                expected.set(-expected.x, -expected.y, expected.z);
                Vector3f result = actual.last().pose().transformPosition(new Vector3f(point));
                if (result.distance(expected) > 1.0E-5F) {
                    throw new AssertionError("lean=" + lean + " expected=" + expected + " actual=" + result);
                }
            }
        }
        System.out.println("BackpackAttachmentContract: standing and crouching transforms passed");
    }
    private BackpackAttachmentContract() {}
}
