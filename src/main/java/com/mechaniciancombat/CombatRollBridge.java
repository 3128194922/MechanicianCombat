package com.mechaniciancombat;

import net.combatroll.api.event.ServerSideRollEvents;

/** Isolates hard references to the optional Combat Roll API. */
final class CombatRollBridge {
	static void register() {
		TkGun.LOGGER.info("[tkgun] Combat Roll 已接入：ROLL 窗口按资源 `roll` 动画长度 = {} tick",
				CombatRollHook.ROLL_DURATION);
		ServerSideRollEvents.PLAYER_START_ROLLING.register((player, direction) -> {
			TkGun.LOGGER.info("[tkgun] 翻滚事件：手持弩炮={} 当前状态={} 玩家={}",
					TkGun.holdingGun(player), TkGunServer.state(player).state,
					player.getName().getString());
			player.removeEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
			if (!TkGun.holdingGun(player)) {
				return;
			}
			TkGunServer.onRollStarted(player);
			var world = player.serverLevel();
			for (int i = 0; i < 10; i++) {
				double angle = i * Math.PI / 5.0;
				world.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
						player.getX() + Math.cos(angle) * 0.8,
						player.getY() + 0.9,
						player.getZ() + Math.sin(angle) * 0.8,
						1, 0.0, 0.0, 0.0, 0.02);
			}
			world.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK,
					player.getX(), player.getY() + 0.9, player.getZ(),
					3, 0.4, 0.3, 0.4, 0.0);
		});
	}

	private CombatRollBridge() {
	}
}
