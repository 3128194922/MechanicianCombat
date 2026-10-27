package com.mechaniciancombat.item;

import com.mechaniciancombat.entity.WindChargeEntity;
import com.mechaniciancombat.registry.ModRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A locally registered wind charge for the 1.20.1 Forge port. */
public final class WindChargeItem extends Item {
    public WindChargeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            WindChargeEntity projectile = new WindChargeEntity(level, player);
            projectile.setPos(player.getX(), player.getEyeY() - 0.1D, player.getZ());
            projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 0.0F);
            if (!level.addFreshEntity(projectile)) {
                return InteractionResultHolder.fail(stack);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 0.5F, 1.1F);
            player.getCooldowns().addCooldown(this, 10);
            player.awardStat(Stats.ITEM_USED.get(this));
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
