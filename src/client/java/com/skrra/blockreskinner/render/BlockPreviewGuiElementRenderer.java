package com.skrra.blockreskinner.render;

import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.render.SpecialGuiElementRenderer;
import net.minecraft.client.render.BlockRenderLayers;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.world.biome.GrassColors;
import org.joml.Quaternionf;

/**
 * Renders block state models into an offscreen texture at full window
 * resolution, so GUI previews stay crisp at any preview size instead of
 * stretching the 16px item atlas. Blocks are oriented like vanilla GUI
 * block items (30° pitch, 225° yaw, 0.625 scale), which also makes axis
 * variants of pillar blocks visually distinguishable.
 */
public class BlockPreviewGuiElementRenderer extends SpecialGuiElementRenderer<BlockPreviewGuiElementRenderState> {
    public BlockPreviewGuiElementRenderer(VertexConsumerProvider.Immediate vertexConsumers) {
        super(vertexConsumers);
    }

    @Override
    public Class<BlockPreviewGuiElementRenderState> getElementClass() {
        return BlockPreviewGuiElementRenderState.class;
    }

    @Override
    protected String getName() {
        return "blockreskinner block preview";
    }

    @Override
    protected float getYOffset(int height, int windowScaleFactor) {
        return height / 2.0F;
    }

    @Override
    protected void render(BlockPreviewGuiElementRenderState state, MatrixStack matrices) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.gameRenderer.getDiffuseLighting().setShaderLights(DiffuseLighting.Type.ITEMS_3D);
        // The base class centers us and applies scale(f, f, -f); rotating 180°
        // around X converts that into the scale(f, -f, f) frame vanilla uses
        // for GUI item models (y-up, z toward the viewer).
        matrices.multiply(new Quaternionf().rotationX((float) Math.PI));
        // Vanilla "gui" display transform for block models.
        matrices.multiply(new Quaternionf().rotationXYZ(
                (float) Math.toRadians(30.0),
                (float) Math.toRadians(225.0),
                0.0F
        ));
        matrices.scale(0.625F, 0.625F, 0.625F);
        matrices.translate(-0.5F, -0.5F, -0.5F);
        renderBlockModel(client, state.blockState(), matrices);
    }

    /**
     * Standalone block model render (equivalent to renderBlockAsEntity) with a
     * tint safety net: previews have no world context, and a color provider
     * that returns black without one would render tinted models (plants,
     * grass, leaves) as black silhouettes. Vanilla providers return sane
     * plains-like defaults for a null world; the guard covers modded ones.
     */
    private void renderBlockModel(MinecraftClient client, BlockState blockState, MatrixStack matrices) {
        if (blockState.getRenderType() != BlockRenderType.MODEL) {
            return;
        }
        BlockStateModel model = client.getBlockRenderManager().getModel(blockState);
        int color = client.getBlockColors().getColor(blockState, null, null, 0);
        if ((color & 0xFFFFFF) == 0) {
            color = GrassColors.getDefaultColor();
        }
        float red = (color >> 16 & 0xFF) / 255.0F;
        float green = (color >> 8 & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        BlockModelRenderer.render(
                matrices.peek(),
                this.vertexConsumers.getBuffer(BlockRenderLayers.getEntityBlockLayer(blockState)),
                model,
                red,
                green,
                blue,
                LightmapTextureManager.MAX_LIGHT_COORDINATE,
                OverlayTexture.DEFAULT_UV
        );
    }
}
