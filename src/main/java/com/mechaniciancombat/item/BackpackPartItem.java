package com.mechaniciancombat.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Installable component for a combat backpack. */
public final class BackpackPartItem extends Item {
	public enum Kind {
		CORE,
		LAUNCHER,
		AUTO_LAUNCHER,
		SUMMON_ANTENNA
	}

	private final Kind kind;

	public BackpackPartItem(Kind kind, Properties settings) {
		super(settings);
		this.kind = kind;
	}

	public Kind kind() {
		return kind;
	}

	public boolean isLauncherKind() {
		return kind == Kind.LAUNCHER || kind == Kind.AUTO_LAUNCHER
				|| kind == Kind.SUMMON_ANTENNA;
	}

	@Override
	public void appendHoverText(ItemStack stack, net.minecraft.world.level.Level context, List<Component> tooltip,
							  TooltipFlag type) {
		super.appendHoverText(stack, context, tooltip, type);
		if (kind == Kind.LAUNCHER) {
			tooltip.add(Component.translatable("tooltip.tkgun.portable_launcher.storage"));
			tooltip.add(Component.translatable("tooltip.tkgun.portable_launcher.ammo"));
			tooltip.add(Component.translatable("tooltip.tkgun.portable_launcher.trajectory"));
		} else if (kind == Kind.AUTO_LAUNCHER) {
			tooltip.add(Component.translatable("tooltip.tkgun.auto_portable_launcher.storage"));
			tooltip.add(Component.translatable("tooltip.tkgun.auto_portable_launcher.attack"));
			tooltip.add(Component.translatable("tooltip.tkgun.auto_portable_launcher.charge"));
		} else if (kind == Kind.SUMMON_ANTENNA) {
			tooltip.add(Component.translatable("tooltip.tkgun.summon_antenna.deploy"));
			tooltip.add(Component.translatable("tooltip.tkgun.summon_antenna.mark"));
			tooltip.add(Component.translatable("tooltip.tkgun.summon_antenna.cast"));
		}
	}
}
