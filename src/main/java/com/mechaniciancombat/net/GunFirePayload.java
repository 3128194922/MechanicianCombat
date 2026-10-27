package com.mechaniciancombat.net;

import com.mechaniciancombat.TkGun;
import net.minecraft.network.FriendlyByteBuf;



/**
 * 客户端 -> 服务端：本地玩家用左键"攻击"开枪。
 *
 * <p>2026-10-14 起带上**枪口的世界坐标和枪管朝向** —— 由客户端在渲染时从 rig 的 `locator` 骨骼
 * 直接量出来（`BowgunGunLayer` → `TkGunClient.captureMuzzle`）。这样子弹/特效就真的从
 * **看得见的那个枪口**出来、沿着**画面上那根枪管**飞，不用再靠手算偏移猜（用户报过"不在枪口位置"）。
 * 服务端只做合理性校验（离玩家太远就用自己算的兜底）。
 */
public record GunFirePayload(double x, double y, double z, double dx, double dy, double dz)
		implements ModPacket {
	public static final PacketType<GunFirePayload> ID =
			new PacketType<>(GunFirePayload.class, TkGun.id("fire"));

	public static final PacketCodec<GunFirePayload> CODEC = PacketCodec.of(
			(value, buf) -> {
				buf.writeDouble(value.x());
				buf.writeDouble(value.y());
				buf.writeDouble(value.z());
				buf.writeDouble(value.dx());
				buf.writeDouble(value.dy());
				buf.writeDouble(value.dz());
			},
			buf -> new GunFirePayload(buf.readDouble(), buf.readDouble(), buf.readDouble(),
					buf.readDouble(), buf.readDouble(), buf.readDouble()));

	@Override
	public PacketType<? extends ModPacket> getId() {
		return ID;
	}
}
