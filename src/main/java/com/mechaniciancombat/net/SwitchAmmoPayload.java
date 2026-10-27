package com.mechaniciancombat.net;

import com.mechaniciancombat.TkGun;
import net.minecraft.network.FriendlyByteBuf;



/** 客户端 -> 服务端：滚轮切换当前枪械弹仓中的选中槽。 */
public record SwitchAmmoPayload(int direction) implements ModPacket {
	public static final PacketType<SwitchAmmoPayload> ID =
			new PacketType<>(SwitchAmmoPayload.class, TkGun.id("switch_ammo"));

	public static final PacketCodec<SwitchAmmoPayload> CODEC = PacketCodec.of(
			(value, buf) -> buf.writeVarInt(value.direction()),
			buf -> new SwitchAmmoPayload(buf.readVarInt()));

	@Override
	public PacketType<? extends ModPacket> getId() {
		return ID;
	}
}
