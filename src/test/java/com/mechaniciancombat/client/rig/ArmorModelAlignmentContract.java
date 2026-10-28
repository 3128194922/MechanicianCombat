package com.mechaniciancombat.client.rig;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.util.RenderUtils;

/** Compare armor geometry with the actual six rig cuboids, including animated rotations. */
public final class ArmorModelAlignmentContract {
    public static void main(String[] args) {
        HumanoidModel<LivingEntity> model = new HumanoidModel<>(LayerDefinition.create(
                HumanoidArmorModel.createBodyLayer(CubeDeformation.NONE), 64, 32).bakeRoot());
        check("chest", model.body, -4, 12, -2, 8, 12, 4, 0, 12);
        check("head", model.head, -4, 24, -4, 8, 8, 8, 0, 24);
        check("arm_left", model.leftArm, 4, 12, -2, 4, 12, 4, 4, 22);
        check("arm_right", model.rightArm, -8, 12, -2, 4, 12, 4, -4, 22);
        check("leg_left", model.leftLeg, 0, 0, -2, 4, 12, 4, 2, 12);
        check("leg_right", model.rightLeg, -4, 0, -2, 4, 12, 4, -2, 12);
    }
    private static void check(String name, ModelPart part, float x, float y, float z,
            float width, float height, float depth, float pivotX, float pivotY) {
        float minX = -(x + width) / 16F, minY = y / 16F, minZ = z / 16F;
        float maxX = -x / 16F, maxY = (y + height) / 16F, maxZ = (z + depth) / 16F;
        GeoVertex[] vertices = {new GeoVertex(new Vector3f(minX,minY,minZ),0,0),
                new GeoVertex(new Vector3f(maxX,maxY,maxZ),0,0)};
        GeoCube geo = new GeoCube(new GeoQuad[]{new GeoQuad(vertices,new Vector3f(0,1,0),Direction.UP)},
                Vec3.ZERO, Vec3.ZERO, new Vec3(width,height,depth),0,false);
        ModelPart.Cube cube = part.getRandomCube(RandomSource.create(0));
        for (float angle : new float[]{0, 0.4F}) {
            GeoBone bone = new GeoBone(null, name, false, 0D, false, false);
            bone.updatePivot(-pivotX,pivotY,0);
            bone.setRotX(angle); bone.setRotY(angle / 2); bone.setRotZ(-angle / 3);
            PoseStack expectedPose = new PoseStack();
            RenderUtils.prepMatrixForBone(expectedPose,bone);
            Vector3f expected = expectedPose.last().pose().transformPosition(new Vector3f(minX,minY,minZ));
            PoseStack armorPose = new PoseStack();
            RenderUtils.prepMatrixForBone(armorPose,bone);
            var saved = part.storePose();
            try {
                if (!ArmorModelAlignment.apply(armorPose,geo,cube,part)) throw new AssertionError("alignment unavailable: " + name);
                part.translateAndRotate(armorPose);
                Vector3f actual = armorPose.last().pose().transformPosition(
                        new Vector3f(cube.maxX / 16F,cube.maxY / 16F,cube.minZ / 16F));
                if (actual.distance(expected) > 0.0001F)
                    throw new AssertionError(name + " angle=" + angle + " expected=" + expected + " actual=" + actual);
                Vector3f expectedOpposite = expectedPose.last().pose().transformPosition(new Vector3f(maxX,maxY,maxZ));
                Vector3f actualOpposite = armorPose.last().pose().transformPosition(
                        new Vector3f(cube.minX / 16F,cube.minY / 16F,cube.maxZ / 16F));
                if (actualOpposite.distance(expectedOpposite) > 0.0001F)
                    throw new AssertionError(name + " opposite corner angle=" + angle);
            } finally { part.loadPose(saved); }
        }
    }
}
