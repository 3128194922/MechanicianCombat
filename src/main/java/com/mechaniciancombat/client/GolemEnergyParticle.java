package com.mechaniciancombat.client;

import com.mechaniciancombat.entity.MoltenGolemEntity;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LightTexture;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.phys.Vec3;

/** Energy convergence that follows the golem's animated locator_right bone. */
public final class GolemEnergyParticle extends TextureSheetParticle {
	private final SpriteSet provider;
	private final int ownerEntityId;
	private final int generation;
	private final boolean normalConverge;

	private GolemEnergyParticle(ClientLevel world, double x, double y, double z,
								SpriteSet provider, int ownerEntityId,
								float scale, int generation) {
		super(world, x, y, z);
		this.provider = provider;
		this.ownerEntityId = ownerEntityId;
		this.generation = generation;
		this.normalConverge = scale < 1.5F;
		this.lifetime = 20;
		this.quadSize = scale;
		this.xd = 0.0D;
		this.yd = 0.0D;
		this.zd = 0.0D;
		this.gravity = 0.0F;
		this.hasPhysics = false;
		this.setSpriteFromAge(provider);
	}

	@Override
	public void tick() {
		if (superseded()) {
			remove();
			return;
		}
		super.tick();
		if (this.age < this.lifetime) {
			updateLocator();
		}
		this.setSpriteFromAge(this.provider);
		float life = (float) this.age / (float) this.lifetime;
		this.setAlpha(Math.max(0.0F, 1.0F - life * life));
	}

	@Override
	protected int getLightColor(float tickDelta) {
		return LightTexture.FULL_BRIGHT;
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	@Override
	public void render(VertexConsumer vertexConsumer, Camera camera, float tickDelta) {
		if (superseded()) {
			remove();
			return;
		}
		updateLocator();
		super.render(vertexConsumer, camera, tickDelta);
	}

	private boolean superseded() {
		return this.normalConverge && MoltenGolemEntity.currentConvergeGeneration(
				this.ownerEntityId) != this.generation;
	}

	private void updateLocator() {
		Vec3 locator = MoltenGolemEntity.clientLocatorPosition(
				this.ownerEntityId, this.level.getGameTime());
		if (locator != null) {
			setPos(locator.x, locator.y, locator.z);
		}
	}

	public record Factory(SpriteSet provider)
			implements ParticleProvider<SimpleParticleType> {
		@Override
		public GolemEnergyParticle createParticle(SimpleParticleType type,
				ClientLevel world, double x, double y, double z,
				double ownerEntityId, double scale, double generation) {
			return new GolemEnergyParticle(world, x, y, z, provider,
					(int) Math.round(ownerEntityId),
					scale <= 0.0D ? 1.35F : (float) scale,
					(int) Math.round(generation));
		}
	}
}
