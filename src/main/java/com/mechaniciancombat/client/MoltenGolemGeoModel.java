package com.mechaniciancombat.client;

import com.mechaniciancombat.TkGun;
import com.mechaniciancombat.entity.MoltenGolemEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class MoltenGolemGeoModel extends GeoModel<MoltenGolemEntity> {
	private static final ResourceLocation MODEL = TkGun.id("geo/entity/molten_golem.geo.json");
	private static final ResourceLocation TEXTURE = TkGun.id("textures/entity/molten_golem.png");
	private static final ResourceLocation ANIMATION =
			TkGun.id("animations/entity/molten_golem.animation.json");

	@Override
	public ResourceLocation getModelResource(MoltenGolemEntity animatable) {
		return MODEL;
	}

	@Override
	public ResourceLocation getTextureResource(MoltenGolemEntity animatable) {
		return TEXTURE;
	}

	@Override
	public ResourceLocation getAnimationResource(MoltenGolemEntity animatable) {
		return ANIMATION;
	}

	@Override
	public RenderType getRenderType(MoltenGolemEntity animatable, ResourceLocation texture) {
		return RenderType.entityCutoutNoCull(texture);
	}
}
