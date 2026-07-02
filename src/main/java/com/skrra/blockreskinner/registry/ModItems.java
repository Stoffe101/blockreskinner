package com.skrra.blockreskinner.registry;

import com.skrra.blockreskinner.BlockReskinnerMod;
import com.skrra.blockreskinner.item.ReskinWandItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

public final class ModItems {
    public static final RegistryKey<Item> RESKIN_WAND_KEY = RegistryKey.of(RegistryKeys.ITEM, BlockReskinnerMod.id("reskin_wand"));
    public static final Item RESKIN_WAND = new ReskinWandItem(new Item.Settings().registryKey(RESKIN_WAND_KEY).maxCount(1));

    private ModItems() {
    }

    public static void register() {
        Registry.register(Registries.ITEM, RESKIN_WAND_KEY, RESKIN_WAND);
    }
}
