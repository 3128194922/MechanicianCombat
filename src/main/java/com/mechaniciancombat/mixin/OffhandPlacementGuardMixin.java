package com.mechaniciancombat.mixin;

import com.mechaniciancombat.TkGun;
import com.mechaniciancombat.item.BackpackLauncherRules;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Enforces gun and launcher-storage rules for every inventory click path. */
@Mixin(AbstractContainerMenu.class)
public abstract class OffhandPlacementGuardMixin {
	@Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
	private void tkgun$guardOffhandInsertion(int slotIndex, int button, ClickType actionType,
												  Player player, CallbackInfo ci) {
		if (slotIndex < 0 || actionType == ClickType.QUICK_MOVE) {
			return;
		}
		AbstractContainerMenu handler = (AbstractContainerMenu) (Object) this;
		if (slotIndex >= handler.slots.size()) {
			return;
		}

		Slot clicked = handler.getSlot(slotIndex);
		boolean clickedOffhand = isOffhand(clicked, player);
		ItemStack staged = switch (actionType) {
			case PICKUP, QUICK_CRAFT -> clickedOffhand
					? handler.getCarried() : ItemStack.EMPTY;
			case SWAP -> {
				if (clickedOffhand && button >= 0 && button < 9) {
					yield player.getInventory().getItem(button);
				}
				yield button == 40 ? clicked.getItem() : ItemStack.EMPTY;
			}
			default -> ItemStack.EMPTY;
		};
		boolean targetsOffhand = clickedOffhand
				|| (actionType == ClickType.SWAP && button == 40);
		if (!targetsOffhand || staged.isEmpty()) {
			return;
		}
		if (TkGun.isGun(staged)) {
			ci.cancel();
			return;
		}
		if (BackpackLauncherRules.hasActiveLauncher(player)
				&& !BackpackLauncherRules.isLaunchable(staged)
				&& !BackpackLauncherRules.isLaunchable(handler.getCarried())) {
			ci.cancel();
		}
	}

	private static boolean isOffhand(Slot slot, Player player) {
		return slot.container == player.getInventory()
				&& slot.getContainerSlot() == Inventory.SLOT_OFFHAND;
	}
}
