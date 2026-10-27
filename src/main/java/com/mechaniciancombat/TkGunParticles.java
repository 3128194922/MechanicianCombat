package com.mechaniciancombat;

import net.minecraft.core.particles.SimpleParticleType;
import com.mechaniciancombat.registry.ModRegistries;

/**
 * 711 素材里那个「散热粒子.json」是 Bedrock 草稿（identifier/texture 都是空的），
 * 这里把它补完并落到本服能用的形式上：一个真正的 MC 粒子 tkgun:heat_vent，
 * 8 帧贴图在 resources/assets/tkgun/textures/particle/heat_vent_0..7.png。
 *
 * 2026-10-13 机械师素材又给了 `粒子.zip`：`converge.png`(32x192) / `xplo.png`(48x288)
 * 两张竖条翻页贴图 —— 切成单帧后登记成
 * {@link #ENERGY_CONVERGE}（枪口能量汇聚）和 {@link #ENERGY_XPLO}（命中能量爆），
 * 生成脚本 `_mech_work\make_particles.py`。
 */
public final class TkGunParticles {
	/** SimpleParticleType 既是注册表里的类型，也是 spawn 时要传的 effect。 */
	public static final SimpleParticleType HEAT_VENT = ModRegistries.HEAT_VENT.get();
	/** 枪口能量汇聚（素材 粒子.zip 的 converge，6 帧）。 */
	public static final SimpleParticleType ENERGY_CONVERGE = ModRegistries.ENERGY_CONVERGE.get();
	/** 命中能量爆（素材 粒子.zip 的 xplo，6 帧）。 */
	public static final SimpleParticleType ENERGY_XPLO = ModRegistries.ENERGY_XPLO.get();
	/**
	 * **二级加速弹壳**（素材 v4 `粒子\二级加速弹壳.png`，6 帧 48x48）：
	 * 专注连射射速提上来之后，每出一发甩出去的弹壳（见 `TkGunServer#fireOnce`）。
	 */
	public static final SimpleParticleType SHELL_BOOST = ModRegistries.SHELL_BOOST.get();
	/** 枪口自发光辉光（与 fire1..4 枪口焰同为 5 tick，由亮到暗）。 */
	public static final SimpleParticleType MUZZLE_GLOW = ModRegistries.MUZZLE_GLOW.get();
	/** Cutting-round impact effect (6 frames from 粒子/切割弹/cutting.png). */
	public static final SimpleParticleType CUTTING_EXPLO = ModRegistries.CUTTING_EXPLO.get();
	/** Sculk-round impact effect (8 frames from 粒子/幽匿弹/sonic_boom.ase). */
	public static final SimpleParticleType SONIC_BOOM = ModRegistries.SONIC_BOOM.get();
	/** Grapeshot pellet impact effect, six frames from grapeshot_explo.png. */
	public static final SimpleParticleType GRAPESHOT_IMPACT = ModRegistries.GRAPESHOT_IMPACT.get();
	/** Rose quartz impact effect, four frames from rose_quartz_explo.png. */
	public static final SimpleParticleType ROSE_QUARTZ_IMPACT = ModRegistries.ROSE_QUARTZ_IMPACT.get();
	/** Full-bright rose quartz guidance trail. */
	public static final SimpleParticleType ROSE_QUARTZ_TRAIL = ModRegistries.ROSE_QUARTZ_TRAIL.get();

	public static final SimpleParticleType GOLEM_ENERGY_CONVERGE = ModRegistries.GOLEM_ENERGY_CONVERGE.get();

	public static final SimpleParticleType GOLEM_ENERGY_CONVERGE_2 = ModRegistries.GOLEM_ENERGY_CONVERGE_2.get();

	public static final SimpleParticleType EXPLOSIVE_BOLT_IMPACT = ModRegistries.EXPLOSIVE_BOLT_IMPACT.get();

	private TkGunParticles() {
	}
}
