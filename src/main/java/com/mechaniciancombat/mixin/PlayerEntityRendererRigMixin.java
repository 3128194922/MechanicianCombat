package com.mechaniciancombat.mixin;

import com.mechaniciancombat.client.rig.BowgunRigClient;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 保留原版 PlayerRenderer 的玩家身体、皮肤和装备层；持枪时在它渲染前调用 GeckoLib，
 * 绘制枪械并采集同帧身体姿势供 PlayerAnimator 使用。
 * <ol>
 *   <li>把 {@code EntityRendererFactory.Context} 记下来 —— GeckoLib 的渲染器构造要用；</li>
 *   <li>渲染玩家时，如果手里是破垒重炮 → 先让 GeckoLib 绘制枪械，再继续原版玩家与装备层渲染。</li>
 * </ol>
 */
@Mixin(PlayerRenderer.class)
public abstract class PlayerEntityRendererRigMixin {
	@Inject(method = "<init>(Lnet/minecraft/client/renderer/entity/EntityRendererProvider$Context;Z)V", at = @At("TAIL"))
	private void tkgun$rememberContext(EntityRendererProvider.Context ctx, boolean slim, CallbackInfo ci) {
		BowgunRigClient.setContext(ctx);
	}

	@Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
			at = @At("HEAD"))
	private void tkgun$renderRig(AbstractClientPlayer player, float yaw, float tickDelta,
								 PoseStack matrices, MultiBufferSource consumers, int light, CallbackInfo ci) {
		BowgunRigClient.renderIfHolding(player, yaw, tickDelta, matrices, consumers, light);
	}
}
