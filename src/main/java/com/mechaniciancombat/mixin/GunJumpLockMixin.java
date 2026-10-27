package com.mechaniciancombat.mixin;

import com.mechaniciancombat.client.GunJumpRules;
import com.mechaniciancombat.client.GunWarningHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** While carrying the gun, only normal walking states may consume the jump key. */
@Mixin(KeyboardInput.class)
public abstract class GunJumpLockMixin {
	private static boolean tkgun$jumpHeldWhileBlocked;

	@Inject(method = "tick(ZF)V", at = @At("TAIL"))
	private void tkgun$lockJump(boolean slowDown, float slowDownFactor, CallbackInfo ci) {
		Minecraft client = Minecraft.getInstance();
		boolean jumpPressed = client.player != null && client.screen == null
				&& client.options.keyJump.isDown();
		if (!jumpPressed || GunJumpRules.canJump(client)) {
			tkgun$jumpHeldWhileBlocked = false;
			return;
		}
		KeyboardInput self = (KeyboardInput) (Object) this;
		self.jumping = false;
		if (!tkgun$jumpHeldWhileBlocked) {
			tkgun$jumpHeldWhileBlocked = true;
			GunWarningHud.showOverweight();
		}
	}
}
