package com.mechaniciancombat.mixin;

import com.mechaniciancombat.TkGun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 手持枪械时滚轮改为切换该枪弹仓槽位，不再切快捷栏。 */
@Mixin(MouseHandler.class)
public abstract class GunAmmoScrollMixin {
	@Inject(method = "onScroll(JDD)V", at = @At("HEAD"), cancellable = true)
	private void tkgun$switchAmmoOnScroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
		Minecraft client = Minecraft.getInstance();
		if (yOffset == 0.0D || client.player == null || client.screen != null
				|| !TkGun.holdingGun(client.player)) {
			return;
		}
		// 向下滚 = 环形逆时针上一颗；向上滚 = 顺时针下一颗。
		// Queue the input for the HUD state machine; it only commits after the animation.
		com.mechaniciancombat.client.AmmoSwitchHud.wheel(yOffset > 0.0D ? 1 : -1);
		ci.cancel();
	}
}
