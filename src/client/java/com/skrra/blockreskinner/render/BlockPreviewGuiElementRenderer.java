package com.skrra.blockreskinner.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.render.SpecialGuiElementRenderer;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
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
        client.getBlockRenderManager().renderBlockAsEntity(
                state.blockState(),
                matrices,
                this.vertexConsumers,
                LightmapTextureManager.MAX_LIGHT_COORDINATE,
                OverlayTexture.DEFAULT_UV
        );
    }
}
