package com.mechaniciancombat.client.entity;

import com.mechaniciancombat.TkGunServer;
import com.mechaniciancombat.entity.MoltenTridentEntity;
import com.mechaniciancombat.TkGun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownTridentRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** 熔铁矢专用三叉戟实体：使用资源包中的 3D 箭矢模型。 */
public class MoltenTridentEntityRenderer extends EntityRenderer<MoltenTridentEntity> {
	private static final Set<Integer> LOGGED = ConcurrentHashMap.newKeySet();
	private static final ResourceLocation STEEL_ARROW =
			new ResourceLocation("tkgun", "textures/entity/steel_arrow.png");
	private static final int STEEL_ARROW_FRAMES = 5;
	private static final float STEEL_ARROW_FPS = 12.0F;
	private static final float STEEL_ARROW_SIZE = 2.0F;
	private static final double STEEL_ARROW_TIP_OFFSET = 0.55D;

	public MoltenTridentEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void render(MoltenTridentEntity entity, float yaw, float tickDelta, PoseStack matrices,
					   MultiBufferSource bufferSource, int light) {
		Vec3 velocity = entity.getDeltaMovement();
		boolean flying = !entity.isStopped();
		Vec3 dir = velocity.lengthSqr() < 1.0E-6D
				? entity.getLookAngle() : velocity.normalize();
		matrices.pushPose();
		if (entity.isStopped()) {
			// 命中方块后箭头通常有一半嵌在方块里；沿反方向退回少量，
			// 保证插住的 3D 箭杆在大多数视角下仍可见。
			matrices.translate(-dir.x * 0.25D, -dir.y * 0.25D, -dir.z * 0.25D);
		}
		// 资源模型的 +Y（top）是箭矢前端：直接把 +Y 转到实体速度方向。
		matrices.mulPose(new Quaternionf().rotationTo(
				new Vector3f(0.0F, 1.0F, 0.0F),
				new Vector3f((float) dir.x, (float) dir.y, (float) dir.z)));
		ItemStack stack = entity.getWeaponItem();
		if (stack.isEmpty() && TkGunServer.BOLT != null) {
			stack = new ItemStack(TkGunServer.BOLT);
		}
		if (LOGGED.add(entity.getId())) {
			TkGun.LOGGER.info("[tkgun] 熔铁矢实体渲染: id={} pos=({}, {}, {}) stack={} item={}",
					entity.getId(), Math.round(entity.getX() * 100) / 100.0,
					Math.round(entity.getY() * 100) / 100.0, Math.round(entity.getZ() * 100) / 100.0,
					stack.getCount(), stack.getItem());
		}
		// ItemRenderer 内部已经 translate(-0.5, -0.5, -0.5)，这里不能再偏移，
		// 否则模型会整体偏离实体约 1 格（看起来就像没渲染）。
		Minecraft.getInstance().getItemRenderer().renderStatic(
				stack, ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY,
				matrices, bufferSource, entity.level(), entity.getId());
		matrices.popPose();
		if (flying) {
			renderSteelArrowEffect(entity, tickDelta, dir, matrices, bufferSource);
		}
		super.render(entity, yaw, tickDelta, matrices, bufferSource, light);
	}

	private void renderSteelArrowEffect(MoltenTridentEntity entity, float tickDelta, Vec3 dir,
										PoseStack matrices, MultiBufferSource bufferSource) {
		float seconds = (entity.tickCount + tickDelta) / 20.0F;
		float duration = STEEL_ARROW_FRAMES / STEEL_ARROW_FPS;
		if (seconds >= duration) {
			return;
		}
		int frame = Math.min(STEEL_ARROW_FRAMES - 1, (int) (seconds * STEEL_ARROW_FPS));
		float v0 = frame / (float) STEEL_ARROW_FRAMES;
		float v1 = (frame + 1) / (float) STEEL_ARROW_FRAMES;
		float half = STEEL_ARROW_SIZE / 2.0F;

		matrices.pushPose();
		matrices.translate(dir.x * STEEL_ARROW_TIP_OFFSET,
				dir.y * STEEL_ARROW_TIP_OFFSET,
				dir.z * STEEL_ARROW_TIP_OFFSET);
		PoseStack.Pose entry = matrices.last();
		entry.pose().rotate(this.entityRenderDispatcher.cameraOrientation());
		Matrix4f m = entry.pose();
		VertexConsumer vc = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(STEEL_ARROW));
		vc.vertex(m, -half, -half, 0.0F).color(255, 255, 255, 255).uv(0.0F, v1)
				.overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
				.normal(0.0F, 1.0F, 0.0F).endVertex();
		vc.vertex(m, half, -half, 0.0F).color(255, 255, 255, 255).uv(1.0F, v1)
				.overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
				.normal(0.0F, 1.0F, 0.0F).endVertex();
		vc.vertex(m, half, half, 0.0F).color(255, 255, 255, 255).uv(1.0F, v0)
				.overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
				.normal(0.0F, 1.0F, 0.0F).endVertex();
		vc.vertex(m, -half, half, 0.0F).color(255, 255, 255, 255).uv(0.0F, v0)
				.overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
				.normal(0.0F, 1.0F, 0.0F).endVertex();
		matrices.popPose();
	}

	@Override
	public ResourceLocation getTextureLocation(MoltenTridentEntity entity) {
		return STEEL_ARROW;
	}
}
