package com.mechaniciancombat.net;

public interface ModPacket {
    PacketType<? extends ModPacket> getId();
}
