package com.mechaniciancombat.item;

import com.mechaniciancombat.TkGun;
import com.mechaniciancombat.screen.AmmoStorageScreenHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.core.NonNullList;

/** 绑定在枪械 ItemStack 上的 8 格弹仓与当前选中槽。 */
public final class GunAmmoData {
	public static final int SIZE = AmmoRingRules.SIZE;
	private static final String ROOT = "tkgun_ammo";
	private static final String SELECTED = "Selected";
	private static final String MAG = "tkgun_mag";
	private static final String MAG_AMMO = "tkgun_mag_ammo";
	private static final String MAG_VERSION = "tkgun_mag_version";

	public static ItemStack heldGun(Player player) {
		ItemStack stack = player.getMainHandItem();
		return TkGun.isGun(stack) ? stack : ItemStack.EMPTY;
	}

	private static net.minecraft.core.RegistryAccess lookup(Player player) {
		return player.level().registryAccess();
	}

	public static NonNullList<ItemStack> read(ItemStack gun, net.minecraft.core.RegistryAccess lookup) {
		NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
		CompoundTag root = StackNbt.copy(gun).getCompound(ROOT);
		ContainerHelper.loadAllItems(root, items);
		return items;
	}

	public static int selected(ItemStack gun) {
		CompoundTag root = StackNbt.copy(gun).getCompound(ROOT);
		return AmmoRingRules.normalizeSelected(root.getByte(SELECTED));
	}

	/** 枪械是否已经保存过弹匣状态（用于区分新枪与“故意打空”的旧枪）。 */
	public static boolean hasSavedMagazine(ItemStack gun) {
		CompoundTag custom = StackNbt.copy(gun);
		return custom.contains(MAG_AMMO);
	}

	public static int savedMagazineCount(ItemStack gun) {
		CompoundTag custom = StackNbt.copy(gun);
		return custom.getInt(MAG);
	}

	public static String savedMagazineAmmo(ItemStack gun) {
		CompoundTag custom = StackNbt.copy(gun);
		return custom.getString(MAG_AMMO);
	}

	public static int savedMagazineVersion(ItemStack gun) {
		CompoundTag custom = StackNbt.copy(gun);
		return custom.getInt(MAG_VERSION);
	}

	/** 把腰射弹匣的“已打掉发数 + 弹种”写回枪械 NBT，跨存档保留。 */
	public static void saveMagazine(ItemStack gun, int mag, String ammoId) {
		StackNbt.update(gun, custom -> {
			custom.putInt(MAG, Math.max(0, mag));
			custom.putString(MAG_AMMO, ammoId == null ? "" : ammoId);
			custom.putInt(MAG_VERSION, 2);
		});
	}

	public static ItemStack selectedAmmo(Player player) {
		ItemStack gun = heldGun(player);
		if (gun.isEmpty()) {
			return ItemStack.EMPTY;
		}
		return read(gun, lookup(player)).get(selected(gun));
	}

	public static boolean selectedIsExplosive(Player player) {
		String path = selectedAmmoPath(player);
		return "explosive_bolt".equals(path);
	}

	public static boolean selectedIsCutting(Player player) {
		return "cutting_bolt".equals(selectedAmmoPath(player));
	}

	public static String selectedAmmoId(Player player) {
		ItemStack ammo = selectedAmmo(player);
		return ammo.isEmpty() ? ""
				: net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(ammo.getItem()).toString();
	}

