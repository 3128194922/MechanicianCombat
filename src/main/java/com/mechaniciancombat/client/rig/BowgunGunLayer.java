package com.mechaniciancombat.client.rig;

import com.mechaniciancombat.TkGun;
import com.mechaniciancombat.client.TkGunClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * 枪那一份：同一份几何里的**枪件骨骼**，换作者的枪贴图补画一遍。
 *
 * <p>主渲染器只评估 GeckoLib 动画、不绘制几何；这里在 {@link #renderForBone} 里用枪贴图绘制枪械骨骼。
 * 用的是**主模型那根骨骼此刻被动画摆好的姿态**（同一个 poseStack），所以枪真的跟着
 * 手臂 / 后坐 / 拉栓 / 抛壳 / 翻滚一起动；{@code renderCube} 不检查 hidden，所以藏了也能画。
 *
 * <p>⚠️ 别用 {@code renderer.reRender(第二套 BakedGeoModel, ...)}：那是另一份骨骼实例，
 * 动画摆的是主模型那份，第二份永远停在静止姿势（表现就是"枪不跟手"）。2026-10-12 踩过。
 *
 * <p>⚠️ 也**别再加"按状态显隐特效"的门禁**（2026-10-12 删掉的 effectVisible）：作者是靠
 * 动画里的 **scale 关键帧**做显隐的 —— 父骨 {@code fire} 在待机/跑/滑铲里是 (1,0,0)（Y 压成 0 = 收起），
 * Focus_shooting 里父骨没有通道（GeckoLib 会把没有通道的骨骼重置回 geo 的初始 scale=1）
 * 而 fire1..6 各自在 0.04s~0.29s 逐帧翻成 (1,1,1) = 连环枪口焰；速度线同理（父骨 bone2）。
 * GeckoLib 4.8 的 AnimationProcessor 是**读 scale** 的（scaleX/Y/ZQueue → setScaleX/Y/Z），
 * 之前那句"GeckoLib 不读 scale"是错的，加门禁反而把作者做好的特效盖掉了。
 */
public class BowgunGunLayer extends GeoRenderLayer<BowgunRig> {
	private static final ResourceLocation TEX = new ResourceLocation("tkgun", "textures/entity/bowgun_gun.png");

	public BowgunGunLayer(GeoRenderer<BowgunRig> renderer) {
		super(renderer);
	}

	@Override
	public void renderForBone(PoseStack poseStack, BowgunRig animatable, GeoBone bone, RenderType type,
							  MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick,
							  int packedLight, int packedOverlay) {
		// 待机/收起时 fire 父骨的 scaleY = 0（特效平面被压平）；一旦它变成 1，说明**没有动画在跑**
		// （骨骼被重置回 geo 初始值）—— 也就是用户看到的"枪卡在原位、粒子特效全摊出来"。
		//
		// ⚠️ 2026-10-13：原来"只要数值变了就打一行"太吵（开火时 scale 会 0.78→0.72→…→0 一路过渡，
		// 一发十几行）。现在只在**跨过两个关键值**时打：≈1（整模型被重置的坏状态）和 ≈0（正常收起）。
		if ("fire".equals(bone.getName())) {
			float sy = bone.getScaleY();
			int state = Math.abs(sy - 1.0F) < 0.02F ? 1 : (Math.abs(sy) < 0.02F ? 0 : -1);
			if (state >= 0 && state != lastFireScaleState) {
				lastFireScaleState = state;
				lastFireScaleY = sy;
				TkGun.LOGGER.info("[tkgun] fire 平面 scaleY={} （0=收起正常，1=整模型被重置的最初状态）", sy);
			}
		}
		// ⚠️ 枪口的量测**不在这里**：GeoRenderLayer#renderForBone 对"没有方块的骨骼"（locator）
		//    根本不会触发（2026-10-14 实测：一次都没跑，服务端拿到 `客户端量到枪口=false`）。
		//    现在放在 BowgunRigRenderer#renderRecursively 里（骨骼遍历本身，必定经过每根骨骼）。
		// 气流平面（`airflow1/2/3`，用第 2 张贴图）交给 BowgunAirflowLayer 单独画 ——
		// 拿枪贴图硬画它的话 UV 越界，会糊出一块乱七八糟的东西。
		if (BowgunRigRenderer.BODY_BONES.contains(bone.getName()) || bone.getCubes().isEmpty()
				|| BowgunAirflowLayer.isAirflowBone(bone.getName())) {
			return;
		}
		// **枪口焰那一组（`fire` / `fire1..4`）不要光照阴影**（用户 2026-10-16："如果可行请去掉此组中
		// 模型的阴影效果"）：改用**自发光（emissive）**那一层画 —— 不参与世界光照与法线明暗，
		// 亮度恒定，夜里/背光处也是亮的，火焰不会再被"打暗"。
		boolean flash = bone.getName().startsWith("fire")
				|| bone.getName().startsWith("change5");
		// 枪换一张贴图 → 换一个 buffer；下一个骨骼 GeckoLib 会自己把缓冲刷回来
		// （GeoRenderer.checkAndRefreshBuffer 每根骨骼都会重新取）。
		RenderType gunLayer = flash
				? RenderType.entityTranslucentEmissive(TEX, false)
				: this.renderer.getRenderType(animatable, TEX, bufferSource, partialTick);
		VertexConsumer gunBuffer = bufferSource.getBuffer(gunLayer);
		int light = flash
				? net.minecraft.client.renderer.LightTexture.FULL_BRIGHT
				: packedLight;
		for (GeoCube cube : bone.getCubes()) {
			poseStack.pushPose();
			this.renderer.renderCube(poseStack, cube, gunBuffer, light, packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);

			poseStack.popPose();
		}
	}

	private static float lastFireScaleY = -1.0F;
	private static int lastFireScaleState = -1;

}
