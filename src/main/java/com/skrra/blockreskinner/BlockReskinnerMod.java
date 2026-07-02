package com.skrra.blockreskinner;

import com.skrra.blockreskinner.networking.ModNetworking;
import com.skrra.blockreskinner.registry.ModItemGroups;
import com.skrra.blockreskinner.registry.ModItems;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockReskinnerMod implements ModInitializer {
    public static final String MOD_ID = "blockreskinner";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModItems.register();
        ModItemGroups.register();
        ModNetworking.registerServer();
    }
}
