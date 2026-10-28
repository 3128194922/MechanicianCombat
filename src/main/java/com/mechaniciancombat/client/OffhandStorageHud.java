package com.mechaniciancombat.client;

import com.mechaniciancombat.item.BackpackLauncherRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.InteractionHand;
import net.minecraft.resources.ResourceLocation;

/** Keeps the native offhand storage frame visible before it contains an item. */
public final class OffhandStorageHud {
	public static void render(GuiGraphics context) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.options.hideGui
				|| !BackpackLauncherRules.hasActiveLauncher(client.player)) {
			return;
		}
		ItemStack offhand = client.player.getItemInHand(InteractionHand.OFF_HAND);
		if (!offhand.isEmpty()) {
			return; // Vanilla draws both the occupied frame and item icon.
		}

		HumanoidArm offhandArm = client.player.getMainArm().getOpposite();
		int width = context.guiWidth();
		int height = context.guiHeight();
		int x = offhandArm == HumanoidArm.LEFT ? width / 2 - 91 - 29 : width / 2 + 91;
		int y = height - 23;
		ResourceLocation texture = new ResourceLocation("minecraft", "textures/gui/widgets.png");
		int u = offhandArm == HumanoidArm.LEFT ? 24 : 53;
		context.blit(texture, x, y, u, 22, 29, 24, 256, 256);
	}

	private OffhandStorageHud() {
	}
}
