package com.mechaniciancombat.client;

import net.minecraft.resources.ResourceLocation;

/** 标记当前正在绘制的 tooltip 是否属于破垒重炮。 */
public final class GunTooltipState {
	public static final ResourceLocation TEXTURE =
			new ResourceLocation("tkgun", "gun_tooltip");

	private static boolean gunTooltip;

	public static void set(boolean value) {
		gunTooltip = value;
	}

	public static boolean isGunTooltip() {
		return gunTooltip;
	}

	private GunTooltipState() {
	}
}
