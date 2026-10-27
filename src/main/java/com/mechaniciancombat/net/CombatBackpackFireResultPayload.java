package com.mechaniciancombat.net;

import com.mechaniciancombat.TkGun;
import net.minecraft.network.FriendlyByteBuf;



/** Server acknowledgement used to release the client fire-request latch. */
public record CombatBackpackFireResultPayload() implements ModPacket {
	public static final PacketType<CombatBackpackFireResultPayload> ID =
			new PacketType<>(CombatBackpackFireResultPayload.class, TkGun.id("backpack_firework_result"));

	public static final PacketCodec<CombatBackpackFireResultPayload> CODEC =
			PacketCodec.of(
					(value, buf) -> {
					},
					buf -> new CombatBackpackFireResultPayload());

	@Override
	public PacketType<? extends ModPacket> getId() {
		return ID;
	}
}
