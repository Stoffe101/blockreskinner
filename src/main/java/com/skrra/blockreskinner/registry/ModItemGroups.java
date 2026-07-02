package com.skrra.blockreskinner.registry;

import com.skrra.blockreskinner.BlockReskinnerMod;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;

public final class ModItemGroups {
    public static final RegistryKey<ItemGroup> BLOCK_RESKINNER = RegistryKey.of(RegistryKeys.ITEM_GROUP, BlockReskinnerMod.id("block_reskinner"));

    private ModItemGroups() {
    }

    public static void register() {
        Registry.register(Registries.ITEM_GROUP, BLOCK_RESKINNER, FabricItemGroup.builder()
                .displayName(Text.translatable("itemGroup.blockreskinner"))
                .icon(() -> new ItemStack(ModItems.RESKIN_WAND))
                .entries((context, entries) -> entries.add(ModItems.RESKIN_WAND))
                .build());
    }
}
