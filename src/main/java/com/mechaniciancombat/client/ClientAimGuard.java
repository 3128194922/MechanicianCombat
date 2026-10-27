package com.mechaniciancombat.client;

import net.minecraft.client.Minecraft;

/** Client-only bridge used by common item code without resolving client classes on a dedicated server. */
public final class ClientAimGuard {
    public static boolean isCurrentPlayerAimLocked() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null && !GunCamera.aimAllowed(minecraft);
    }

    private ClientAimGuard() { }
}
