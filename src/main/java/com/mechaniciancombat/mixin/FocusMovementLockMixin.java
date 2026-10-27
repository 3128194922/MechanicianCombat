package com.mechaniciancombat.mixin;

import com.mechaniciancombat.TkGun;
import com.mechaniciancombat.client.GunClientState;
import com.mechaniciancombat.client.TkGunClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 专注期间清零移动输入；翻滚按下帧保留方向供 Combat Roll 计算翻滚向量。 */
@Mixin(KeyboardInput.class)
public abstract class FocusMovementLockMixin {
	@Inject(method = "tick(ZF)V", at = @At("TAIL"))
	private void tkgun$lockFocusMovement(boolean slowDown, float slowDownFactor, CallbackInfo ci) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || !TkGun.holdingGun(client.player)
				|| !GunClientState.focusish(client.player.getId())) {
			return;
		}
		KeyboardInput self = (KeyboardInput) (Object) this;
		if (!TkGunClient.isRollTriggerTick(client)) {
			self.forwardImpulse = 0.0F;
			self.leftImpulse = 0.0F;
		}
		self.jumping = false;
		self.shiftKeyDown = false;
	}
}
