package com.mechaniciancombat.client;

import com.mechaniciancombat.TkGun;
import com.mechaniciancombat.client.rig.BowgunRig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.core.molang.MolangParser;

import java.util.HashMap;
import java.util.Map;
import java.util.function.DoubleSupplier;

/** 为 tkgun rig 注册私有 Molang 变量，避免改写 GeckoLib 提供给其他模组的标准 query。 */
public final class TkGunMolang {
	private static boolean installed;
	private static final boolean VERBOSE = System.getProperty("tkgun.demo") != null
			|| System.getProperty("tkgun.verbose") != null;
	private static final Map<String, Long> PROBE_TIME = new HashMap<>();

	public static void install() {
		if (installed) {
			return;
		}
		installed = true;
		MolangParser.INSTANCE.setValue("query.tkgun_head_x_rotation", probe("head_x", TkGunMolang::headXRotation));
		MolangParser.INSTANCE.setValue("query.tkgun_body_x_rotation", probe("body_x", TkGunMolang::bodyXRotation));
		MolangParser.INSTANCE.setValue("query.tkgun_head_y_rotation", probe("head_y", TkGunMolang::headYRotation));
		MolangParser.INSTANCE.setValue("query.tkgun_body_y_rotation", probe("body_y", TkGunMolang::bodyYRotation));
		TkGun.LOGGER.info("[tkgun] 已注册 rig 专用视角 Molang 变量");
	}

	private static DoubleSupplier probe(String name, DoubleSupplier supplier) {
		return () -> {
			double value = supplier.getAsDouble();
			if (VERBOSE) {
				long now = System.currentTimeMillis();
				Long last = PROBE_TIME.get(name);
				if (last == null || now - last >= 1000L) {
					PROBE_TIME.put(name, now);
					TkGun.LOGGER.info("[tkgun] rig Molang 求值 {} = {}", name,
							Math.round(value * 100.0D) / 100.0D);
				}
			}
			return value;
		};
	}

	private static LivingEntity currentPlayer() {
		return BowgunRig.CURRENT_PLAYER;
	}

	private static float partialTick() {
		return Minecraft.getInstance().getPartialTick();
	}

	private static double headXRotation() {
		LivingEntity player = currentPlayer();
		return player == null ? 0.0D : -net.minecraft.util.Mth.rotLerp(partialTick(), player.xRotO, player.getXRot());
	}

	private static double bodyXRotation() {
		return headXRotation();
	}

	private static double headYRotation() {
		LivingEntity player = currentPlayer();
		if (player == null || isLocalAiming(player)) {
			return 0.0D;
		}
		return -Mth.wrapDegrees(player.yHeadRot - player.yBodyRot);
	}

	private static double bodyYRotation() {
		LivingEntity player = currentPlayer();
		return player == null ? 0.0D
				: Mth.lerp(partialTick(), player.yBodyRotO, player.yBodyRot);
	}

	private static boolean isLocalAiming(LivingEntity player) {
		Minecraft client = Minecraft.getInstance();
		if (client.player != player) {
			return false;
		}
		if (client.options.keyUse.isDown()) {
			return GunCamera.aimAllowed(client);
		}
		return GunClientState.focused(player.getId());
	}

	private TkGunMolang() { }
}