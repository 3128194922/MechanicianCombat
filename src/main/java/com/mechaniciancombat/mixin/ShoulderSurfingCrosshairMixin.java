package com.mechaniciancombat.mixin;

import com.mechaniciancombat.TkGun;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Optional r396 crosshair compatibility; selected only when Shoulder Surfing exists. */
@Pseudo
@Mixin(targets = "com.github.exopandora.shouldersurfing.client.CrosshairRenderer", remap = false)
public abstract class ShoulderSurfingCrosshairMixin {
    @Inject(method = "doRenderCrosshair", at = @At("HEAD"), cancellable = true)
    private void tkgun$hideShoulderCrosshair(CallbackInfoReturnable<Boolean> cir) {
        var player = Minecraft.getInstance().player;
        if (player != null && TkGun.holdingGun(player)) cir.setReturnValue(false);
    }
}
