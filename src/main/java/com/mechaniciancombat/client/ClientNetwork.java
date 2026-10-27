package com.mechaniciancombat.client;

import com.mechaniciancombat.net.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import java.util.function.BiConsumer;

public final class ClientNetwork {
    public record Context(Minecraft client) {
        public LocalPlayer player() { return client.player; }
    }
    public static <T extends ModPacket> void registerGlobalReceiver(PacketType<T> type, BiConsumer<T, Context> handler) {
        ModNetwork.registerClientReceiver(type, packet -> handler.accept(packet, new Context(Minecraft.getInstance())));
    }
    public static void send(ModPacket packet) { ModNetwork.sendToServer(packet); }
    private ClientNetwork() { }
}
