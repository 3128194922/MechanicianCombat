package com.mechaniciancombat.net;

import com.mechaniciancombat.TkGun;
import net.minecraft.network.FriendlyByteBuf;



/** 客户端 -> 服务端：打开机械师弹药环 UI。 */
public record OpenAmmoStoragePayload() implements ModPacket {
	public static final PacketType<OpenAmmoStoragePayload> ID =
			new PacketType<>(OpenAmmoStoragePayload.class, TkGun.id("open_ammo_storage"));

	public static final PacketCodec<OpenAmmoStoragePayload> CODEC = PacketCodec.of(
			(value, buf) -> { },
			buf -> new OpenAmmoStoragePayload());

	@Override
	public PacketType<? extends ModPacket> getId() {
		return ID;
	}
}
