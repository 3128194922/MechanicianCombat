package com.mechaniciancombat.client;

import com.mechaniciancombat.TkGun;
import com.mechaniciancombat.item.CombatBackpackItem;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class CombatBackpackGeoModel extends GeoModel<CombatBackpackItem> {
	private static final ResourceLocation MODEL =
			TkGun.id("geo/entity/combat_backpack.geo.json");
	private static final ResourceLocation ANIMATION =
			TkGun.id("animations/entity/combat_backpack.animation.json");

	@Override
	public ResourceLocation getModelResource(CombatBackpackItem animatable) {
		return MODEL;
	}

	@Override
	public ResourceLocation getTextureResource(CombatBackpackItem animatable) {
		return animatable.textureLocation();
	}

	@Override
	public ResourceLocation getAnimationResource(CombatBackpackItem animatable) {
		return ANIMATION;
	}

	@Override
	public RenderType getRenderType(CombatBackpackItem animatable, ResourceLocation texture) {
		return RenderType.entityCutoutNoCull(texture);
	}
}
