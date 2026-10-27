package com.mechaniciancombat.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;

/** Temporary development tool: removes a non-player entity on right click. */
public final class EntityRemovalToolItem extends Item {
	public EntityRemovalToolItem(Properties settings) {
		super(settings);
	}

	@Override
	public InteractionResult interactLivingEntity(ItemStack stack, Player user,
									LivingEntity entity, InteractionHand hand) {
		if (user.level().isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (entity == user || entity instanceof Player || !entity.isAlive()) {
			return InteractionResult.PASS;
		}
		if (user.level() instanceof ServerLevel world) {
			world.sendParticles(ParticleTypes.POOF,
					entity.getX(), entity.getY(0.5D), entity.getZ(),
					12, 0.3D, 0.4D, 0.3D, 0.02D);
			world.playSound(null, entity.blockPosition(),
					SoundEvents.ENDERMAN_TELEPORT,
					SoundSource.PLAYERS, 0.6F, 1.4F);
		}
		entity.discard();
		return InteractionResult.SUCCESS;
	}
}
