package com.mechaniciancombat.net;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Forge channel; all mutation handlers are delivered on the game thread. */
public final class ModNetwork {
    private static final String VERSION = "forge-r396-1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("tkgun", "main"), () -> VERSION, VERSION::equals, VERSION::equals);
    private static final Map<PacketType<?>, BiConsumer<?, ServerContext>> SERVER = new HashMap<>();
    private static final Map<PacketType<?>, Consumer<?>> CLIENT = new HashMap<>();

    public record ServerContext(ServerPlayer player) {
        public MinecraftServer server() { return player.getServer(); }
    }

    public static <T extends ModPacket> void registerGlobalReceiver(PacketType<T> type, BiConsumer<T, ServerContext> handler) {
        if (SERVER.put(type, handler) != null) throw new IllegalStateException("Duplicate server receiver " + type.location());
    }

    public static <T extends ModPacket> void registerClientReceiver(PacketType<T> type, Consumer<T> handler) {
        if (CLIENT.put(type, handler) != null) throw new IllegalStateException("Duplicate client receiver " + type.location());
    }

    public static void register() {
        register(0, GunFirePayload.ID, GunFirePayload.CODEC, NetworkDirection.PLAY_TO_SERVER);
        register(1, GunFocusPayload.ID, GunFocusPayload.CODEC, NetworkDirection.PLAY_TO_SERVER);
        register(2, GunReloadPayload.ID, GunReloadPayload.CODEC, NetworkDirection.PLAY_TO_SERVER);
        register(3, GunRollPayload.ID, GunRollPayload.CODEC, NetworkDirection.PLAY_TO_SERVER);
        register(4, OpenAmmoStoragePayload.ID, OpenAmmoStoragePayload.CODEC, NetworkDirection.PLAY_TO_SERVER);
        register(5, SwitchAmmoPayload.ID, SwitchAmmoPayload.CODEC, NetworkDirection.PLAY_TO_SERVER);
        register(6, CombatBackpackFirePayload.ID, CombatBackpackFirePayload.CODEC, NetworkDirection.PLAY_TO_SERVER);
        register(7, GunStatePayload.ID, GunStatePayload.CODEC, NetworkDirection.PLAY_TO_CLIENT);
        register(8, GunHitMarkerPayload.ID, GunHitMarkerPayload.CODEC, NetworkDirection.PLAY_TO_CLIENT);
        register(9, GunDemoPayload.ID, GunDemoPayload.CODEC, NetworkDirection.PLAY_TO_CLIENT);
        register(10, CombatBackpackFireResultPayload.ID, CombatBackpackFireResultPayload.CODEC, NetworkDirection.PLAY_TO_CLIENT);
        register(11, CombatBackpackStatusPayload.ID, CombatBackpackStatusPayload.CODEC, NetworkDirection.PLAY_TO_CLIENT);
        // Animation keyframe sound names are reported from the client to server for validation.
        register(12, GunSoundPayload.ID, GunSoundPayload.CODEC, NetworkDirection.PLAY_TO_SERVER);
    }

    @SuppressWarnings("unchecked")
    private static <T extends ModPacket> void register(int id, PacketType<T> type, PacketCodec<T> codec, NetworkDirection direction) {
        CHANNEL.registerMessage(id, type.packetClass(), codec.encoder(), codec.decoder(), (packet, supplier) -> {
            var context = supplier.get();
            context.enqueueWork(() -> {
                if (context.getDirection() == NetworkDirection.PLAY_TO_SERVER) {
                    var player = context.getSender();
                    var handler = (BiConsumer<T, ServerContext>) SERVER.get(type);
                    if (player != null && handler != null) handler.accept(packet, new ServerContext(player));
                } else {
                    var handler = (Consumer<T>) CLIENT.get(type);
                    if (handler != null) handler.accept(packet);
                }
            });
            context.setPacketHandled(true);
        }, java.util.Optional.ofNullable(direction));
    }

    public static void send(ServerPlayer player, ModPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToServer(ModPacket packet) { CHANNEL.sendToServer(packet); }
    private ModNetwork() { }
}
