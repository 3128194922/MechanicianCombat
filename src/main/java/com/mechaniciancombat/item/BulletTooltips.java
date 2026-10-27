package com.mechaniciancombat.item;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.ChatFormatting;

import java.util.List;

/** 弹药 tooltip 的统一排版：标点只用于拆分，不进入实际显示文本。 */
final class BulletTooltips {
	static void addDamage(List<Component> tooltip, String damageKey) {
		tooltip.add(Component.translatable("tooltip.tkgun.damage", Component.translatable(damageKey))
				.withStyle(ChatFormatting.GRAY));
	}

	static void addEffect(List<Component> tooltip, String effectKey) {
		String raw = Component.translatable(effectKey).getString();
		boolean first = true;
		for (String part : raw.split("\\r?\\n|；|;")) {
			String line = part.trim();
			if (line.isEmpty()) {
				continue;
			}
			MutableComponent text = first
					? Component.translatable("tooltip.tkgun.effect", Component.literal(line))
					: Component.literal(line);
			tooltip.add(text.withStyle(ChatFormatting.GRAY));
			first = false;
		}
	}

	private BulletTooltips() {
	}
}
