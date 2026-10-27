package com.mechaniciancombat.screen;

import com.mechaniciancombat.TkGun;
import com.mechaniciancombat.item.AmmoItems;
import com.mechaniciancombat.item.GunAmmoData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;


/**
 * 机械师弹药环。
 *
 * <p>8 个环形槽位直接映射玩家背包中存档使用的
 * {@code 3,4,12,13,20,23,29,32}，其余背包槽仍然可见、可正常整理。
 * 环形槽只允许放入本模组弹药。
 */
public final class AmmoStorageScreenHandler extends AbstractContainerMenu {
	public static final int SIZE = GunAmmoData.SIZE;
	private static final boolean TRACE_CLICKS = Boolean.getBoolean("tkgun.traceAmmoMenuClicks");
	private static final int CENTER_X = 88;
	private static final int CENTER_Y = 56;
	private static final int RADIUS = 37;

	public static MenuType<AmmoStorageScreenHandler> TYPE;
	private final ContainerData selectedSlot;

	public AmmoStorageScreenHandler(int containerId, Inventory playerInventory) {
		super(TYPE, containerId);
		GunAmmoData.AmmoInventory serverInventory =
				playerInventory.player instanceof ServerPlayer serverPlayer
						? GunAmmoData.inventory(serverPlayer) : null;
		var inventory = serverInventory != null
				? serverInventory
				: new net.minecraft.world.SimpleContainer(SIZE);
		for (int index = 0; index < SIZE; index++) {
			addSlot(new Slot(inventory, index, slotX(index), slotY(index)) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return AmmoItems.isAmmo(stack);
				}
			});
		}
		for (int index = 0; index < 36; index++) {
			addSlot(new Slot(playerInventory, index, inventoryX(index), inventoryY(index)));
		}
		SimpleContainerData selected = new SimpleContainerData(1);
		selected.set(0, serverInventory == null ? 0 : serverInventory.selected());
		addDataSlots(selected);
		this.selectedSlot = selected;
	}

	public AmmoStorageScreenHandler(int containerId, Inventory playerInventory,
			net.minecraft.network.FriendlyByteBuf data) {
		this(containerId, playerInventory);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		if (slotIndex < 0 || slotIndex >= slots.size()) {
			return ItemStack.EMPTY;
		}
		Slot slot = slots.get(slotIndex);
		if (slotIndex < SIZE && !slot.hasItem()) {
			return depositAmmo(player, slot);
		}
		ItemStack original = slot.getItem();
		ItemStack copy = original.copy();
		if (slotIndex < SIZE) {
			player.getInventory().add(original);
		} else if (AmmoItems.isAmmo(original)
				&& moveItemStackTo(original, 0, SIZE, true)) {
			// 弹药优先进入环形仓库。
		} else {
			int inventoryIndex = slotIndex - SIZE;
			if (inventoryIndex < 9) {
				if (!moveItemStackTo(original, SIZE + 9, SIZE + 36, true)) {
					return ItemStack.EMPTY;
				}
			} else if (!moveItemStackTo(original, SIZE, SIZE + 9, true)) {
				return ItemStack.EMPTY;
			}
		}
		if (original.isEmpty()) {
			slot.set(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		return copy;
	}

	@Override
	public void clicked(int slotIndex, int button, ClickType type, Player player) {
		if (TRACE_CLICKS) {
			traceClick("before", slotIndex, button, type, player);
		}
		if (type == ClickType.QUICK_MOVE && slotIndex >= 0 && slotIndex < SIZE
				&& !slots.get(slotIndex).hasItem()) {
			depositAmmo(player, slots.get(slotIndex));
			if (TRACE_CLICKS) {
				traceClick("after", slotIndex, button, type, player);
			}
			return;
		}
		super.clicked(slotIndex, button, type, player);
		if (TRACE_CLICKS) {
			traceClick("after", slotIndex, button, type, player);
		}
	}

	private void traceClick(String phase, int slotIndex, int button, ClickType type, Player player) {
		ItemStack slotStack = slotIndex >= 0 && slotIndex < slots.size()
				? slots.get(slotIndex).getItem() : ItemStack.EMPTY;
		TkGun.LOGGER.info("[tkgun-ui-trace] side={} player={} phase={} click={} button={} type={} slot={}x{} cursor={}x{}",
				player.level().isClientSide ? "client" : "server", player.getName().getString(), phase,
				slotIndex, button, type, itemId(slotStack), slotStack.getCount(), itemId(getCarried()),
				getCarried().getCount());
	}

	private static String itemId(ItemStack stack) {
		return stack.isEmpty() ? "empty"
				: net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	public int getSelectedSlot() {
		return Math.floorMod(selectedSlot.get(0), SIZE);
	}

	public void setSelectedSlot(int slot) {
		selectedSlot.set(0, Math.floorMod(slot, SIZE));
	}

	private static ItemStack depositAmmo(Player player, Slot target) {
		Inventory inventory = player.getInventory();
		for (int i = 0; i < 36; i++) {
			ItemStack candidate = inventory.getItem(i);
			if (!AmmoItems.isAmmo(candidate)) {
				continue;
			}
			ItemStack moved = candidate.copy();
			target.set(moved);
			inventory.setItem(i, ItemStack.EMPTY);
			return moved.copy();
		}
		return ItemStack.EMPTY;
	}

	private static int inventoryX(int index) {
		// The texture paints each 18x18 box at the slot coordinate, while Slot.x/y
		// denotes the inset 16x16 item/highlight area.
		return 9 + (index < 9 ? index : (index - 9) % 9) * 18;
	}

	private static int inventoryY(int index) {
		return index < 9 ? 185 : 123 + ((index - 9) / 9) * 18;
	}

	public static int slotX(int index) {
		double angle = -Math.PI / 2.0D + index * Math.PI * 2.0D / SIZE;
		return Math.round(CENTER_X + (float) Math.cos(angle) * RADIUS) - 8;
	}

	public static int slotY(int index) {
		double angle = -Math.PI / 2.0D + index * Math.PI * 2.0D / SIZE;
		return Math.round(CENTER_Y + (float) Math.sin(angle) * RADIUS) - 8;
	}

	/**
	 * 打开 UI 用的代理工厂。
	 *
	 * <p>本工程没有 refmap；匿名实现 `NamedScreenHandlerFactory#getDisplayName` 时，
	 * 方法名不会被可靠重映射，运行期会缺 `method_5476()`。代理按参数/返回类型响应，
	 * 不依赖混淆后的方法名。
	 */
	public static net.minecraft.world.MenuProvider createFactory() {
		return new net.minecraft.world.SimpleMenuProvider(
				(containerId, inventory, player) ->
						new AmmoStorageScreenHandler(containerId, inventory),
				Component.translatable("ui.tkgun.ammo_storage"));
	}
}
