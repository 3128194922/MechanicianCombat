package com.mechaniciancombat.client;

import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * 枪口辉光：每发子弹在 locator 处生成一颗自发光 billboard。
 *
 * <p>作者模型自带 `fire1..4` 的枪口焰约 0.2083s（5 tick）。这颗粒子用同样时长：
 * 起始白黄、随后降到暗橙并淡出，避免辉光比火焰先消失或拖尾。
 */
public class MuzzleGlowParticle extends TextureSheetParticle {
	/** 与 `stand_shooting` / `Focus_shooting` 的 `fire1..4` 最末关键帧对齐。 */
	private static final int LIFETIME = 5;

	private final SpriteSet provider;

	protected MuzzleGlowParticle(ClientLevel world, double x, double y, double z,
								 SpriteSet provider) {
		super(world, x, y, z);
		this.provider = provider;
		this.lifetime = LIFETIME;
		this.quadSize = 1.20F;                  // 约一格大小的柔和光团
		this.setColor(1.0F, 0.95F, 0.72F);
		this.setAlpha(1.0F);
		this.hasPhysics = false;
		this.gravity = 0.0F;
		this.setSpriteFromAge(provider);
	}

	@Override
	public void tick() {
		super.tick();
		float t = this.lifetime <= 1 ? 1.0F : (float) this.age / (float) this.lifetime;
		// 白黄（亮）→ 暗橙（暗），同时透明度和尺寸衰减。
		this.setColor(1.0F, 1.0F - 0.60F * t, 0.95F - 0.88F * t);
		this.setAlpha(1.0F - t);
		this.quadSize = 1.20F * (1.0F - 0.22F * t);
		this.setSpriteFromAge(this.provider);
	}

	/** 粒子不参与世界光照，保持“发光”而不是随环境变暗。 */
	@Override
	protected int getLightColor(float tickDelta) {
		return LightTexture.FULL_BRIGHT;
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	public record Factory(SpriteSet provider) implements ParticleProvider<SimpleParticleType> {
		@Override
		public MuzzleGlowParticle createParticle(SimpleParticleType type, ClientLevel world,
												 double x, double y, double z,
												 double vx, double vy, double vz) {
			return new MuzzleGlowParticle(world, x, y, z, this.provider);
		}
	}
}
