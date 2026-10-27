package com.mechaniciancombat.gametest;

import com.mechaniciancombat.client.FallbackShoulderCamera;
import com.mechaniciancombat.TkGun;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Opt-in test-mod recorder for real UI input; never packaged in the main mod. */
@Mod.EventBusSubscriber(modid = "tkguntests", value = Dist.CLIENT)
public final class R396ClientInputProbe {
    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        String output = System.getProperty("tkgun.r396.inputlog");
        var mc = Minecraft.getInstance();
        if (output == null || event.phase != TickEvent.Phase.END || mc.player == null) return;
        var p = mc.player;
        String line = String.format(Locale.ROOT,
                "%d,%s,%s,%s,%s,%s,%s,%.4f,%.4f,%.5f,%.5f,%.5f,%.3f,%.3f,%.3f,%s,%s,%s%n",
                p.tickCount, mc.options.keyUp.isDown(), mc.options.keyDown.isDown(),
                mc.options.keyLeft.isDown(), mc.options.keyRight.isDown(), mc.options.keyUse.isDown(),
                mc.options.keySprint.isDown(), p.input.forwardImpulse, p.input.leftImpulse,
                p.getX(), p.getY(), p.getZ(), FallbackShoulderCamera.cameraYaw(),
                FallbackShoulderCamera.cameraPitch(), p.getYRot(),
                FallbackShoulderCamera.isFallbackActive(mc), TkGun.holdingGun(p), p.isUsingItem());
        try {
            Files.writeString(Path.of(output), line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
    }
    private R396ClientInputProbe() { }
}
