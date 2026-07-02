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
        int firstRow = height - 112;
        int secondRow = height - 88;
        int secondColumn = left + 184;
        addDrawableChild(directionButton("screen.blockreskinner.connection.north", left, firstRow, () -> north, value -> north = value));
        addDrawableChild(directionButton("screen.blockreskinner.connection.east", secondColumn, firstRow, () -> east, value -> east = value));
        addDrawableChild(directionButton("screen.blockreskinner.connection.south", left, secondRow, () -> south, value -> south = value));
        addDrawableChild(directionButton("screen.blockreskinner.connection.west", secondColumn, secondRow, () -> west, value -> west = value));
    }

    private ButtonWidget directionButton(String labelKey, int x, int y, java.util.function.Supplier<ConnectionOverride> getter, java.util.function.Consumer<ConnectionOverride> setter) {
        return ButtonWidget.builder(buttonText(labelKey, getter.get()), button -> {
                    ConnectionOverride next = ConnectionEditorWidget.next(getter.get());
                    setter.accept(next);
                    button.setMessage(buttonText(labelKey, next));
                })
                .dimensions(x, y, 176, 20)
                .build();
    }

    private Text buttonText(String labelKey, ConnectionOverride value) {
        return Text.translatable("screen.blockreskinner.connection.button", Text.translatable(labelKey), valueText(value));
    }

    private Text valueText(ConnectionOverride value) {
        return switch (value) {
            case AUTO -> Text.translatable("screen.blockreskinner.connection.auto");
            case FORCE_ON -> Text.translatable("screen.blockreskinner.connection.connected");
            case FORCE_OFF -> Text.translatable("screen.blockreskinner.connection.disconnected");
        };
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
        context.drawTextWithShadow(textRenderer, Text.translatable("screen.blockreskinner.connection_controls"), gridLeft(), height - 146, 0xFFFFFF);
        context.drawWrappedTextWithShadow(textRenderer, Text.translatable("screen.blockreskinner.connected.description"), gridLeft(), height - 134, gridWidth(), 0xBBBBBB);
    }

    @Override
    protected int gridBottom() {
        return height - 158;
    }
}
