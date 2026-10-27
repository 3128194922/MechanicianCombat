package com.mechaniciancombat.client.entity;

import com.mechaniciancombat.client.MoltenGolemGeoModel;
import com.mechaniciancombat.entity.MoltenGolemEntity;
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class MoltenGolemEntityRenderer
		extends GeoEntityRenderer<MoltenGolemEntity> {
	public MoltenGolemEntityRenderer(EntityRendererProvider.Context context) {
		super(context, new MoltenGolemGeoModel());
		this.shadowRadius = 1.4F;
	}


	@Override
	public void renderRecursively(PoseStack matrices, MoltenGolemEntity animatable,
								  GeoBone bone, RenderType renderType,
								  MultiBufferSource consumers, VertexConsumer buffer,
								  boolean isReRender, float tickDelta, int packedLight,
								  int packedOverlay, float red, float green, float blue, float alpha) {
		if (!isReRender && "locator_right".equals(bone.getName())) {
			Vector3d position = bone.getWorldPosition();
			Vec3 locator = new Vec3(position.x, position.y, position.z);
			MoltenGolemEntity.recordRenderedLocator(animatable.getId(), locator,
					animatable.level().getGameTime());
			animatable.emitClientAttackLocatorEffects(locator);
		}
		super.renderRecursively(matrices, animatable, bone, renderType, consumers,
				buffer, isReRender, tickDelta, packedLight, packedOverlay, red, green, blue, alpha);
	}
}
