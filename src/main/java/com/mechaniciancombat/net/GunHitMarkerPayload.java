package com.mechaniciancombat.net;

import com.mechaniciancombat.TkGun;
import net.minecraft.network.FriendlyByteBuf;



/** Server-to-shooter pulse that appears together with the impact confirmation sound. */
public record GunHitMarkerPayload() implements ModPacket {
	public static final PacketType<GunHitMarkerPayload> ID =
			new PacketType<>(GunHitMarkerPayload.class, TkGun.id("hit_marker"));

	public static final PacketCodec<GunHitMarkerPayload> CODEC =
			PacketCodec.of((value, buf) -> {
			}, buf -> new GunHitMarkerPayload());

	@Override
	public PacketType<? extends ModPacket> getId() {
		return ID;
	}
}
