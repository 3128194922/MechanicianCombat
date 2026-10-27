package com.mechaniciancombat.mixin;

import com.mechaniciancombat.client.FallbackShoulderCamera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class FallbackShoulderRunMixin {
	@Inject(method = "tick(ZF)V", at = @At("TAIL"))
	private void tkgun$steerDecoupledRunning(boolean slowDown, float slowDownFactor,
											 CallbackInfo ci) {
		Minecraft client = Minecraft.getInstance();
		KeyboardInput input = (KeyboardInput) (Object) this;
		FallbackShoulderCamera.steerRunningInput(client, input);
	}
}
