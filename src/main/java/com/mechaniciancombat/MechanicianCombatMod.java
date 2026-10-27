package com.mechaniciancombat;

import com.mechaniciancombat.registry.ModRegistries;
import com.mechaniciancombat.combat.WindChargeFallProtection;
import com.mechaniciancombat.event.BackpackFireworkSelfDamageHandler;
import com.mechaniciancombat.net.ModNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Forge entry point for the tkgun content and lifecycle. */
@Mod(TkGun.MOD_ID)
public final class MechanicianCombatMod {
    public MechanicianCombatMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModRegistries.register(modBus);
        ModNetwork.register();
        MinecraftForge.EVENT_BUS.addListener(BackpackFireworkSelfDamageHandler::onLivingHurt);
        MinecraftForge.EVENT_BUS.addListener(WindChargeFallProtection::onLivingTick);
        MinecraftForge.EVENT_BUS.addListener(WindChargeFallProtection::onLivingFall);
        modBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(TkGunServer::initialize);
    }
}
