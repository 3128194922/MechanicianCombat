package com.mechaniciancombat.item;

import com.mechaniciancombat.TkGunServer;
import com.mechaniciancombat.entity.MoltenTridentEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 熔铁弩矢：可堆叠 64 的三叉戟式弹药。
 *
 * <p>保留三叉戟的投掷、近战和回收行为，但不消耗耐久；
 * 同时刻意忽略激流，避免右键变成冲刺旋转攻击。
 */
public class MoltenTridentItem extends TridentItem {
	public MoltenTridentItem(Item.Properties settings) {
		super(settings);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
		ItemStack stack = user.getItemInHand(hand);
		user.startUsingItem(hand);
		return InteractionResultHolder.consume(stack);
	}

	@Override
	public void releaseUsing(ItemStack stack, Level world, LivingEntity user, int remainingUseTicks) {
		if (!(user instanceof Player player)) {
			return;
		}
		int useTicks = getUseDuration(stack) - remainingUseTicks;
		if (useTicks < 10 || world.isClientSide) {
			return;
		}
		MoltenTridentEntity trident = new MoltenTridentEntity(
				TkGunServer.MOLTEN_TRIDENT_ENTITY, world, player, stack.copyWithCount(1));
		trident.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 2.5F, 1.0F);
		if (player.getAbilities().instabuild) {
			trident.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
		}
		if (!world.addFreshEntity(trident)) {
			com.mechaniciancombat.TkGun.LOGGER.warn(
					"[tkgun] Molten bolt throw was rejected by the level; preserving the item stack (world={}, owner={}, pos={})",
					world.dimension().location(), player.getGameProfile().getName(), trident.position());
			return;
		}
		com.mechaniciancombat.TkGun.LOGGER.info(
				"[tkgun] Molten bolt throw spawned (world={}, owner={}, entity={}, loyalty={})",
				world.dimension().location(), player.getGameProfile().getName(), trident.getId(),
				EnchantmentHelper.getItemEnchantmentLevel(Enchantments.LOYALTY, trident.getWeaponItem()));
		world.playSound(null, trident.blockPosition(), SoundEvents.TRIDENT_THROW,
				SoundSource.PLAYERS, 1.0F, 1.0F);
		if (!player.getAbilities().instabuild) {
			stack.shrink(1);
		}
	}

	@Override
	public boolean isEnchantable(ItemStack stack) {
		return true;
	}

	@Override
	public int getEnchantmentValue() {
		return 1;
	}

	@Override
	public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
		return enchantment == Enchantments.LOYALTY;
	}

	@Override
	public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
		return EnchantmentHelper.getEnchantments(book).keySet().stream()
				.allMatch(enchantment -> enchantment == Enchantments.LOYALTY);
	}

	@Override
	public void appendHoverText(ItemStack stack, net.minecraft.world.level.Level context, List<Component> tooltip, TooltipFlag type) {
		super.appendHoverText(stack, context, tooltip, type);
		BulletTooltips.addDamage(tooltip, "tooltip.tkgun.bolt.damage");
		BulletTooltips.addEffect(tooltip, "tooltip.tkgun.bolt.effect");
	}
}
