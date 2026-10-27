package com.mechaniciancombat.item;

import com.mechaniciancombat.TkGunServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.level.Level;

/** Temporary development item that clears the current backpack cooldown. */
public final class BackpackCooldownResetItem extends Item {
	public BackpackCooldownResetItem(Properties settings) {
		super(settings);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
		ItemStack stack = user.getItemInHand(hand);
		if (!world.isClientSide() && user instanceof ServerPlayer player) {
			TkGunServer.clearBackpackCooldown(player);
		}
		return InteractionResultHolder.success(stack);
	}
}
