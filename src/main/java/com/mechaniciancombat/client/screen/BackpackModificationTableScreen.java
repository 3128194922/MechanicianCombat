package com.mechaniciancombat.client.screen;

import com.mechaniciancombat.client.CombatBackpackGeoRenderer;
import com.mechaniciancombat.item.CombatBackpackItem;
import com.mechaniciancombat.screen.BackpackModificationTableScreenHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

/**
 * Authored backpack-modification layout.
 *
 * <p>The large preview only renders the combat backpack placed in the dedicated
 * slot. Its authored group pivot is placed at the frame center and uses a fixed
 * view angle; the drawing region is clipped to the inside of the frame.</p>
 */
public final class BackpackModificationTableScreen
		extends AbstractContainerScreen<BackpackModificationTableScreenHandler> {
	private static final ResourceLocation TEXTURE =
			new ResourceLocation("tkgun", "textures/gui/container/backpack_modification_table.png");
	private static final int TEXTURE_WIDTH = 256;
	private static final int TEXTURE_HEIGHT = 256;
	private static final int PREVIEW_X = 52;
	private static final int PREVIEW_Y = 7;
	private static final int PREVIEW_WIDTH = 71;
	private static final int PREVIEW_HEIGHT = 72;
	private static final float PREVIEW_CENTER_X = PREVIEW_X + 35.0F;
	private static final float BACKPACK_SCALE = 50.413567F;
	private static final float BACKPACK_OFFSET_X = -9.5F;
	private static final float BACKPACK_OFFSET_Y = -19.809195F;
	private static final float FIXED_YAW = -45.0F;
	private static final float FIXED_PITCH = 22.5F;

	public BackpackModificationTableScreen(
			BackpackModificationTableScreenHandler handler,
			Inventory inventory,
			Component title) {
		super(handler, inventory, title);
		imageWidth = 176;
		imageHeight = 166;
	}

	/** Acceptance-only full mouse gesture used by the isolated UI harness. */
	public boolean acceptanceClickSlot(int menuSlot) {
		if (menuSlot < 0 || menuSlot >= menu.slots.size()) {
			return false;
		}
		var slot = menu.getSlot(menuSlot);
		double mouseX = leftPos + slot.x + 8.0D;
		double mouseY = topPos + slot.y + 8.0D;
		boolean handled = mouseClicked(mouseX, mouseY, 0);
		if (handled) {
			mouseDragged(mouseX, mouseY, 0, 0.0D, 0.0D);
			mouseReleased(mouseX, mouseY, 0);
		}
		com.mechaniciancombat.TkGun.LOGGER.info(
				"[tkgun-table-ui] click slot={} handled={} cursor={}x{} target={}x{}",
				menuSlot, handled, itemName(menu.getCarried()), menu.getCarried().getCount(),
				itemName(slot.getItem()), slot.getItem().getCount());
		return handled;
	}

	public ItemStack acceptanceSlotStack(int menuSlot) {
		return menuSlot < 0 || menuSlot >= menu.slots.size()
				? ItemStack.EMPTY : menu.getSlot(menuSlot).getItem().copy();
	}

	public ItemStack acceptanceCarriedStack() {
		return menu.getCarried().copy();
	}

	private static String itemName(ItemStack stack) {
		return stack.isEmpty() ? "empty"
				: net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
	}

	@Override
	public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		renderTooltip(context, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics context, float delta, int mouseX, int mouseY) {
		context.blit(
				TEXTURE,
				leftPos, topPos,
				0.0F, 0.0F,
				imageWidth, imageHeight,
				TEXTURE_WIDTH, TEXTURE_HEIGHT);
		ItemStack backpack = menu.getSlot(
				BackpackModificationTableScreenHandler.BACKPACK_SLOT_INDEX).getItem();
		if (!(backpack.getItem() instanceof CombatBackpackItem)) {
			return;
		}
		context.enableScissor(
				leftPos + PREVIEW_X + 1,
				topPos + PREVIEW_Y + 1,
				leftPos + PREVIEW_X + PREVIEW_WIDTH - 1,
				topPos + PREVIEW_Y + PREVIEW_HEIGHT - 1);
		try {
			drawBackpackPreview(context, backpack);
		} finally {
			context.disableScissor();
		}
	}

	private void drawBackpackPreview(GuiGraphics context, ItemStack stack) {
		var matrices = context.pose();
		matrices.pushPose();
		try {
			matrices.translate(
					leftPos + PREVIEW_CENTER_X + BACKPACK_OFFSET_X,
					topPos + PREVIEW_Y + PREVIEW_HEIGHT / 2.0F
							+ BACKPACK_OFFSET_Y,
					120.0D);
			matrices.scale(BACKPACK_SCALE, BACKPACK_SCALE, BACKPACK_SCALE);
			// The authored GUI orientation is upside down for this item renderer.
			matrices.scale(1.0F, -1.0F, 1.0F);
			matrices.mulPose(new Quaternionf().rotationXYZ(
					(float) Math.toRadians(FIXED_PITCH),
					(float) Math.toRadians(FIXED_YAW),
					0.0F));
			CombatBackpackGeoRenderer.getPreview().renderMaintenancePreview(
					stack,
					matrices,
					context.bufferSource(),
					0x00F000F0,
					OverlayTexture.NO_OVERLAY);
		} finally {
			matrices.popPose();
		}
	}

	@Override
	protected void renderLabels(GuiGraphics context, int mouseX, int mouseY) {
		// The authored panel has no separate title region.
	}

	/**
	 * Screen registration stays reflection-based because this build does not carry
	 * a Mixin refmap. The provider signature is the same shape used by the ammo
	 * storage screen.
	 */
	public static void registerClientScreen() {
		try {
			Class<?> providerType = Arrays.stream(MenuScreens.class.getDeclaredClasses())
					.filter(Class::isInterface)
					.filter(type -> Arrays.stream(type.getDeclaredMethods())
							.anyMatch(method -> "create".equals(method.getName())
									|| method.getParameterCount() == 3))
					.findFirst()
					.orElseThrow(() -> new IllegalStateException("HandledScreens.Provider not found"));
			Object provider = Proxy.newProxyInstance(
					BackpackModificationTableScreen.class.getClassLoader(),
					new Class<?>[]{providerType},
					(proxy, method, args) -> {
						if ("create".equals(method.getName())) {
							return new BackpackModificationTableScreen(
									(BackpackModificationTableScreenHandler) args[0],
									(Inventory) args[1],
									(Component) args[2]);
						}
						return null;
					});
			Method register = Arrays.stream(MenuScreens.class.getDeclaredMethods())
					.filter(method -> method.getParameterCount() == 2)
					.filter(method -> method.getParameterTypes()[0]
							.isAssignableFrom(net.minecraft.world.inventory.MenuType.class))
					.filter(method -> method.getParameterTypes()[1] == providerType)
					.findFirst()
					.orElseThrow(() -> new IllegalStateException("HandledScreens.register not found"));
			register.setAccessible(true);
			register.invoke(null, BackpackModificationTableScreenHandler.TYPE, provider);
		} catch (Throwable error) {
			com.mechaniciancombat.TkGun.LOGGER.error(
					"[tkgun] Unable to register backpack modification table screen", error);
		}
	}
}
