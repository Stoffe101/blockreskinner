package com.skrra.blockreskinner.skin;

import net.minecraft.util.math.BlockPos;

/**
 * Visual-only player head skin. Stores just the player name plus the floor
 * rotation (0-15, cardinal values used by the GUI); each client resolves the
 * profile and skin texture through the vanilla player skin cache, so nothing
 * heavier than the name ever travels over the network or into the save.
 */
public record PlayerHeadSkinData(BlockPos pos, String playerName, int rotation) implements SkinData {
    @Override
    public SkinType type() {
        return SkinType.PLAYER_HEAD;
    }
}