	/** 用注册 ID 建一个无附魔的弹药模板；无效 ID 返回空栈。 */
	public static ItemStack stackForId(String ammoId) {
		net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(ammoId);
		if (id == null) {
			return ItemStack.EMPTY;
		}
		net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);
		return item == net.minecraft.world.item.Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
	}

	private static String selectedAmmoPath(Player player) {
		ItemStack ammo = selectedAmmo(player);
		return ammo.isEmpty() ? ""
				: net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(ammo.getItem()).getPath();
	}

	public static void save(ItemStack gun, NonNullList<ItemStack> items, int selected,
							net.minecraft.core.RegistryAccess lookup) {
		StackNbt.update(gun, custom -> {
			CompoundTag root = new CompoundTag();
			ContainerHelper.saveAllItems(root, items);
			root.putByte(SELECTED, (byte) Math.floorMod(selected, SIZE));
			custom.put(ROOT, root);
		});
	}

	/** direction = -1 逆时针上一槽，+1 顺时针下一槽。 */
	public static int cycle(Player player, int direction) {
		ItemStack gun = heldGun(player);
		if (gun.isEmpty()) {
			return 0;
		}
		if (!AmmoRingRules.isValidDirection(direction)) {
			return selected(gun);
		}
		net.minecraft.core.RegistryAccess lookup = lookup(player);
		NonNullList<ItemStack> items = read(gun, lookup);
		int selected = AmmoRingRules.nextSelected(selected(gun), direction);
		save(gun, items, selected, lookup);
		player.getInventory().setChanged();
		if (player.containerMenu instanceof AmmoStorageScreenHandler handler) {
			handler.setSelectedSlot(selected);
		}
		ItemStack ammo = items.get(selected);
		player.displayClientMessage(Component.translatable("text.tkgun.current_ammo",
				ammo.isEmpty() ? Component.translatable("text.tkgun.empty") : ammo.getHoverName()), true);
		return selected;
	}

	/** Selects the current stack of an ammo type while preserving the ring contents. */
	public static boolean selectAmmoType(Player player, String ammoId) {
		ItemStack gun = heldGun(player);
		if (gun.isEmpty() || ammoId == null || ammoId.isEmpty()) {
			return false;
		}
		net.minecraft.core.RegistryAccess lookup = lookup(player);
		NonNullList<ItemStack> items = read(gun, lookup);
		int current = selected(gun);
		if (stackMatches(items.get(current), ammoId)) {
			return true;
		}
		for (int slot = 0; slot < SIZE; slot++) {
			if (stackMatches(items.get(slot), ammoId)) {
				save(gun, items, slot, lookup);
				player.getInventory().setChanged();
				return true;
			}
		}
		return false;
	}

	private static boolean stackMatches(ItemStack stack, String ammoId) {
		return !stack.isEmpty()
				&& ammoId.equals(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
	}

	/** 从当前选中槽消耗弹药。 */
	public static boolean consume(Player player, int amount) {
		if (!AmmoRingRules.isValidConsumeAmount(amount)) {
			return false;
		}
		ItemStack gun = heldGun(player);
		if (gun.isEmpty()) {
			return false;
		}
		net.minecraft.core.RegistryAccess lookup = lookup(player);
		NonNullList<ItemStack> items = read(gun, lookup);
		int selected = selected(gun);
		ItemStack ammo = items.get(selected);
		if (ammo.isEmpty() || ammo.getCount() < amount || !AmmoItems.isAmmo(ammo)) {
			player.displayClientMessage(Component.translatable("text.tkgun.current_ammo",
					Component.translatable("text.tkgun.empty")), true);
			return false;
		}
		ammo.shrink(amount);
		if (ammo.isEmpty()) {
			items.set(selected, ItemStack.EMPTY);
		}
		save(gun, items, selected, lookup);
		player.getInventory().setChanged();
		return true;
	}

	/**
	 * 从所有相同弹种槽位原子消耗 amount 发。
	 * 先扣当前选中槽，再扣其余同弹种槽；若选中槽被扣空，自动切到下一个同弹种槽。
	 */
	public static boolean consumeExpected(Player player, String expectedId, int amount) {
		ItemStack gun = heldGun(player);
		if (gun.isEmpty() || !AmmoRingRules.isValidConsumeAmount(amount)
				|| expectedId == null || expectedId.isEmpty()) {
			return false;
		}
		net.minecraft.core.RegistryAccess lookup = lookup(player);
		NonNullList<ItemStack> items = read(gun, lookup);
		int selected = selected(gun);
		int total = 0;
		for (ItemStack stack : items) {
			if (!stack.isEmpty() && expectedId.equals(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())) {
				total += stack.getCount();
			}
		}
		if (total < amount) {
			return false;
		}
		int remaining = consumeFrom(items, selected, expectedId, amount);
		for (int slot = 0; slot < SIZE && remaining > 0; slot++) {
			if (slot == selected) {
				continue;
			}
			remaining = consumeFrom(items, slot, expectedId, remaining);
		}
		if (remaining != 0) {
			return false;
		}
		int newSelected = selected;
		if (items.get(selected).isEmpty()) {
			for (int slot = 0; slot < SIZE; slot++) {
				ItemStack stack = items.get(slot);
				if (!stack.isEmpty() && expectedId.equals(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())) {
					newSelected = slot;
					break;
				}
			}
		}
		save(gun, items, newSelected, lookup);
		player.getInventory().setChanged();
		return true;
	}

	private static int consumeFrom(NonNullList<ItemStack> items, int slot, String expectedId,
								   int remaining) {
		ItemStack stack = items.get(slot);
		if (stack.isEmpty() || !expectedId.equals(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())) {
			return remaining;
		}
		int take = Math.min(remaining, stack.getCount());
		stack.shrink(take);
		if (stack.isEmpty()) {
			items.set(slot, ItemStack.EMPTY);
		}
		return remaining - take;
	}

	/** Return rounds to a matching stack or an empty gun-ammo slot. */
	public static boolean refund(Player player, String ammoId, int amount) {
		if (amount <= 0) {
			return true;
		}
		ItemStack template = stackForId(ammoId);
		return !template.isEmpty() && refund(player, template, amount);
	}

	/** 用带附魔/组件的模板退还弹药，优先合并到组件完全相同的堆叠。 */
	public static boolean refund(Player player, ItemStack template, int amount) {
		return refund(player, heldGun(player), template, amount);
	}

	public static boolean refund(Player player, ItemStack gun, ItemStack template, int amount) {
		if (amount <= 0) {
			return true;
		}
		if (template.isEmpty() || !AmmoItems.isAmmo(template)) {
			return false;
		}
		if (gun.isEmpty()) {
			return false;
		}
		net.minecraft.core.RegistryAccess lookup = lookup(player);
		NonNullList<ItemStack> items = read(gun, lookup);
		int maxCount = template.getMaxStackSize();
		int capacity = 0;
		for (ItemStack stack : items) {
			if (stack.isEmpty()) {
				capacity += maxCount;
			} else if (ItemStack.isSameItemSameTags(stack, template)) {
				capacity += Math.max(0, maxCount - stack.getCount());
			}
		}
		if (capacity < amount) {
			return false;
		}
		int remaining = amount;
		for (ItemStack stack : items) {
			if (stack.isEmpty() || !ItemStack.isSameItemSameTags(stack, template)) {
				continue;
			}
			int moved = Math.min(remaining, maxCount - stack.getCount());
			stack.grow(moved);
			remaining -= moved;
			if (remaining == 0) {
				break;
			}
		}
		for (int slot = 0; slot < items.size() && remaining > 0; slot++) {
			if (!items.get(slot).isEmpty()) {
				continue;
			}
			int moved = Math.min(remaining, maxCount);
			items.set(slot, template.copyWithCount(moved));
			remaining -= moved;
		}
		if (remaining != 0) {
			return false;
		}
		save(gun, items, selected(gun), lookup);
		player.getInventory().setChanged();
		return true;
	}

	public static AmmoInventory inventory(Player player) {
		ItemStack gun = heldGun(player);
		net.minecraft.core.RegistryAccess lookup = lookup(player);
		return new AmmoInventory(player, gun, read(gun, lookup), selected(gun), lookup);
	}

	public static final class AmmoInventory implements Container {
		private final Player player;
		private final ItemStack gun;
		private final NonNullList<ItemStack> items;
		private int selected;
		private final net.minecraft.core.RegistryAccess lookup;

		private AmmoInventory(Player player, ItemStack gun, NonNullList<ItemStack> items,
							  int selected, net.minecraft.core.RegistryAccess lookup) {
			this.player = player;
			this.gun = gun;
			this.items = items;
			this.selected = selected;
			this.lookup = lookup;
		}

		public int selected() {
			return selected;
		}

		public void select(int slot) {
			selected = Math.floorMod(slot, SIZE);
			setChanged();
		}

		@Override
		public int getContainerSize() {
			return SIZE;
		}

		@Override
		public boolean isEmpty() {
			return items.stream().allMatch(ItemStack::isEmpty);
		}

		@Override
		public ItemStack getItem(int slot) {
			return items.get(slot);
		}

		@Override
		public ItemStack removeItem(int slot, int amount) {
			ItemStack stack = ContainerHelper.removeItem(items, slot, amount);
			if (!stack.isEmpty()) {
				setChanged();
			}
			return stack;
		}

		@Override
		public ItemStack removeItemNoUpdate(int slot) {
			ItemStack stack = ContainerHelper.takeItem(items, slot);
			if (!stack.isEmpty()) {
				setChanged();
			}
			return stack;
		}

		@Override
		public void setItem(int slot, ItemStack stack) {
			items.set(slot, stack);
			setChanged();
		}

		@Override
		public void setChanged() {
			save(gun, items, selected, lookup);
			player.getInventory().setChanged();
		}

		@Override
		public boolean stillValid(Player player) {
			return this.player == player && !gun.isEmpty();
		}

		@Override
		public boolean canPlaceItem(int slot, ItemStack stack) {
			return AmmoItems.isAmmo(stack);
		}

		@Override
		public void clearContent() {
			items.clear();
			setChanged();
		}
	}

	private GunAmmoData() {
	}
}

/** Pure rules for the fixed-size weapon ammunition ring. */
final class AmmoRingRules {
	static final int SIZE = 8;

	static int normalizeSelected(int selected) {
		return Math.floorMod(selected, SIZE);
	}

	static boolean isValidDirection(int direction) {
		return direction == -1 || direction == 1;
	}

	static boolean isValidConsumeAmount(int amount) {
		return amount > 0;
	}

	static int nextSelected(int selected, int direction) {
		int normalized = normalizeSelected(selected);
		return isValidDirection(direction) ? Math.floorMod(normalized + direction, SIZE) : normalized;
	}

	private AmmoRingRules() {
	}
}
