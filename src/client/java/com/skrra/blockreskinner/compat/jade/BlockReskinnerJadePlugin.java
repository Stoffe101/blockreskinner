package com.skrra.blockreskinner.compat.jade;

import com.skrra.blockreskinner.BlockReskinnerMod;
import net.minecraft.block.Block;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin(BlockReskinnerMod.MOD_ID)
public class BlockReskinnerJadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.addConfig(ReskinJadeProvider.UID, true);
        registration.markAsClientFeature(ReskinJadeProvider.UID);
        registration.registerBlockComponent(ReskinJadeProvider.INSTANCE, Block.class);
    }
}
