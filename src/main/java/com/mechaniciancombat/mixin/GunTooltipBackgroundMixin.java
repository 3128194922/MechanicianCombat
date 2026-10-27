package com.mechaniciancombat.mixin;

import com.mechaniciancombat.client.GunTooltipState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 用视觉/GUI/枪械详情栏资源替换枪械 tooltip 的原版背景。 */
@Mixin(TooltipRenderUtil.class)
public abstract class GunTooltipBackgroundMixin {
	@Inject(method = "renderTooltipBackground(Lnet/minecraft/client/gui/GuiGraphics;IIIII)V",
			at = @At("HEAD"), cancellable = true)
	private static void tkgun$renderGunTooltipBackground(GuiGraphics context, int x, int y,
													  int width, int height, int z, CallbackInfo ci) {
		if (!GunTooltipState.isGunTooltip()) {
			return;
		}
		context.pose().pushPose();
		context.pose().translate(0.0F, 0.0F, z);
		// 原版 TooltipBackgroundRenderer 会在文本矩形外再扩 3px；
		// 自定义背景必须覆盖同一范围，否则文字/阴影会露出边缘。
		context.blit(GunTooltipState.TEXTURE,
				x - 3, y - 3, 0, 0, width + 6, height + 6, 256, 256);
		context.pose().popPose();
		ci.cancel();
	}
}
