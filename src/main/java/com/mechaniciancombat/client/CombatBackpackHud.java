package com.mechaniciancombat.client;

import com.mechaniciancombat.item.CombatBackpackItem;
import com.mechaniciancombat.item.BackpackUpgradeData;
import com.mechaniciancombat.net.CombatBackpackStatusPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Right-side backpack charge and phase readout. */
public final class CombatBackpackHud {
	private static final ResourceLocation AMMO_INFO_TEXTURE =
			new ResourceLocation("tkgun", "textures/gui/sprites/ammo_info.png");

	public static void render(GuiGraphics context) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.level == null || client.options.hideGui
				|| !BackpackUpgradeData.canUseLauncher(
						CombatBackpackItem.equippedStack(client.player))) {
			return;
		}

		int id = client.player.getId();
		int phase = CombatBackpackClientState.statusPhase(id);
		int charges = CombatBackpackClientState.charges(id);
		int remaining = CombatBackpackClientState.remainingTicks(id);
		BackpackUpgradeData.LauncherType launcherType =
				BackpackUpgradeData.launcherType(
						CombatBackpackItem.equippedStack(client.player));
		if (launcherType == BackpackUpgradeData.LauncherType.SUMMON_ANTENNA) {
			renderSummonAntenna(context, client, phase, remaining);
			return;
		}
		int maxCharges = launcherType == BackpackUpgradeData.LauncherType.AUTO ? 10 : 3;
		String stateText = switch (phase) {
			case CombatBackpackStatusPayload.PHASE_COOLDOWN ->
					"背包状态：装填" + Math.max(1, (remaining + 19) / 20) + "秒";
			case CombatBackpackStatusPayload.PHASE_MARKED ->
					"背包状态：已标记";
			case CombatBackpackStatusPayload.PHASE_AUTO_ACTIVE ->
					"背包状态：自动攻击";
			case CombatBackpackStatusPayload.PHASE_AUTO_PAUSED ->
					"背包状态：自动待命";
			default -> "背包状态：预备";
		};
		String chargeText = "当前充能：" + charges + "/" + maxCharges;
		int width = Math.max(client.font.width(stateText),
				client.font.width(chargeText)) + 10;
		int effectRows = client.player.getActiveEffects().size();
		int x = context.guiWidth() - width - 6;
		int y = 30 + effectRows * 24;
		int height = 28;

		drawAmmoInfoBackground(context, x, y, width, height);
		int stateColour = phase == CombatBackpackStatusPayload.PHASE_COOLDOWN
				? 0xFFFFB060
				: (phase == CombatBackpackStatusPayload.PHASE_MARKED
						|| phase == CombatBackpackStatusPayload.PHASE_AUTO_ACTIVE
						|| phase == CombatBackpackStatusPayload.PHASE_AUTO_PAUSED)
				? 0xFF9BE0FF : 0xFFE8E8E8;
		context.drawString(client.font, stateText, x + 6, y + 5, stateColour, true);
		context.drawString(client.font, chargeText, x + 6, y + 15,
				charges > 0 ? 0xFFFFF0A0 : 0xFFFF6666, true);
	}

	private static void renderSummonAntenna(GuiGraphics context,
			Minecraft client, int phase, int remaining) {
		String stateText = switch (phase) {
			case CombatBackpackStatusPayload.PHASE_COOLDOWN ->
					net.minecraft.network.chat.Component.translatable(
							"hud.tkgun.summon_antenna.cooldown",
							Math.max(1, (remaining + 19) / 20)).getString();
			case CombatBackpackStatusPayload.PHASE_SUMMON_READY ->
					net.minecraft.network.chat.Component.translatable(
							"hud.tkgun.summon_antenna.ready").getString();
			case CombatBackpackStatusPayload.PHASE_SUMMON_ARMED ->
					net.minecraft.network.chat.Component.translatable(
							"hud.tkgun.summon_antenna.armed").getString();
			case CombatBackpackStatusPayload.PHASE_SUMMON_MARKED ->
					net.minecraft.network.chat.Component.translatable(
							"hud.tkgun.summon_antenna.marked").getString();
			case CombatBackpackStatusPayload.PHASE_SUMMON_CAST ->
					net.minecraft.network.chat.Component.translatable(
							"hud.tkgun.summon_antenna.cast").getString();
			default -> net.minecraft.network.chat.Component.translatable(
					"hud.tkgun.summon_antenna.dormant").getString();
		};
		String chargeText = net.minecraft.network.chat.Component.translatable(
				"hud.tkgun.summon_antenna.charge").getString();
		int width = Math.max(client.font.width(stateText),
				client.font.width(chargeText)) + 10;
		int effectRows = client.player.getActiveEffects().size();
		int x = context.guiWidth() - width - 6;
		int y = 30 + effectRows * 24;
		int height = 28;
		drawAmmoInfoBackground(context, x, y, width, height);
		int colour = phase == CombatBackpackStatusPayload.PHASE_COOLDOWN
				? 0xFFFFB060
				: phase == CombatBackpackStatusPayload.PHASE_SUMMON_ARMED
						|| phase == CombatBackpackStatusPayload.PHASE_SUMMON_MARKED
						|| phase == CombatBackpackStatusPayload.PHASE_SUMMON_CAST
						? 0xFF9BE0FF : 0xFFE8E8E8;
		context.drawString(client.font, stateText, x + 6, y + 5, colour, true);
		context.drawString(client.font, chargeText, x + 6, y + 15,
				0xFFFFF0A0, true);
	}

	private static void drawAmmoInfoBackground(GuiGraphics context,
			int x, int y, int width, int height) {
		GuiNineSlice.ammoBackground(context, AMMO_INFO_TEXTURE,
				x - 3, y - 3, width + 6, height + 6);
	}

	private CombatBackpackHud() {
	}
}
