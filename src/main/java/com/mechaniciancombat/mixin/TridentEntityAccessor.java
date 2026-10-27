package com.mechaniciancombat.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 让自定义熔铁弩矢实体能初始化三叉戟的忠诚/附魔光效数据。 */
@Mixin(ThrownTrident.class)
public interface TridentEntityAccessor {
	@Accessor("tridentItem")
	ItemStack tkgun$getTridentItem();

	@Mutable
	@Accessor("tridentItem")
	void tkgun$setTridentItem(ItemStack stack);

	@Accessor("ID_LOYALTY")
	static EntityDataAccessor<Byte> tkgun$getLoyaltyData() {
		throw new AssertionError();
	}

	@Accessor("ID_FOIL")
	static EntityDataAccessor<Boolean> tkgun$getEnchantedData() {
		throw new AssertionError();
	}

	@Accessor("dealtDamage")
	void tkgun$setDealtDamage(boolean value);
}
