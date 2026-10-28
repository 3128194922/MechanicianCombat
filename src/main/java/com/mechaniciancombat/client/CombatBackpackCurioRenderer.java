package com.mechaniciancombat.client;

import com.mechaniciancombat.TkGun;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
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
		// The gun rig attaches the backpack to its animated chest bone while held.
		if (wearer instanceof net.minecraft.world.entity.player.Player player && TkGun.holdingGun(player)) {
			return;
		}
		if (wearer == Minecraft.getInstance().player
				&& Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
			return;
		}
		if (!(renderLayerParent.getModel() instanceof PlayerModel playerModel)) {
			return;
		}
		matrices.pushPose();
		try {
			applyBodyAttachment(matrices, playerModel.body);
			CombatBackpackGeoRenderer.getAccessory().renderAttached(
					stack, wearer, matrices, consumers, light, tickDelta);
		} finally {
			matrices.popPose();
		}
	}

	static void applyBodyAttachment(PoseStack matrices, ModelPart body) {
		// The player model already includes its crouch translation and rotation.
		body.translateAndRotate(matrices);
		// LivingEntityRenderer flips X and Y for ModelPart; restore the geo model frame.
		matrices.translate(0.25D, 0.0D, 0.125D);
		matrices.scale(-1.0F, -1.0F, 1.0F);
	}
}
