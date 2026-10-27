package com.mechaniciancombat.item;

import com.mechaniciancombat.TkGun;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public final class CombatBackpackItem extends Item implements GeoItem, ICurioItem {
	private static final RawAnimation AIM =
			RawAnimation.begin().thenPlayAndHold("aim");
	private static final RawAnimation CD =
			RawAnimation.begin().thenPlayAndHold("CD");
	private static final RawAnimation READY =
			RawAnimation.begin().thenPlayAndHold("ready");
	private static final RawAnimation MAINTENANCE =
			RawAnimation.begin().thenPlayAndHold("Maintenance");
	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private final String textureName;

	public CombatBackpackItem(Properties settings) {
		this(settings, "combat_backpack");
	}

	public CombatBackpackItem(Properties settings, String textureName) {
		super(settings);
		this.textureName = textureName;
	}

	public static ItemStack equippedStack(LivingEntity entity) {
		var inventory = CuriosApi.getCuriosInventory(entity).resolve().orElse(null);
		if (inventory == null) {
			return ItemStack.EMPTY;
		}
		var backSlot = inventory.getStacksHandler("back").orElse(null);
		if (backSlot == null) {
			return ItemStack.EMPTY;
		}
		for (int index = 0; index < backSlot.getStacks().getSlots(); index++) {
			ItemStack stack = backSlot.getStacks().getStackInSlot(index);
			if (stack.getItem() instanceof CombatBackpackItem) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	public ResourceLocation textureLocation() {
		return TkGun.id("textures/entity/" + textureName + ".png");
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(this, "equipped", 0,
				state -> state.setAndContinue(switch (CombatBackpackAnimation.renderingPhase()) {
					case CD -> CD;
					case READY -> READY;
					case MAINTENANCE -> MAINTENANCE;
					default -> AIM;
				})));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
