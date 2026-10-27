package com.mechaniciancombat.client.entity;

import com.mechaniciancombat.entity.BoltProjectileEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceLocation;

/**
 * 弩矢弹丸的客户端渲染器：**什么都不画**。
 *
 * <p>飞出去的样子全靠服务端每 tick 沿路径铺的曳光粒子（Flame/Smoke）+ 命中时的爆裂粒子，
 * 所以这里连模型都不需要有（返回一张已存在的贴图当占位，满足父类抽象方法即可）。
 * 这样也彻底断了"看起来像原版箭"的可能——用户 2026-10-12 反复强调过。
 */
public class BoltProjectileRenderer extends EntityRenderer<BoltProjectileEntity> {
	private static final ResourceLocation DUMMY = new ResourceLocation("tkgun", "textures/entity/bowgun_gun.png");

	public BoltProjectileRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void render(BoltProjectileEntity entity, float yaw, float tickDelta, PoseStack matrices,
					   MultiBufferSource bufferSource, int light) {
		// 故意空实现：弹体不画。
	}

	@Override
	public ResourceLocation getTextureLocation(BoltProjectileEntity entity) {
		return DUMMY;
	}
}
