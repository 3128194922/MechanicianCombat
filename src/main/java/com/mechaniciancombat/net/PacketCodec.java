package com.mechaniciancombat.net;

import net.minecraft.network.FriendlyByteBuf;
import java.util.function.BiConsumer;
import java.util.function.Function;

public record PacketCodec<T>(BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder) {
    public static <T> PacketCodec<T> of(BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder) {
        return new PacketCodec<>(encoder, decoder);
    }
}
