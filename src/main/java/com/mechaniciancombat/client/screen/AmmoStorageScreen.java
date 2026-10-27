package com.mechaniciancombat.client.screen;

import com.mechaniciancombat.screen.AmmoStorageScreenHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/** 上方独立弹药环 + 下方玩家背包。 */
public final class AmmoStorageScreen extends AbstractContainerScreen<AmmoStorageScreenHandler> {
	private static final ResourceLocation TEXTURE =
			new ResourceLocation("tkgun", "textures/gui/ammo_storage.png");
	private final List<int[]> ringCenters = new ArrayList<>();

	public AmmoStorageScreen(AmmoStorageScreenHandler handler, Inventory inventory, Component title) {
		super(handler, inventory, title);
		imageWidth = 176;
		imageHeight = 212;
		for (int index = 0; index < AmmoStorageScreenHandler.SIZE; index++) {
			ringCenters.add(new int[]{
					AmmoStorageScreenHandler.slotX(index) + 9,
					AmmoStorageScreenHandler.slotY(index) + 9});
		}
		double cx = ringCenters.stream().mapToInt(v -> v[0]).average().orElse(88.0D);
		double cy = ringCenters.stream().mapToInt(v -> v[1]).average().orElse(56.0D);
		ringCenters.sort(Comparator.comparingDouble(v -> Math.atan2(v[1] - cy, v[0] - cx)));
	}

	@Override
	protected void init() {
		super.init();
		// 整个面板（背景、槽位、点击区域共用 x/y）向左、向上各偏移 1 像素。
		leftPos -= 1;
		topPos -= 1;
	}

	/**
	 * `HandledScreens.register` 在 1.21.1 是 private；这里用动态代理实现它的 private
	 * {@code Provider} 接口，避免再给原版类加一个只用于注册的 mixin。
	 */
	public static void registerClientScreen() {
		try {
			// 生产环境会重映射类名/方法名，不能硬编码 register/Provider。
			// 通过结构寻找：一个只含 create(handler, inventory, title) 的私有接口 + 接收它的静态方法。
			Class<?> providerType = Arrays.stream(
							net.minecraft.client.gui.screens.MenuScreens.class.getDeclaredClasses())
					.filter(Class::isInterface)
					.filter(type -> Arrays.stream(type.getDeclaredMethods())
							.anyMatch(method -> "create".equals(method.getName())
									|| method.getParameterCount() == 3))
					.findFirst()
					.orElseThrow(() -> new IllegalStateException("HandledScreens.Provider not found"));
			Object provider = Proxy.newProxyInstance(
					AmmoStorageScreen.class.getClassLoader(),
					new Class<?>[]{providerType},
					(proxy, method, args) -> {
						if ("create".equals(method.getName())) {
							return new AmmoStorageScreen(
									(AmmoStorageScreenHandler) args[0],
									(Inventory) args[1],
									(Component) args[2]);
						}
						return null;
					});
			Method register = Arrays.stream(
							net.minecraft.client.gui.screens.MenuScreens.class.getDeclaredMethods())
					.filter(method -> method.getParameterCount() == 2)
					.filter(method -> method.getParameterTypes()[0]
							.isAssignableFrom(net.minecraft.world.inventory.MenuType.class))
					.filter(method -> method.getParameterTypes()[1] == providerType)
					.findFirst()
					.orElseThrow(() -> new IllegalStateException("HandledScreens.register not found"));
			register.setAccessible(true);
			register.invoke(null, AmmoStorageScreenHandler.TYPE, provider);
		} catch (Throwable t) {
			com.mechaniciancombat.TkGun.LOGGER.error("[tkgun] 无法注册机械师弹药环屏幕", t);
		}
	}

	@Override
	public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		// HandledScreen itself does not draw slot tooltips; inventory-like screens do it here.
		renderTooltip(context, mouseX, mouseY);
	}

	/** Acceptance-only helpers that dispatch through this screen's normal slot hit-testing path. */
	public boolean acceptanceClickSlot(int menuSlot) {
		if (menuSlot < 0 || menuSlot >= menu.slots.size()) {
			return false;
		}
		Slot slot = menu.getSlot(menuSlot);
		double mouseX = leftPos + slot.x + 8.0D;
		double mouseY = topPos + slot.y + 8.0D;
		int hit = acceptanceHitSlotIndex(mouseX, mouseY);
		if (hit != menuSlot) {
			com.mechaniciancombat.TkGun.LOGGER.error(
					"[tkgun-ui] acceptance click target={} actualHit={} gui=({},{}), leftTop=({},{}), slot=({}, {})",
					menuSlot, hit, mouseX, mouseY, leftPos, topPos, slot.x, slot.y);
			return false;
		}
		// Screen.mouseClicked only starts the vanilla quick-craft gesture when the
		// cursor already carries a stack. A real mouse also sends the drag and
		// release callbacks; dispatch the complete gesture here so acceptance
		// exercises the same placement path as an actual click-and-release.
		boolean handled = mouseClicked(mouseX, mouseY, 0);
		if (handled) {
			mouseDragged(mouseX, mouseY, 0, 0.0D, 0.0D);
			mouseReleased(mouseX, mouseY, 0);
		}
		com.mechaniciancombat.TkGun.LOGGER.info(
				"[tkgun-ui] acceptance click target={} actualHit={} handled={} cursorBefore={}x{} slotBefore={}x{}",
				menuSlot, hit, handled, itemName(menu.getCarried()), menu.getCarried().getCount(),
				itemName(slot.getItem()), slot.getItem().getCount());
		return handled;
	}

	public int acceptanceHitSlotIndex(double mouseX, double mouseY) {
		for (int index = 0; index < menu.slots.size(); index++) {
			Slot slot = menu.getSlot(index);
			if (slot.isActive()
					&& mouseX >= leftPos + slot.x && mouseX < leftPos + slot.x + 16.0D
					&& mouseY >= topPos + slot.y && mouseY < topPos + slot.y + 16.0D) {
				return index;
			}
		}
		return -1;
	}

	private static String itemName(net.minecraft.world.item.ItemStack stack) {
		return stack.isEmpty() ? "empty"
				: net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
	}

	public net.minecraft.world.item.ItemStack acceptanceSlotStack(int menuSlot) {
		if (menuSlot < 0 || menuSlot >= menu.slots.size()) {
			return net.minecraft.world.item.ItemStack.EMPTY;
		}
		return menu.getSlot(menuSlot).getItem().copy();
	}

	public net.minecraft.world.item.ItemStack acceptanceCarriedStack() {
		return menu.getCarried().copy();
	}

	@Override
	protected void renderBg(GuiGraphics context, float delta, int mouseX, int mouseY) {
		context.blit(TEXTURE, leftPos, topPos, 0, 0, 176, 212, 176, 212);
		int selected = menu.getSelectedSlot();
		int sx = leftPos + AmmoStorageScreenHandler.slotX(selected);
		int sy = topPos + AmmoStorageScreenHandler.slotY(selected);
		context.fill(sx, sy, sx + 16, sy + 16, 0x66FFD24A);
	}

	@Override
	protected void renderLabels(GuiGraphics context, int mouseX, int mouseY) {
		// 环形窗口保持紧凑，不额外绘制标题/背包标题。
	}

}
