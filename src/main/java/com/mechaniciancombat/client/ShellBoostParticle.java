package com.mechaniciancombat.client;

import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * **二级加速弹壳**（v4 素材 `粒子\二级加速弹壳.png`，6 帧 48x48）：
 * 专注连射射速提上来之后甩出去的弹壳 —— 逐帧翻页 + 一点重力 + 会旋转着掉。
 *
 * <p>和服务端那簇 `heat_vent` 一样，位置/初速都是服务端给的（`TkGunServer#fireOnce` 从抛壳口甩），
 * 这里只管"长什么样、怎么飘"。
 */
public class ShellBoostParticle extends TextureSheetParticle {
	private final SpriteSet provider;

	protected ShellBoostParticle(ClientLevel world, double x, double y, double z,
								 double vx, double vy, double vz, SpriteSet provider) {
		super(world, x, y, z);
		this.provider = provider;
		this.lifetime = 14 + this.random.nextInt(6);
		this.quadSize = 0.16F + this.random.nextFloat() * 0.06F;
		this.xd = vx;
		this.yd = vy;
		this.zd = vz;
		this.gravity = 0.9F;       // 弹壳会掉下来
		this.hasPhysics = true;
		this.setSpriteFromAge(provider);
	}

	@Override
	public void tick() {
		super.tick();
		this.setSpriteFromAge(this.provider);   // 6 帧逐帧翻页
		float life = (float) this.age / (float) this.lifetime;
		this.setAlpha(life > 0.7F ? Math.max(0.0F, 1.0F - (life - 0.7F) / 0.3F) : 1.0F);
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	public record Factory(SpriteSet provider) implements ParticleProvider<SimpleParticleType> {
		@Override
		public ShellBoostParticle createParticle(SimpleParticleType type, ClientLevel world,
												 double x, double y, double z,
												 double vx, double vy, double vz) {
			return new ShellBoostParticle(world, x, y, z, vx, vy, vz, this.provider);
		}
	}
}
