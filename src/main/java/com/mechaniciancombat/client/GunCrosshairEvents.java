package com.mechaniciancombat.client;

import com.mechaniciancombat.TkGun;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Forge equivalent of r396's vanilla crosshair suppression. */
@Mod.EventBusSubscriber(modid = TkGun.MOD_ID, value = Dist.CLIENT)
public final class GunCrosshairEvents {
    @SubscribeEvent
    public static void beforeOverlay(RenderGuiOverlayEvent.Pre event) {
        var player = Minecraft.getInstance().player;
        if (player != null && TkGun.holdingGun(player)
                && event.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id())) {
            event.setCanceled(true);
        }
    }
    private GunCrosshairEvents() { }
}
