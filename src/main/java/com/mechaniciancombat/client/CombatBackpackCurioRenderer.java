package com.mechaniciancombat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/** Curios renderer for the combat backpack on vanilla player renderers. */
public final class CombatBackpackCurioRenderer implements ICurioRenderer {
	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack,
			SlotContext slotContext, PoseStack matrices, RenderLayerParent<T, M> renderLayerParent,
			MultiBufferSource consumers, int light, float limbAngle, float limbDistance,
			float tickDelta, float animationProgress, float yHeadRot, float headPitch) {
		LivingEntity wearer = slotContext.entity();
		if (wearer == Minecraft.getInstance().player
				&& Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
			return;
		}
		if (!(renderLayerParent.getModel() instanceof PlayerModel playerModel)) {
			return;
		}
		matrices.pushPose();
		try {
			ICurioRenderer.translateIfSneaking(matrices, wearer);
			ICurioRenderer.rotateIfSneaking(matrices, wearer);
			playerModel.body.translateAndRotate(matrices);
			// GeckoLib 已在烘焙时反转几何 X 轴，因此此处不要再次镜像。
			// 模型宽 8 像素，从 X=0 延伸到 X=0.5；左移 0.25 格使其在躯干居中。
			// 从身体枢轴下移 0.50 格，将模型落在背部躯干而不是头部后方。
			// Z 范围为 0..0.25，向后偏移 0.125 格后贴合躯干背面。
			matrices.translate(-0.25D, 0.50D, 0.125D);
			CombatBackpackGeoRenderer.getAccessory().renderAttached(
					stack, wearer, matrices, consumers, light, tickDelta);
		} finally {
			matrices.popPose();
		}
	}
}
