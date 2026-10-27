package com.mechaniciancombat.mixin;

import com.mechaniciancombat.client.FallbackShoulderCamera;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class FallbackShoulderMouseMixin {
	@Inject(method = "turn(DD)V", at = @At("HEAD"), cancellable = true)
	private void tkgun$turnFallbackCamera(double deltaX, double deltaY, CallbackInfo ci) {
		if ((Object) this instanceof LocalPlayer player
				&& FallbackShoulderCamera.turn(player, deltaX, deltaY)) {
			ci.cancel();
		}
	}
}
