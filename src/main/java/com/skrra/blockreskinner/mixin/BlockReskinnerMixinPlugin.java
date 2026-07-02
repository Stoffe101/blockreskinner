package com.skrra.blockreskinner.mixin;

import com.skrra.blockreskinner.BlockReskinnerMod;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class BlockReskinnerMixinPlugin implements IMixinConfigPlugin {
    private static final String SODIUM_LEVEL_SLICE_MIXIN = "com.skrra.blockreskinner.mixin.SodiumLevelSliceMixin";
    private boolean loggedSodium;

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (SODIUM_LEVEL_SLICE_MIXIN.equals(mixinClassName)) {
            boolean loaded = FabricLoader.getInstance().isModLoaded("sodium");
            if (!loggedSodium) {
                loggedSodium = true;
                BlockReskinnerMod.LOGGER.info("Sodium compatibility mixin enabled: {}", loaded);
            }
            return loaded;
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
