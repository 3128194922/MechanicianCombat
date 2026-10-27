package com.mechaniciancombat.mixin;

import com.mechaniciancombat.item.BackpackLauncherRules;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Hides only the stored offhand stack while retaining the main-hand item render. */
@Mixin(ItemInHandLayer.class)
public abstract class OffhandStorageThirdPersonMixin {
	@Redirect(method = "render",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/world/entity/LivingEntity;getOffhandItem()Lnet/minecraft/world/item/ItemStack;"))
	private ItemStack tkgun$hideStoredOffhandThirdPerson(LivingEntity entity) {
		ItemStack offhand = entity.getItemInHand(net.minecraft.world.InteractionHand.OFF_HAND);
		return BackpackLauncherRules.hasActiveLauncher(entity)
				? ItemStack.EMPTY : offhand;
	}
}
