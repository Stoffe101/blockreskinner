package com.skrra.blockreskinner.render.head;

import com.skrra.blockreskinner.render.BlockRenderOverrideHooks;
import com.skrra.blockreskinner.skin.ClientSkinCache;
import com.skrra.blockreskinner.skin.PlayerHeadSkinData;
import com.skrra.blockreskinner.skin.SimpleSkinData;
import com.skrra.blockreskinner.skin.SkinData;
import com.skrra.blockreskinner.skin.SkinQueries;
import net.minecraft.block.Blocks;
import net.minecraft.block.AbstractSkullBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.SkullBlock;
import net.minecraft.block.WallSkullBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.SkullBlockEntityModel;
import net.minecraft.client.render.block.entity.SkullBlockEntityRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationPropertyHelper;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-only renderer for head/skull visual skins. Head block states have no
 * baked chunk model (their chunk render type is invisible; vanilla draws them
 * through SkullBlockEntityRenderer from a real block entity). Visual skins
 * must never create block entities in the world, so this renderer draws the
 * skull model directly for every skinned position, using the same static
 * vanilla render helper the block entity renderer uses. It is hooked at the
 * end of WorldRenderer's block entity pass, so matrices, lighting, and the
 * render command queue are the exact ones vanilla block entities use — which
 * also keeps it independent of Sodium's chunk meshing.
 */
public final class VisualHeadRenderer {
    private static final double MAX_RENDER_DISTANCE_SQ = 64.0 * 64.0;
    private static final Map<SkullBlock.SkullType, SkullBlockEntityModel> MODELS = new HashMap<>();
    private static LoadedEntityModels modelSource;

    private VisualHeadRenderer() {
    }

    public static void render(MatrixStack matrices, Vec3d cameraPos, OrderedRenderCommandQueue queue) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        if (world == null) {
            return;
        }
        for (SkinData data : ClientSkinCache.all()) {
            Direction facing;
            float yaw;
            SkullBlockEntityModel model;
            RenderLayer layer;
            BlockState visual;

            if (data instanceof SimpleSkinData simple && simple.visualState().getBlock() instanceof AbstractSkullBlock skull) {
                visual = simple.visualState();
                model = getModel(client, skull.getSkullType());
                facing = wallFacing(visual);
                yaw = yawDegrees(visual, facing);
                layer = SkullBlockEntityRenderer.getCutoutRenderLayer(skull.getSkullType(), null);
            } else if (data instanceof PlayerHeadSkinData playerHead) {
                visual = Blocks.PLAYER_HEAD.getDefaultState();
                model = getModel(client, SkullBlock.Type.PLAYER);
                facing = null;
                yaw = RotationPropertyHelper.toDegrees(playerHead.rotation());
                // Vanilla resolves the profile + skin texture asynchronously;
                // renders with the default player skin until resolved.
                layer = PlayerHeadProfiles.renderLayer(playerHead.playerName());
            } else {
                continue;
            }

            BlockPos pos = data.pos();
            if (model == null
                    || pos.getSquaredDistanceFromCenter(cameraPos.getX(), cameraPos.getY(), cameraPos.getZ()) > MAX_RENDER_DISTANCE_SQ) {
                continue;
            }
            // Mirror the guards VisualStateResolver applies for chunk skins.
            BlockState real = world.getBlockState(pos);
            if (!SkinQueries.isSupportedTarget(real) || SkinQueries.isConnectedBlock(real)) {
                continue;
            }

            int light = BlockRenderOverrideHooks.skinnedLightmapCoordinates(world, visual, pos);
            matrices.push();
            matrices.translate(pos.getX() - cameraPos.getX(), pos.getY() - cameraPos.getY(), pos.getZ() - cameraPos.getZ());
            SkullBlockEntityRenderer.render(facing, yaw, 0.0F, matrices, queue, light, model, layer, 0, null);
            matrices.pop();
        }
    }

    public static Direction wallFacing(BlockState visual) {
        return visual.getBlock() instanceof WallSkullBlock && visual.contains(WallSkullBlock.FACING)
                ? visual.get(WallSkullBlock.FACING)
                : null;
    }

    public static float yawDegrees(BlockState visual, Direction wallFacing) {
        if (wallFacing != null) {
            return RotationPropertyHelper.toDegrees(RotationPropertyHelper.fromDirection(wallFacing.getOpposite()));
        }
        int rotation = visual.contains(SkullBlock.ROTATION) ? visual.get(SkullBlock.ROTATION) : 0;
        return RotationPropertyHelper.toDegrees(rotation);
    }

    /** Skull models are rebuilt lazily and dropped when entity models reload. */
    public static SkullBlockEntityModel getModel(MinecraftClient client, SkullBlock.SkullType type) {
        LoadedEntityModels models = client.getLoadedEntityModels();
        if (models != modelSource) {
            MODELS.clear();
            modelSource = models;
        }
        return MODELS.computeIfAbsent(type, t -> SkullBlockEntityRenderer.getModels(models, t));
    }
}
