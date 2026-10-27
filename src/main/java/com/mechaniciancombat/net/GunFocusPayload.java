package com.mechaniciancombat.net;

import com.mechaniciancombat.TkGun;
import net.minecraft.network.FriendlyByteBuf;



/**
 * 客户端在 Z 键按下沿发送专注切换请求。dodgeCombo 表示本次同时配合翻滚/滑铲，
 * 服务端会在对应动作开始后兑现快速专注。
 */
public record GunFocusPayload(boolean dodgeCombo) implements ModPacket {
	public static final PacketType<GunFocusPayload> ID =
			new PacketType<>(GunFocusPayload.class, TkGun.id("focus_toggle"));

	public static final PacketCodec<GunFocusPayload> CODEC = PacketCodec.of(
			(value, buf) -> buf.writeBoolean(value.dodgeCombo()),
			// 宽读：旧服务端/旧客户端没有这一位时按普通 Z 处理。
			buf -> new GunFocusPayload(buf.readableBytes() > 0 && buf.readBoolean()));

	public GunFocusPayload() {
		this(false);
	}

	@Override
	public PacketType<? extends ModPacket> getId() {
		return ID;
	}
}
