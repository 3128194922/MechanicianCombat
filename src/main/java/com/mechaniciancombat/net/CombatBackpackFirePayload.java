package com.mechaniciancombat.net;

import com.mechaniciancombat.TkGun;
import net.minecraft.network.FriendlyByteBuf;



/** Client-side spawn point plus the current camera frustum used for target marking. */
public record CombatBackpackFirePayload(double x, double y, double z,
										double cameraX, double cameraY, double cameraZ,
										double cameraYaw, double cameraPitch,
										double halfHorizontalTan, double halfVerticalTan)
		implements ModPacket {
	public static final PacketType<CombatBackpackFirePayload> ID =
			new PacketType<>(CombatBackpackFirePayload.class, TkGun.id("backpack_firework"));

	public static final PacketCodec<CombatBackpackFirePayload> CODEC =
			PacketCodec.of(
					(value, buf) -> {
						buf.writeDouble(value.x());
						buf.writeDouble(value.y());
						buf.writeDouble(value.z());
						buf.writeDouble(value.cameraX());
						buf.writeDouble(value.cameraY());
						buf.writeDouble(value.cameraZ());
						buf.writeDouble(value.cameraYaw());
						buf.writeDouble(value.cameraPitch());
						buf.writeDouble(value.halfHorizontalTan());
						buf.writeDouble(value.halfVerticalTan());
					},
					buf -> new CombatBackpackFirePayload(
							buf.readDouble(), buf.readDouble(), buf.readDouble(),
							buf.readDouble(), buf.readDouble(), buf.readDouble(),
							buf.readDouble(), buf.readDouble(),
							buf.readDouble(), buf.readDouble()));

	@Override
	public PacketType<? extends ModPacket> getId() {
		return ID;
	}
}
