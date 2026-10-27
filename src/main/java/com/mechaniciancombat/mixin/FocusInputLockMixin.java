package com.mechaniciancombat.mixin;

import com.mechaniciancombat.TkGun;
import com.mechaniciancombat.client.GunClientState;
import com.mechaniciancombat.client.TkGunClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 专注期间屏蔽原版热键处理；保留玩家背包与战斗背包动作，并放行翻滚按下帧。
 */
@Mixin(Minecraft.class)
public abstract class FocusInputLockMixin {
	@Inject(method = "handleKeybinds()V", at = @At("HEAD"), cancellable = true)
	private void tkgun$lockFocusInput(CallbackInfo ci) {
		Minecraft client = (Minecraft) (Object) this;
		if (client.player != null && client.screen == null
				&& TkGun.holdingGun(client.player)
				&& GunClientState.focusish(client.player.getId())
				&& !TkGunClient.isRollTriggerTick(client)) {
			TkGunClient.handleBackpackActionKey(client);
			if (client.options.keyInventory.consumeClick()) {
				client.setScreen(new InventoryScreen(client.player));
			}
			ci.cancel();
		}
	}
}
