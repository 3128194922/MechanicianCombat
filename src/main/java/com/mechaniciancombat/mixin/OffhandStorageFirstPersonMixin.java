package com.mechaniciancombat.mixin;

import com.mechaniciancombat.item.BackpackLauncherRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** The launcher storage slot is inventory-only and has no first-person hand model. */
@Mixin(ItemInHandRenderer.class)
public abstract class OffhandStorageFirstPersonMixin {
	@Shadow
	private ItemStack offHandItem;

	@Redirect(method = "renderHandsWithItems",
			at = @At(value = "FIELD",
					target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;offHandItem:Lnet/minecraft/world/item/ItemStack;"))
	private ItemStack tkgun$hideStoredOffhandField(ItemInHandRenderer renderer) {
		if (BackpackLauncherRules.hasActiveLauncher(Minecraft.getInstance().player)) {
			return ItemStack.EMPTY;
		}
		return ((OffhandStorageFirstPersonMixin) (Object) renderer).offHandItem;
	}
}
