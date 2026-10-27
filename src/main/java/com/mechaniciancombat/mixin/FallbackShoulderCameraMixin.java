package com.mechaniciancombat.mixin;

import com.mechaniciancombat.client.FallbackShoulderCamera;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class FallbackShoulderCameraMixin {
	@Shadow
	protected abstract void setPosition(double x, double y, double z);

	@Shadow
	protected abstract void setRotation(float yaw, float pitch);

	@Inject(method = "setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V",
			at = @At("TAIL"))
	private void tkgun$applyFallbackShoulder(BlockGetter view, Entity focusedEntity,
											  boolean thirdPerson, boolean inverseView,
											  float tickDelta, CallbackInfo ci) {
		if (!thirdPerson || inverseView) {
			return;
		}
		FallbackShoulderCamera.Pose pose =
				FallbackShoulderCamera.computePose(view, focusedEntity, tickDelta);
		if (pose != null) {
			this.setPosition(pose.position().x, pose.position().y, pose.position().z);
			this.setRotation(pose.yaw(), pose.pitch());
		}
	}
}
