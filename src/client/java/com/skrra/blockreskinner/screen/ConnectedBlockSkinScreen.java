package com.skrra.blockreskinner.screen;

import com.skrra.blockreskinner.networking.payload.ApplyConnectedSkinPayload;
import com.skrra.blockreskinner.screen.widget.ConnectionEditorWidget;
import com.skrra.blockreskinner.skin.ConnectionOverride;
import com.skrra.blockreskinner.skin.SkinQueries;
import com.skrra.blockreskinner.util.BlockStateUtil;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class ConnectedBlockSkinScreen extends BlockSkinScreen {
    private ConnectionOverride north = ConnectionOverride.AUTO;
    private ConnectionOverride east = ConnectionOverride.AUTO;
    private ConnectionOverride south = ConnectionOverride.AUTO;
    private ConnectionOverride west = ConnectionOverride.AUTO;

    public ConnectedBlockSkinScreen(BlockPos pos) {
        super(pos, connectedStates(pos), Text.translatable("screen.blockreskinner.connected.title"));
    }

    private static List<BlockState> connectedStates(BlockPos pos) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return List.of();
        }
        return SkinQueries.connectedVisualStates(client.world.getBlockState(pos));
    }

    @Override
    protected void init() {
        super.init();
        int left = gridLeft();
        int y = height - 84;
        addDrawableChild(directionButton(Text.translatable("screen.blockreskinner.north"), left, y, () -> north, value -> north = value));
        addDrawableChild(directionButton(Text.translatable("screen.blockreskinner.east"), left + 88, y, () -> east, value -> east = value));
        addDrawableChild(directionButton(Text.translatable("screen.blockreskinner.south"), left + 176, y, () -> south, value -> south = value));
        addDrawableChild(directionButton(Text.translatable("screen.blockreskinner.west"), left + 264, y, () -> west, value -> west = value));
    }

    private ButtonWidget directionButton(Text label, int x, int y, java.util.function.Supplier<ConnectionOverride> getter, java.util.function.Consumer<ConnectionOverride> setter) {
        return ButtonWidget.builder(buttonText(label, getter.get()), button -> {
                    ConnectionOverride next = ConnectionEditorWidget.next(getter.get());
                    setter.accept(next);
                    button.setMessage(buttonText(label, next));
                })
                .dimensions(x, y, 82, 20)
                .build();
    }

    private Text buttonText(Text label, ConnectionOverride value) {
        return Text.literal(label.getString() + ": " + value.name());
    }

    @Override
    protected void apply() {
        if (selected != null) {
            ClientPlayNetworking.send(new ApplyConnectedSkinPayload(pos, BlockStateUtil.toString(selected), north, east, south, west));
            close();
        }
    }

    @Override
    protected void renderSelected(DrawContext context) {
        super.renderSelected(context);
        context.drawTextWithShadow(textRenderer, Text.translatable("screen.blockreskinner.connection_controls"), gridLeft(), height - 96, 0xBBBBBB);
    }

    @Override
    protected int gridBottom() {
        return height - 108;
    }
}
