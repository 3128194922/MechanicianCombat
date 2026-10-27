package com.mechaniciancombat.mixin;

import com.mechaniciancombat.client.GunCamera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


/** 瞄准状态下把每帧鼠标位移减半，不改玩家保存的鼠标灵敏度。 */
@Mixin(MouseHandler.class)
public abstract class AimMouseSensitivityMixin {
	@Shadow private double accumulatedDX;
	@Shadow private double accumulatedDY;

	@Inject(method = "turnPlayer()V", at = @At("HEAD"))
	private void tkgun$halveAimMouseDelta(CallbackInfo ci) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || !GunCamera.aiming(client)) {
			return;
		}
		this.accumulatedDX *= 0.5D;
		this.accumulatedDY *= 0.5D;
	}
}
