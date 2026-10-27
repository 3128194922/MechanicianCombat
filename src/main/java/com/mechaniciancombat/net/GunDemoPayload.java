package com.mechaniciancombat.net;

import com.mechaniciancombat.TkGun;
import net.minecraft.network.FriendlyByteBuf;



/** 服务端 -> 客户端：打开/关闭自动演示（按原版键位跑一遍所有动作并截图）。 */
public record GunDemoPayload(boolean on) implements ModPacket {
	public static final PacketType<GunDemoPayload> ID =
			new PacketType<>(GunDemoPayload.class, TkGun.id("demo"));

	public static final PacketCodec<GunDemoPayload> CODEC = PacketCodec.of(
			(value, buf) -> buf.writeBoolean(value.on()),
			buf -> new GunDemoPayload(buf.readBoolean()));

	@Override
	public PacketType<? extends ModPacket> getId() {
		return ID;
	}
}
