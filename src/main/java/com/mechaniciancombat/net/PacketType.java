package com.mechaniciancombat.net;

import net.minecraft.resources.ResourceLocation;

public record PacketType<T extends ModPacket>(Class<T> packetClass, ResourceLocation location) { }
