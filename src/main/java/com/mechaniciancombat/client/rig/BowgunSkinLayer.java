package com.mechaniciancombat.client.rig;

import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * **玩家皮肤的第二层**（头发 / 帽子 / 衣服覆盖层）。
 * 用户 2026-10-17："为玩家的模型添加第二层皮肤（头发等）的渲染"。
 *
 * <h2>为什么原版第二层会丢</h2>
 * 我们的 mod 把玩家模型整个换成 GeckoLib rig（`GeoReplacedEntity`，见 {@link BowgunRig}），
 * 身体那 6 块骨骼的 UV 是导出脚本按**官方玩家几何的第一层**写死的（`gen_geckolib_official#skin_table()`）——
 * 原版模型里那套"第二层"方块（hat / jacket / sleeve / pant）自然就不存在了。
 *
 * <h2>怎么补（和 {@link BowgunGunLayer} / {@link BowgunAirflowLayer} 同一套路：几何不动，重画一遍）</h2>
 * 对这 6 块身体骨骼，把它们的方块**再画一遍**：
 * <ul>
 *   <li><b>UV 平移到原版皮肤的第二层区域</b>：64x64 皮肤的标准版式是
 *       头 +32u、躯干/右臂/右腿 +16v、左臂 +16u、左腿 −16u（下面的 {@link #LAYER2} 表）；</li>
 *   <li><b>沿面法线外扩一点点</b>（原版：帽子 0.5 像素、其余 0.25 像素）——
 *       不外扩就和第一层完全重合，会 z-fighting 花掉。</li>
 * </ul>
 *
 * <p>贴图和第一层共用（就是玩家皮肤本身），所以直接往当前那一层（参数里的 `type` / `buffer`）里画，
 * 不需要新建 RenderLayer。想关掉：启动参数 {@code -Dtkgun.skinlayer=false}。
 *
 * <p>已知限制：作者的几何是**宽手臂**（4x12x4），slim（Alex）皮肤的袖子层会稍微错位 —— 和第一层一样。
 */
public class BowgunSkinLayer extends GeoRenderLayer<BowgunRig> {
	/** 每块身体骨骼的第二层参数：{UV 偏移 u（像素）, UV 偏移 v（像素）, 法线外扩（像素）}。 */
	private static final java.util.Map<String, float[]> LAYER2 = java.util.Map.of(
			"head", new float[]{32.0F, 0.0F, 0.5F},       // hat
			"chest", new float[]{0.0F, 16.0F, 0.25F},     // jacket
			"arm_right", new float[]{0.0F, 16.0F, 0.25F}, // right sleeve
			"leg_right", new float[]{0.0F, 16.0F, 0.25F}, // right pant
			"arm_left", new float[]{16.0F, 0.0F, 0.25F},  // left sleeve
			"leg_left", new float[]{-16.0F, 0.0F, 0.25F}); // left pant
	/** 皮肤是 64x64（UV 归一化分母）。 */
	private static final float TEX = 64.0F;
	private static final boolean ENABLED =
			!"false".equalsIgnoreCase(System.getProperty("tkgun.skinlayer", "true"));

	public BowgunSkinLayer(GeoRenderer<BowgunRig> renderer) {
		super(renderer);
	}

	/** 这根骨头有没有第二层（`head`/`chest`/两条胳膊/两条腿）。 */
	public static boolean hasSecondLayer(String name) {
		return LAYER2.containsKey(name);
	}

	@Override
	public void renderForBone(PoseStack poseStack, BowgunRig animatable, GeoBone bone, RenderType type,
							  MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick,
							  int packedLight, int packedOverlay) {
		if (!ENABLED || bone.getCubes().isEmpty()) {
			return;
		}
		// 头部已经戴了装备（默认是原版头盔）时不要再叠第二层皮肤，否则头发/帽子会从头盔里穿出来。
		if ("head".equals(bone.getName()) && hasHeadEquipment()) {
			return;
		}
		float[] spec = LAYER2.get(bone.getName());
		if (spec == null) {
			return;
		}
		float du = spec[0] / TEX;
		float dv = spec[1] / TEX;
		float inflate = spec[2] / 16.0F;          // 像素 → 格
		for (GeoCube cube : bone.getCubes()) {
			// 整体沿 XYZ 三轴扩张并平移 UV。共用的棱边顶点会得到相同坐标，
			// 避免逐面沿法线移动造成相邻面分离、露出间隙。
			this.renderer.renderCube(poseStack, shiftAndInflate(cube, du, dv, inflate), buffer,
					packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
		}
	}

	private boolean hasHeadEquipment() {
		if (!(this.renderer instanceof BowgunRigRenderer rig)) {
			return false;
		}
		return rig.getCurrentEntity() != null
				&& !rig.getCurrentEntity().getItemBySlot(EquipmentSlot.HEAD).isEmpty();
	}

	/** 复制一份方块：UV 整体平移 (du,dv)，包围盒每个轴向外扩 {@code inflate}（格）。 */
	private static GeoCube shiftAndInflate(GeoCube cube, float du, float dv, float inflate) {
		GeoQuad[] quads = cube.quads();
		float minX = Float.POSITIVE_INFINITY;
		float minY = Float.POSITIVE_INFINITY;
		float minZ = Float.POSITIVE_INFINITY;
		float maxX = Float.NEGATIVE_INFINITY;
		float maxY = Float.NEGATIVE_INFINITY;
		float maxZ = Float.NEGATIVE_INFINITY;
		for (GeoQuad quad : quads) {
			if (quad == null) {
				continue;
			}
			for (GeoVertex vertex : quad.vertices()) {
				Vector3f p = vertex.position();
				minX = Math.min(minX, p.x());
				minY = Math.min(minY, p.y());
				minZ = Math.min(minZ, p.z());
				maxX = Math.max(maxX, p.x());
				maxY = Math.max(maxY, p.y());
				maxZ = Math.max(maxZ, p.z());
			}
		}
		float centerX = (minX + maxX) * 0.5F;
		float centerY = (minY + maxY) * 0.5F;
		float centerZ = (minZ + maxZ) * 0.5F;
		GeoQuad[] out = new GeoQuad[quads.length];
		for (int i = 0; i < quads.length; i++) {
			GeoQuad quad = quads[i];
			if (quad == null) {
				continue;   // ⚠️ 没写 UV 的面就是 null（airflow 层踩过：不判空直接 NPE 崩渲染线程）
			}
			GeoVertex[] vs = quad.vertices();
			GeoVertex[] nv = new GeoVertex[vs.length];
			for (int j = 0; j < vs.length; j++) {
				GeoVertex v = vs[j];
				Vector3f p = new Vector3f(v.position());
				p.x = p.x < centerX ? p.x - inflate : p.x + inflate;
				p.y = p.y < centerY ? p.y - inflate : p.y + inflate;
				p.z = p.z < centerZ ? p.z - inflate : p.z + inflate;
				nv[j] = new GeoVertex(p, v.texU() + du, v.texV() + dv);
			}
			out[i] = new GeoQuad(nv, quad.normal(), quad.direction());
		}
		return new GeoCube(out, cube.pivot(), cube.rotation(), cube.size(), cube.inflate(), cube.mirror());
	}
}
