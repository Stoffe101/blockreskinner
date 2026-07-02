package snownee.jade.api;

import net.minecraft.block.Block;
import net.minecraft.util.Identifier;

public interface IWailaClientRegistration {
    void addConfig(Identifier id, boolean enabled);

    void registerBlockComponent(IComponentProvider<BlockAccessor> provider, Class<? extends Block> blockClass);

    void markAsClientFeature(Identifier id);
}
