package com.mechaniciancombat.mixin;

import com.mechaniciancombat.TkGunServer;
import com.mechaniciancombat.item.BackpackLauncherRules;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Loyalty returns backpack-launched tridents to the occupied offhand first. */
@Mixin(ThrownTrident.class)
public abstract class BackpackLoyaltyPickupMixin {
	@Inject(method = "tryPickup",
			at = @At("HEAD"), cancellable = true)
	private void tkgun$returnLoyaltyToLauncherStorage(
			net.minecraft.world.entity.player.Player player,
			CallbackInfoReturnable<Boolean> cir) {
		ThrownTrident trident = (ThrownTrident) (Object) this;
		if (!(player instanceof ServerPlayer serverPlayer)
				|| trident.getOwner() != player
				|| !TkGunServer.isBackpackProjectile(trident)
				|| !BackpackLauncherRules.hasActiveLauncher(player)
				|| !(player.level() instanceof ServerLevel world)) {
			return;
		}
		ItemStack weapon = ((TridentEntityAccessor) trident).tkgun$getTridentItem();
		int loyalty = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.LOYALTY, weapon);
		if (loyalty <= 0) {
			return;
		}

		Inventory inventory = player.getInventory();
		ItemStack returned = weapon.copyWithCount(1);
		ItemStack stored = inventory.getItem(Inventory.SLOT_OFFHAND);
		boolean accepted = false;
		if (stored.isEmpty()) {
			inventory.setItem(Inventory.SLOT_OFFHAND, returned);
			accepted = true;
		} else if (ItemStack.isSameItemSameTags(stored, returned)
				&& stored.getCount() < stored.getMaxStackSize()) {
			stored.grow(1);
			accepted = true;
		}
		if (accepted) {
			inventory.setChanged();
			serverPlayer.inventoryMenu.broadcastChanges();
			if (serverPlayer.containerMenu != serverPlayer.inventoryMenu) {
				serverPlayer.containerMenu.broadcastChanges();
			}
			TkGunServer.restoreBackpackChargeAfterLoyalty(serverPlayer);
			cir.setReturnValue(true);
		}
	}
}
