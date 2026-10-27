package com.mechaniciancombat.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.inventory.Slot;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.List;

/**
 * 破垒重炮物品。右键负责举枪瞄准，左键负责射击；专注模式通过 Z 键切换。
 * useAction 使用 NONE，所有武器姿势由 GeckoLib rig 播放。
 */
public class BowgunItem extends Item {
	public BowgunItem(Properties settings) {
		super(settings);
	}

	@Override
	public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction clickType, Player player) {
		// 从副手取出枪械必须保留；只有把鼠标中的物品放进副手时才拒绝。
		if (isOffhandSlot(slot, player) && !player.containerMenu.getCarried().isEmpty()) {
			return true;
		}
		return super.overrideStackedOnOther(stack, slot, clickType, player);
	}

	@Override
	public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack cursorStack, Slot slot, ClickAction clickType,
							 Player player, net.minecraft.world.entity.SlotAccess cursorStackReference) {
		// 鼠标持有枪械时，点击空的或已有物品的副手槽都不会执行交换。
		return isOffhandSlot(slot, player)
				|| super.overrideOtherStackedOnMe(stack, cursorStack, slot, clickType, player, cursorStackReference);
	}

	private static boolean isOffhandSlot(Slot slot, Player player) {
		return slot != null && player != null
				&& slot.container == player.getInventory()
				&& slot.getContainerSlot() == Inventory.SLOT_OFFHAND;
	}

	@Override
	public void appendHoverText(ItemStack stack, net.minecraft.world.level.Level context, List<Component> tooltip, TooltipFlag type) {
		super.appendHoverText(stack, context, tooltip, type);
		String description = Component.translatable("tooltip.tkgun.bowgun.description").getString();
		for (String part : description.split("\\r?\\n|；|;")) {
			String line = part.trim();
			if (!line.isEmpty()) {
				tooltip.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
			}
		}
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
		ItemStack stack = user.getItemInHand(hand);
		// **不可右键瞄准的状态 → 直接屏蔽右键输入**（用户 2026-10-17）：返回 `fail` 就不会
		// `setCurrentHand`，玩家不会进入"使用物品"状态（没有举枪姿势、没有减速、没有 FOV）。
		// 客户端看镜像状态；服务端看权威状态（两边同一条规则：翻滚/滑铲 + 结束后 1 秒）。
		if (world.isClientSide) {
			if (DistExecutor.unsafeCallWhenOn(Dist.CLIENT,
					() -> () -> com.mechaniciancombat.client.ClientAimGuard.isCurrentPlayerAimLocked())) {
				return InteractionResultHolder.fail(stack);
			}
		} else if (user instanceof net.minecraft.server.level.ServerPlayer sp) {
			if (com.mechaniciancombat.TkGunServer.aimInputLocked(sp)) {
				return InteractionResultHolder.fail(stack);
			}
		}
		// 右键建立举枪使用状态；专注由 Z 单独切换，两者不通过同一次右键输入互相改写。
		user.startUsingItem(hand);
		return InteractionResultHolder.consume(stack);
	}

	@Override
	public int getUseDuration(ItemStack stack) {
		// 右键举枪是按住状态，不使用原版蓄力时长。
		return 72000;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.NONE;
	}

	@Override
	public void releaseUsing(ItemStack stack, Level world, LivingEntity user, int remainingUseTicks) {
		// 松开右键不直接切换专注或开火；服务端状态由独立输入与状态机维护。
	}
}
