package com.mechaniciancombat.mixin;

import com.mechaniciancombat.TkGun;
import com.mechaniciancombat.client.GunTooltipState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 标记 HandledScreen 当前悬停的是枪械，供 tooltip 背景渲染器替换背景。 */
@Mixin(AbstractContainerScreen.class)
public abstract class GunTooltipScreenMixin {
	@Shadow
	protected Slot hoveredSlot;

	@Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("HEAD"))
	private void tkgun$markGunTooltip(GuiGraphics context, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		GunTooltipState.set(hoveredSlot != null && hoveredSlot.hasItem()
				&& TkGun.isGun(hoveredSlot.getItem()));
	}

	@Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("RETURN"))
	private void tkgun$clearGunTooltip(GuiGraphics context, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		GunTooltipState.set(false);
	}
}
