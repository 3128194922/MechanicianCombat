package com.mechaniciancombat.client;

import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.DustParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import org.joml.Vector3f;

/** Rose quartz guidance trail with the same full-bright handling as impact effects. */
public class GlowingRoseTrailParticle extends DustParticle {
	private static final DustParticleOptions EFFECT =
			new DustParticleOptions(new Vector3f(0.96F, 0.25F, 0.55F), 0.55F);

	private GlowingRoseTrailParticle(ClientLevel world, double x, double y, double z,
									 double vx, double vy, double vz, SpriteSet provider) {
		super(world, x, y, z, vx, vy, vz, EFFECT, provider);
	}

	@Override
	protected int getLightColor(float tickDelta) {
		return LightTexture.FULL_BRIGHT;
	}

	public record Factory(SpriteSet provider) implements ParticleProvider<SimpleParticleType> {
		@Override
		public GlowingRoseTrailParticle createParticle(SimpleParticleType type, ClientLevel world,
													   double x, double y, double z,
													   double vx, double vy, double vz) {
			return new GlowingRoseTrailParticle(world, x, y, z, vx, vy, vz, provider);
		}
	}
}
