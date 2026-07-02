package com.skrra.blockreskinner.screen;

import com.skrra.blockreskinner.networking.payload.ApplyConnectedSkinPayload;
import com.skrra.blockreskinner.screen.widget.ConnectionEditorWidget;
import com.skrra.blockreskinner.screen.widget.ModernScreenUtil;
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
    private static final int ROW_PITCH = 26;

    private final boolean northConnected;
    private final boolean eastConnected;
    private final boolean southConnected;
    private final boolean westConnected;
    private ConnectionOverride north = ConnectionOverride.AUTO;
    private ConnectionOverride east = ConnectionOverride.AUTO;
    private ConnectionOverride south = ConnectionOverride.AUTO;
    private ConnectionOverride west = ConnectionOverride.AUTO;

    public ConnectedBlockSkinScreen(BlockPos pos, boolean northConnected, boolean eastConnected, boolean southConnected, boolean westConnected) {
        super(pos, connectedStates(pos), Text.translatable("screen.blockreskinner.connected.title"));
        this.northConnected = northConnected;
        this.eastConnected = eastConnected;
        this.southConnected = southConnected;
        this.westConnected = westConnected;
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
        if (search != null) {
            search.setPlaceholder(Text.translatable("screen.blockreskinner.search.materials"));
        }
        Layout layout = layout();
        int w = overrideButtonWidth(layout);
        int x = layout.contentRight() - w - 10;
        int y = connectionStartY(layout);
        addDrawableChild(directionButton(x, y, w, () -> north, value -> north = value));
        addDrawableChild(directionButton(x, y + ROW_PITCH, w, () -> east, value -> east = value));
        addDrawableChild(directionButton(x, y + ROW_PITCH * 2, w, () -> south, value -> south = value));
        addDrawableChild(directionButton(x, y + ROW_PITCH * 3, w, () -> west, value -> west = value));
    }

    private ButtonWidget directionButton(int x, int y, int width, java.util.function.Supplier<ConnectionOverride> getter, java.util.function.Consumer<ConnectionOverride> setter) {
        return ButtonWidget.builder(valueText(getter.get()), button -> {
                    ConnectionOverride next = ConnectionEditorWidget.next(getter.get());
                    setter.accept(next);
                    button.setMessage(valueText(next));
                })
                .dimensions(x, y, width, 20)
                .build();
    }

    private Text valueText(ConnectionOverride value) {
        return switch (value) {
            case AUTO -> Text.translatable("screen.blockreskinner.connected.override.auto");
            case FORCE_ON -> Text.translatable("screen.blockreskinner.connected.override.connected");
            case FORCE_OFF -> Text.translatable("screen.blockreskinner.connected.override.disconnected");
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
    protected void renderChrome(DrawContext context, Layout layout) {
        ModernScreenUtil.panel(context, layout.panelX(), layout.panelY(), layout.panelW(), layout.panelH());
        context.drawTextWithShadow(textRenderer, Text.translatable("screen.blockreskinner.connected.title"), layout.contentX(), layout.panelY() + 11, ModernScreenUtil.TEXT);
        context.drawTextWithShadow(textRenderer, Text.translatable("screen.blockreskinner.connected.subtitle"), layout.contentX(), layout.panelY() + 26, ModernScreenUtil.MUTED);
        context.fill(layout.contentX(), layout.panelY() + HEADER_HEIGHT, layout.contentRight(), layout.panelY() + HEADER_HEIGHT + 1, 0x553A4555);
        ModernScreenUtil.softCard(context, layout.searchX() - 1, layout.searchY() - 1, layout.searchW() + 2, SEARCH_HEIGHT + 2);
    }

    @Override
    protected void renderCategories(DrawContext context, Layout layout, int mouseX, int mouseY) {
    }

    @Override
    protected void renderSelectedPanel(DrawContext context, Layout layout) {
        int sideX = sideX(layout);
        int sideW = sideW(layout);
        ModernScreenUtil.card(context, sideX, layout.contentY(), sideW, layout.contentH());
        context.drawTextWithShadow(textRenderer, Text.translatable("screen.blockreskinner.connected.material"), sideX + 10, layout.contentY() + 8, ModernScreenUtil.MUTED);

        if (selected != null) {
            drawBlockPreview(context, selected, sideX + 10, layout.contentY() + 20, 36);
            context.drawTextWithShadow(textRenderer, Text.literal(ModernScreenUtil.trim(textRenderer, selected.getBlock().getName().getString(), sideW - 66)), sideX + 54, layout.contentY() + 25, ModernScreenUtil.TEXT);
            context.drawTextWithShadow(textRenderer, Text.literal(ModernScreenUtil.trim(textRenderer, BlockStateUtil.toString(selected), sideW - 66)), sideX + 54, layout.contentY() + 38, ModernScreenUtil.SUBTLE);
        } else {
            context.drawTextWithShadow(textRenderer, Text.translatable("screen.blockreskinner.no_selection"), sideX + 10, layout.contentY() + 25, ModernScreenUtil.SUBTLE);
        }

        int headerY = controlsHeaderY(layout);
        context.drawTextWithShadow(textRenderer, Text.translatable("screen.blockreskinner.connection_controls"), sideX + 10, headerY, ModernScreenUtil.TEXT);
        context.drawWrappedTextWithShadow(textRenderer, Text.translatable("screen.blockreskinner.connected.description"), sideX + 10, headerY + 12, sideW - 20, ModernScreenUtil.MUTED);

        int rowY = connectionStartY(layout);
        renderConnectionRow(context, layout, rowY, Text.translatable("screen.blockreskinner.direction.north"), northConnected);
        renderConnectionRow(context, layout, rowY + ROW_PITCH, Text.translatable("screen.blockreskinner.direction.east"), eastConnected);
        renderConnectionRow(context, layout, rowY + ROW_PITCH * 2, Text.translatable("screen.blockreskinner.direction.south"), southConnected);
        renderConnectionRow(context, layout, rowY + ROW_PITCH * 3, Text.translatable("screen.blockreskinner.direction.west"), westConnected);
    }

    private void renderConnectionRow(DrawContext context, Layout layout, int y, Text direction, boolean connected) {
        int sideX = sideX(layout);
        int sideW = sideW(layout);
        int buttonW = overrideButtonWidth(layout);
        int x = sideX + 10;
        int rowW = sideW - 20;
        context.fill(x, y - 2, x + rowW, y + 22, 0x661D2230);
        context.drawTextWithShadow(textRenderer, direction, x + 6, y + 6, ModernScreenUtil.TEXT);

        int statusX = x + 46;
        int statusW = rowW - buttonW - 54;
        Text status = connected
                ? Text.translatable("screen.blockreskinner.connected.current.connected.short")
                : Text.translatable("screen.blockreskinner.connected.current.disconnected.short");
        int color = connected ? ModernScreenUtil.GOOD : ModernScreenUtil.SUBTLE;
        String trimmed = ModernScreenUtil.trim(textRenderer, status.getString(), Math.max(30, statusW));
        context.drawTextWithShadow(textRenderer, Text.literal(trimmed), statusX, y + 6, color);
    }

    @Override
    protected Layout layout() {
        int panelW = Math.min(Math.max(560, width - 24), 1040);
        if (width < 584) {
            panelW = Math.max(300, width - 12);
        }
        int panelH = Math.min(Math.max(320, height - 24), 620);
        if (height < 344) {
            panelH = Math.max(260, height - 12);
        }
        int panelX = (width - panelW) / 2;
        int panelY = (height - panelH) / 2;
        int contentX = panelX + PAD;
        int contentW = panelW - PAD * 2;
        int searchY = panelY + HEADER_HEIGHT + GAP;
        int footerY = panelY + panelH - FOOTER_HEIGHT;
        int contentY = searchY + SEARCH_HEIGHT + GAP;
        int contentH = Math.max(170, footerY - GAP - contentY);
        int sideW = Math.min(304, Math.max(238, contentW / 3));
        int gridW = Math.max(220, contentW - sideW - GAP);
        int tile = panelH < 390 ? 52 : 60;
        return new Layout(panelX, panelY, panelW, panelH, contentX, contentW, searchY, contentY, contentH, contentY, 0, 0, contentX, gridW, tile, false);
    }

    private int sideX(Layout layout) {
        return layout.gridX() + layout.gridW() + GAP;
    }

    private int sideW(Layout layout) {
        return layout.contentRight() - sideX(layout);
    }

    private int overrideButtonWidth(Layout layout) {
        return Math.min(96, Math.max(70, sideW(layout) - 158));
    }

    private int controlsHeaderY(Layout layout) {
        return layout.contentY() + 62;
    }

    /**
     * Rows start below the wrapped description; clamped so all four rows and
     * their buttons always fit inside the side panel.
     */
    private int connectionStartY(Layout layout) {
        int descLines = textRenderer.wrapLines(Text.translatable("screen.blockreskinner.connected.description"), sideW(layout) - 20).size();
        int wanted = controlsHeaderY(layout) + 12 + descLines * 9 + 8;
        int latest = layout.contentY() + layout.contentH() - ROW_PITCH * 4 + 2;
        return Math.min(wanted, latest);
    }
}
