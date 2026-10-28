package com.mechaniciancombat.mixin;

import com.mechaniciancombat.TkGun;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Avoid drawing a second, vanilla item model over the animated GeckoLib gun rig. */
@Mixin(ItemInHandLayer.class)
public abstract class BowgunVanillaItemLayerMixin<T extends LivingEntity, M extends HumanoidModel<T>> {
	@Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
	private void tkgun$skipVanillaBowgun(T entity, ItemStack stack, ItemDisplayContext displayContext,
			HumanoidArm arm, PoseStack poseStack, MultiBufferSource buffers, int packedLight, CallbackInfo ci) {
		if (entity instanceof Player player && TkGun.holdingGun(player) && TkGun.isGun(stack)) {
			ci.cancel();
		}
	}
}
