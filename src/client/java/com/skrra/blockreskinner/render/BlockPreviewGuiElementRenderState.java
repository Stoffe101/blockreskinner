package com.skrra.blockreskinner.render;

import net.minecraft.block.BlockState;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.special.SpecialGuiElementRenderState;
import org.jspecify.annotations.Nullable;

/**
 * Render state for a 3D block model preview drawn as a special GUI element.
 * The scale is the edge length in GUI pixels a full block should occupy,
 * matching how vanilla sizes item models (16 = slot-sized).
 */
public record BlockPreviewGuiElementRenderState(
        BlockState blockState,
        int x1,
        int y1,
        int x2,
        int y2,
        float scale,
        @Nullable ScreenRect scissorArea,
        @Nullable ScreenRect bounds
) implements SpecialGuiElementRenderState {
    public BlockPreviewGuiElementRenderState(BlockState blockState, int x1, int y1, int x2, int y2, float scale, @Nullable ScreenRect scissorArea) {
        this(blockState, x1, y1, x2, y2, scale, scissorArea, SpecialGuiElementRenderState.createBounds(x1, y1, x2, y2, scissorArea));
    }
}
