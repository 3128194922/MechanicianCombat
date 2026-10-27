package com.mechaniciancombat.mixin;

import java.util.List;
import java.util.Set;
import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Skip only integrations whose target mod is absent; core mixins stay required. */
public final class OptionalMixinPlugin implements IMixinConfigPlugin {
    public void onLoad(String mixinPackage) { }
    public String getRefMapperConfig() { return null; }
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith("CombatRollInputGuardMixin")) return installed("combatroll");
        if (mixinClassName.endsWith("ShoulderSurfingCrosshairMixin")) return installed("shouldersurfing");
        return true;
    }
    private static boolean installed(String id) {
        return FMLLoader.getLoadingModList().getMods().stream().anyMatch(mod -> id.equals(mod.getModId()));
    }
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) { }
    public List<String> getMixins() { return null; }
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo info) { }
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo info) { }
}
