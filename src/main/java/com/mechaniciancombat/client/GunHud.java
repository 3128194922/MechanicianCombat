package com.mechaniciancombat.client;

import com.mechaniciancombat.TkGun;
import com.mechaniciancombat.item.GunAmmoData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/** 右下角弹药与热量 HUD：腰射显示弹匣计数，专注显示热量与当前弹仓余量。 */
public final class GunHud {
	private static final ResourceLocation AMMO_INFO_TEXTURE =
			new ResourceLocation("tkgun", "textures/gui/sprites/ammo_info.png");

	public static void render(GuiGraphics context) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.level == null || client.options.hideGui
				|| client.screen != null) {
			return;
		}
		if (!TkGun.holdingGun(client.player)) {
			return;
		}
		int id = client.player.getId();
		int left = GunClientState.roundsLeft(id);
		boolean reloading = GunClientState.reloading(id);
		String focusTag = GunClientState.focusText(id);
		boolean focused = GunClientState.focusish(id) || GunClientState.overheated(id);

		String main = focused
				? "热量槽：" + GunClientState.heatPercent(id) + "%"
				: loadedMagazineText(id, left);
		String ammo = selectedAmmoText(client);
		int mainW = client.font.width(main);
		int ammoW = client.font.width(ammo);
		int pad = 4;
		int boxW = Math.max(mainW, ammoW) + pad * 2;
		int boxH = 32;
		int x = context.guiWidth() - boxW - 6;
		int y = context.guiHeight() - 74;

		GuiNineSlice.ammoBackground(context, AMMO_INFO_TEXTURE,
				x - 3, y - 3, boxW + 6, boxH + 6);
		int heat = GunClientState.heatPercent(id);
		int colour = reloading ? 0xFFE0A030
				: focused ? (heat >= 75 ? 0xFFFF5555 : 0xFFFFB060)
				: (left == 0 ? 0xFFFF5555 : 0xFFFFF0A0);
		context.drawString(client.font, main, x + pad, y + 4, colour, true);
		context.drawString(client.font, ammo, x + pad, y + 16, 0xFFC8C8C8, true);

		// 专注：上面单独一行（青色）
		if (focusTag != null) {
			int tw = client.font.width(focusTag);
			int bx = x + boxW - tw - pad;
			context.fill(bx - pad, y - 12, bx + tw + pad, y - 1, 0x90102828);
			context.drawString(client.font, focusTag, bx, y - 10, 0xFF9BE0FF, true);
		}
		AmmoSwitchHud.render(context);
		GunWarningHud.render(context);
	}

	private static String loadedMagazineText(int entityId, int left) {
		String id = GunClientState.magAmmo(entityId);
		if (id.isEmpty()) {
			return "空弹匣 " + left + "/6";
		}
		return itemName(id) + " " + left + "/6";
	}

	private static String selectedAmmoText(Minecraft client) {
		ItemStack gun = GunAmmoData.heldGun(client.player);
		if (gun.isEmpty()) {
			return "→空";
		}
		var items = GunAmmoData.read(gun, client.level.registryAccess());
		int selected = AmmoSwitchHud.displayedSelectedSlot();
		if (selected < 0) {
			selected = GunAmmoData.selected(gun);
		}
		ItemStack ammo = items.get(Math.floorMod(selected, GunAmmoData.SIZE));
		if (ammo.isEmpty()) {
			return "→空";
		}
		ResourceLocation ammoId = BuiltInRegistries.ITEM.getKey(ammo.getItem());
		int total = 0;
		for (ItemStack stack : items) {
			if (!stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(ammoId)) {
				total += stack.getCount();
			}
		}
		return "→" + ammo.getHoverName().getString() + " " + total;
	}

	private static String itemName(String registryId) {
		try {
			var item = BuiltInRegistries.ITEM.get(new ResourceLocation(registryId));
			return item == net.minecraft.world.item.Items.AIR
					? "空弹匣" : new ItemStack(item).getHoverName().getString();
		} catch (RuntimeException ignored) {
			return "空弹匣";
		}
	}

	private GunHud() {
	}
}
