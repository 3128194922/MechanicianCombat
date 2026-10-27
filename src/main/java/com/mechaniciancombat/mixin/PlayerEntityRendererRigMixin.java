package com.mechaniciancombat.mixin;

import com.mechaniciancombat.client.rig.BowgunRigClient;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 只做两件事（都跟"手持破垒重炮"有关，其它玩家一切照旧）：
 * <ol>
 *   <li>把 {@code EntityRendererFactory.Context} 记下来 —— GeckoLib 的渲染器构造要用；</li>
 *   <li>渲染玩家时，如果手里是破垒重炮 → 交给 GeckoLib rig 渲染并取消原版那条身体渲染。</li>
 * </ol>
 *
 * <p>⚠️ **原版那条渲染必须整条取消**（hide 原版玩家模型、只留 GeckoLib 模型）—— 2026-10-14 用户：
 * "之前的原理相当于是隐藏原版玩家模型 直接展示的 geolib 模型"。
 * 这一天试过"不取消 + 让原版模型摆成 rig 姿势去画身体/盔甲"的两版，都被否了：
 * ① 拿原版 6 块平级部件近似 rig 的骨骼位移 → **肢体动作变形**（用户："绝不能委屈肢体动作"）；
 * ② 原版盔甲那层补画 → 盔甲要么被 rig 身体盖住、要么尺寸不对。
 * 现在改为 {@code BowgunArmorLayer} 在 rig 每根身体骨骼自己的 poseStack 上按原版盔甲贴图重画，
 * 和肢体动画共用同一套骨骼变换，因此不再需要原版 PlayerEntityModel。
 *
 * <p>⚠️ 本 mod 没有 refmap（跟 TkAuth 一样），所以注解里的方法名/描述符要写 **intermediary** 名：
 * {@code <init>} 的描述符 = (Lnet/minecraft/class_5617$class_5618;Z)V，
 * 渲染方法 = {@code method_4215}(Lnet/minecraft/class_742;FFLnet/minecraft/class_4587;Lnet/minecraft/class_4597;I)V，
 * 名字牌 = {@code method_4213}(Lnet/minecraft/class_742;Lnet/minecraft/class_2561;Lnet/minecraft/class_4587;Lnet/minecraft/class_4597;IF)V。
 */
@Mixin(PlayerRenderer.class)
public abstract class PlayerEntityRendererRigMixin {
	/** 名字牌方法（名字照抄 intermediary，理由同上）。 */
	@Shadow
	protected abstract void renderNameTag(AbstractClientPlayer player, Component text, PoseStack matrices,
											MultiBufferSource bufferSource, int light);

	@Inject(method = "<init>(Lnet/minecraft/client/renderer/entity/EntityRendererProvider$Context;Z)V", at = @At("TAIL"))
	private void tkgun$rememberContext(EntityRendererProvider.Context ctx, boolean slim, CallbackInfo ci) {
		BowgunRigClient.setContext(ctx);
	}

	@Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
			at = @At("HEAD"), cancellable = true)
	private void tkgun$renderRig(AbstractClientPlayer player, float yaw, float tickDelta,
								 PoseStack matrices, MultiBufferSource consumers, int light, CallbackInfo ci) {
		if (BowgunRigClient.renderIfHolding(player, yaw, tickDelta, matrices, consumers, light)) {
			// 我们取消的是**整条**原版渲染，顺手把名字牌也取消了 —— 这里补回来：
			// 不然多人游戏里，只要某人手里拿着弩炮，别人就看不到他头顶的名字了。
			// 原版对自己（摄像机实体）是不画名字牌的，这里照旧跳过。
			net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
			if (player != client.player && player != client.getCameraEntity()) {
				this.renderNameTag(player, player.getDisplayName(), matrices, consumers, light);
			}
			ci.cancel();
		}
	}
}
