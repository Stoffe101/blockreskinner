package com.skrra.blockreskinner.screen;

import com.skrra.blockreskinner.networking.payload.ApplyConnectedSkinPayload;
import com.skrra.blockreskinner.screen.layout.ReskinLayout;
import com.skrra.blockreskinner.screen.layout.ReskinLayoutProfile;
import com.skrra.blockreskinner.screen.widget.ConnectionEditorWidget;
import com.skrra.blockreskinner.screen.widget.ModernUi;
import com.skrra.blockreskinner.skin.ClientSkinCache;
import com.skrra.blockreskinner.skin.ConnectedSkinData;
import com.skrra.blockreskinner.skin.ConnectionOverride;
import com.skrra.blockreskinner.skin.SkinQueries;
import com.skrra.blockreskinner.util.BlockStateUtil;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.WallShape;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.state.property.Properties;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * Connected block (fence/wall/pane) skin picker. Shares the virtual-canvas
 * chrome with BlockSkinScreen; the category sidebar is replaced by a
 * right-side details panel with a top-down connection preview built from the
 * real neighboring blocks. The four side cells of the preview are the
 * override controls: clicking a side cycles Auto -> Connected -> Disconnected.
 */
public class ConnectedBlockSkinScreen extends BlockSkinScreen {
    private static final int ROW_PITCH = 26;
    private static final int ROW_HEIGHT = 24;
    private static final int CELL = 34;
    private static final int ARM = 12;
    private static final int GRID = CELL * 3 + ARM * 2;
    private static final int STATUS_LINE = 12;
    private static final int PREVIEW_HEIGHT = GRID + 6 + STATUS_LINE;

    private final boolean[] currentConnections;
    /** Real neighboring block states (N, E, S, W), read once for GUI display only. */
    private final BlockState[] neighbors = new BlockState[4];
    private final ConnectionOverride[] overrides = {
            ConnectionOverride.AUTO, ConnectionOverride.AUTO, ConnectionOverride.AUTO, ConnectionOverride.AUTO
    };
    private final ConnectionOverride[] initialOverrides = {
            ConnectionOverride.AUTO, ConnectionOverride.AUTO, ConnectionOverride.AUTO, ConnectionOverride.AUTO
    };

    public ConnectedBlockSkinScreen(BlockPos pos, boolean northConnected, boolean eastConnected, boolean southConnected, boolean westConnected) {
        super(pos, connectedStates(pos), Text.translatable("screen.blockreskinner.connected.title"));
        this.currentConnections = new boolean[]{northConnected, eastConnected, southConnected, westConnected};
        this.searchPlaceholder = Text.translatable("screen.blockreskinner.search.materials");
        this.headerTitle = Text.translatable("screen.blockreskinner.connected.title");
        this.headerSubtitle = Text.translatable("screen.blockreskinner.connected.subtitle");
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null) {
            neighbors[0] = client.world.getBlockState(pos.north());
            neighbors[1] = client.world.getBlockState(pos.east());
            neighbors[2] = client.world.getBlockState(pos.south());
            neighbors[3] = client.world.getBlockState(pos.west());
        }
        loadCurrentConnectedSkin();
    }

    private static List<BlockState> connectedStates(BlockPos pos) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return List.of();
        }
        return SkinQueries.connectedVisualStates(client.world.getBlockState(pos));
    }

    @Override
    protected ReskinLayout buildLayout(ReskinLayoutProfile profile) {
        return ReskinLayout.forConnected(profile);
    }

    @Override
    protected void apply() {
        BlockState material = materialToApply();
        if (material != null) {
            ClientPlayNetworking.send(new ApplyConnectedSkinPayload(pos, BlockStateUtil.toString(material),
                    overrides[0], overrides[1], overrides[2], overrides[3]));
            close();
        }
    }

    @Override
    protected void updateApplyButton() {
        if (applyButton != null) {
            // Connection-only edits are valid: no material selection required.
            applyButton.setEnabled(materialToApply() != null && (materialChanged() || overridesChanged()));
        }
        if (clearButton != null) {
            clearButton.setEnabled(currentVisualState != null);
        }
    }

    @Override
    protected void renderDetailsPanel(DrawContext context, int mouseX, int mouseY) {
        ReskinLayout.Rect panel = layout.sidePanel;
        ModernUi.panel(context, panel.x(), panel.y(), panel.w(), panel.h());
        Sections sections = sections();
        int textX = panel.x() + 10;
        int y = panel.y() + 7;

        // Editing: the actual clicked block, with a preview so the user can see
        // at a glance what they are editing, not just read its name.
        int realPreview = 30;
        int realPreviewX = panel.right() - 10 - realPreview;
        if (realState != null) {
            drawBlockPreview(context, realState, realPreviewX, panel.y() + 8, realPreview);
        }
        int infoWide = realPreviewX - textX - 8;
        context.drawTextWithShadow(textRenderer, Text.translatable("screen.blockreskinner.editing"),
                textX, y, ModernUi.TEXT_MUTED);
        drawStateLine(context, Text.translatable("screen.blockreskinner.real_block"), realState,
                textX, y + 12, infoWide, ModernUi.TEXT_PRIMARY);
        drawIdLine(context, realState, textX, y + 24, infoWide);
        drawStateLine(context, Text.translatable("screen.blockreskinner.current_visual_material"), currentVisualState,
                textX, y + 38, panel.w() - 20, ModernUi.TEXT_MUTED);

        int newY = y + 55;
        context.drawTextWithShadow(textRenderer, Text.translatable("screen.blockreskinner.new_visual_material"),
                textX, newY, ModernUi.TEXT_MUTED);
        BlockState newMaterial = selected != null ? selected : currentVisualState;
        BlockState newPreview = newMaterial != null ? newMaterial : realState;
        if (newPreview != null) {
            drawBlockPreview(context, newPreview, textX, newY + 12, 34);
        }
        int infoX = textX + 42;
        int infoW = panel.right() - 10 - infoX;
        if (newMaterial != null) {
            context.drawTextWithShadow(textRenderer,
                    Text.literal(ModernUi.trim(textRenderer, newMaterial.getBlock().getName().getString(), infoW)),
                    infoX, newY + 16, ModernUi.TEXT_PRIMARY);
            context.drawTextWithShadow(textRenderer,
                    Text.literal(ModernUi.trim(textRenderer, BlockStateUtil.toString(newMaterial), infoW)),
                    infoX, newY + 29, ModernUi.TEXT_DISABLED);
        } else {
            // No material picked: connection-only edits keep the block's own look.
            context.drawTextWithShadow(textRenderer, Text.translatable("screen.blockreskinner.same_as_real_block"),
                    infoX, newY + 16, ModernUi.TEXT_PRIMARY);
            context.drawTextWithShadow(textRenderer, Text.translatable("screen.blockreskinner.original_texture"),
                    infoX, newY + 29, ModernUi.TEXT_DISABLED);
        }

        ModernUi.rule(context, textX, sections.ruleY(), panel.w() - 20, 48);
        context.drawTextWithShadow(textRenderer, Text.translatable("screen.blockreskinner.connection_controls"),
                textX, sections.ruleY() + 8, ModernUi.TEXT_PRIMARY);
        for (OrderedText line : hintLines()) {
            context.drawText(textRenderer, line, textX, sections.hintY(), ModernUi.TEXT_MUTED, true);
            break; // single-line hint; wrapLines used only as a safety net
        }

        renderConnectionPreview(context, panel, sections.compassTop(), mouseX, mouseY);

        if (sections.showRows()) {
            renderOverrideRows(context, panel, sections.rowsTop(), textX);
        }
    }

    private void renderOverrideRows(DrawContext context, ReskinLayout.Rect panel, int top, int textX) {
        Text[] directions = {
                Text.translatable("screen.blockreskinner.direction.north"),
                Text.translatable("screen.blockreskinner.direction.east"),
                Text.translatable("screen.blockreskinner.direction.south"),
                Text.translatable("screen.blockreskinner.direction.west")
        };
        for (int i = 0; i < 4; i++) {
            int rowY = top + i * ROW_PITCH;
            int rowW = panel.w() - 20;
            context.fill(textX, rowY, textX + rowW, rowY + ROW_HEIGHT,
                    i % 2 == 0 ? ModernUi.withAlpha(ModernUi.CARD_BACKGROUND, 0x66) : ModernUi.withAlpha(ModernUi.CARD_BACKGROUND, 0x3A));
            context.drawTextWithShadow(textRenderer, directions[i], textX + 6, rowY + 4, ModernUi.TEXT_PRIMARY);

            boolean connected = currentConnections[i];
            Text realStatus = connected
                    ? Text.translatable("screen.blockreskinner.connected.current.connected.short")
                    : Text.translatable("screen.blockreskinner.connected.current.disconnected.short");
            int statusColor = connected ? ModernUi.SUCCESS : ModernUi.TEXT_MUTED;
            int statusX = textX + 52;
            int statusW = Math.max(55, rowW / 2 - 40);
            context.drawTextWithShadow(textRenderer,
                    Text.literal(ModernUi.trim(textRenderer,
                            Text.translatable("screen.blockreskinner.connected.real_compact", realStatus).getString(), statusW)),
                    statusX, rowY + 4, statusColor);
            String override = Text.translatable("screen.blockreskinner.connected.override.compact").getString()
                    + " " + overrideText(overrides[i]).getString();
            context.drawTextWithShadow(textRenderer,
                    Text.literal(ModernUi.trim(textRenderer, override, rowW - statusX + textX - statusW - 8)),
                    statusX + statusW + 8, rowY + 4, overrideColor(i));
        }
    }

    @Override
    protected boolean handleExtraClick(double vx, double vy) {
        int side = sideCellAt(vx, vy);
        if (side >= 0) {
            overrides[side] = ConnectionEditorWidget.next(overrides[side]);
            updateApplyButton();
            return true;
        }
        return false;
    }

    private List<OrderedText> hintLines() {
        return textRenderer.wrapLines(Text.translatable("screen.blockreskinner.connected.hint"), layout.sidePanel.w() - 20);
    }

    /**
     * Section positions inside the side panel. The fixed info sections end at
     * panel.y()+110; below that the rule/header, hint line, connection preview
     * and (space permitting) the compact rows are laid out top-down. The rows
     * are the only droppable piece — the preview itself is the control.
     */
    private record Sections(int ruleY, int hintY, int compassTop, boolean showRows, int rowsTop) {
    }

    private Sections sections() {
        ReskinLayout.Rect panel = layout.sidePanel;
        int ruleY = panel.y() + 114;
        int hintY = ruleY + 20;
        int compassTop = hintY + 14;
        int bottom = panel.bottom() - 8;
        // Degenerate windows: keep the preview inside the panel even if it has
        // to hug the hint; the test matrix sizes never hit this clamp.
        compassTop = Math.min(compassTop, Math.max(hintY + 10, bottom - PREVIEW_HEIGHT));
        boolean showRows = compassTop + PREVIEW_HEIGHT + 10 + 4 * ROW_PITCH <= bottom;
        int rowsTop = compassTop + PREVIEW_HEIGHT + 10;
        return new Sections(ruleY, hintY, compassTop, showRows, rowsTop);
    }

    /**
     * Top-down 3x3 connection preview: the center cell shows the material with
     * a synthetic connected state (the final visual result), the side cells
     * show the real neighboring blocks, and the arms between them show the
     * resulting visual connection per side. Side cells are click targets.
     */
    private void renderConnectionPreview(DrawContext context, ReskinLayout.Rect panel, int top, int mouseX, int mouseY) {
        // Arms first, underneath the cells.
        for (int i = 0; i < 4; i++) {
            ReskinLayout.Rect center = previewCell(top, -1);
            ReskinLayout.Rect side = previewCell(top, i);
            int color = armColor(i);
            switch (i) {
                case 0 -> context.fill(center.x() + CELL / 2 - 1, side.bottom(), center.x() + CELL / 2 + 2, center.y(), color);
                case 1 -> context.fill(center.right(), side.y() + CELL / 2 - 1, side.x(), side.y() + CELL / 2 + 2, color);
                case 2 -> context.fill(center.x() + CELL / 2 - 1, center.bottom(), center.x() + CELL / 2 + 2, side.y(), color);
                case 3 -> context.fill(side.right(), side.y() + CELL / 2 - 1, center.x(), side.y() + CELL / 2 + 2, color);
            }
        }

        // Neighbor cells (N, E, S, W) — the clickable override controls.
        for (int i = 0; i < 4; i++) {
            ReskinLayout.Rect cell = previewCell(top, i);
            BlockState neighbor = neighbors[i];
            boolean present = neighbor != null && !neighbor.isAir() && neighbor.getRenderType() == BlockRenderType.MODEL;
            boolean hovered = cell.contains(mouseX, mouseY);
            int fill = present ? ModernUi.withAlpha(ModernUi.CARD_BACKGROUND, hovered ? 0xE0 : 0xAA)
                    : ModernUi.withAlpha(ModernUi.CARD_BACKGROUND, hovered ? 0x77 : 0x3A);
            context.fill(cell.x(), cell.y(), cell.right(), cell.bottom(), fill);
            ModernUi.border(context, cell.x(), cell.y(), cell.w(), cell.h(), cellBorderColor(i, hovered));
            if (present) {
                drawBlockPreview(context, neighbor, cell.x() + 4, cell.y() + 4, CELL - 8);
            } else {
                context.drawCenteredTextWithShadow(textRenderer, Text.literal("·"),
                        cell.x() + CELL / 2, cell.y() + (CELL - 8) / 2, ModernUi.TEXT_DISABLED);
            }
        }

        // Center cell: material with the final visual connections applied.
        ReskinLayout.Rect center = previewCell(top, -1);
        context.fill(center.x(), center.y(), center.right(), center.bottom(), ModernUi.CARD_BACKGROUND);
        ModernUi.border(context, center.x(), center.y(), center.w(), center.h(), ModernUi.BORDER);
        BlockState centerState = previewCenterState();
        if (centerState != null) {
            drawBlockPreview(context, centerState, center.x() + 4, center.y() + 4, CELL - 8);
        }

        // Compact per-side override status under the grid, spread across the panel.
        int statusY = top + GRID + 6;
        int statusX = panel.x() + 10;
        String[] letters = {"N", "E", "S", "W"};
        int segment = (panel.w() - 20) / 4;
        for (int i = 0; i < 4; i++) {
            String label = letters[i] + " " + shortOverrideText(overrides[i]).getString();
            context.drawTextWithShadow(textRenderer,
                    Text.literal(ModernUi.trim(textRenderer, label, segment - 4)),
                    statusX + i * segment, statusY, overrideColor(i));
        }
    }

    /** Cell rect within the preview grid: -1 = center, 0..3 = N/E/S/W. */
    private ReskinLayout.Rect previewCell(int top, int index) {
        ReskinLayout.Rect panel = layout.sidePanel;
        int centerX = panel.x() + panel.w() / 2;
        int left = centerX - GRID / 2;
        int midX = centerX - CELL / 2;
        int midY = top + CELL + ARM;
        return switch (index) {
            case 0 -> new ReskinLayout.Rect(midX, top, CELL, CELL);
            case 1 -> new ReskinLayout.Rect(left + GRID - CELL, midY, CELL, CELL);
            case 2 -> new ReskinLayout.Rect(midX, top + GRID - CELL, CELL, CELL);
            case 3 -> new ReskinLayout.Rect(left, midY, CELL, CELL);
            default -> new ReskinLayout.Rect(midX, midY, CELL, CELL);
        };
    }

    private int sideCellAt(double vx, double vy) {
        if (layout == null || layout.sidePanel == null) {
            return -1;
        }
        int top = sections().compassTop();
        for (int i = 0; i < 4; i++) {
            if (previewCell(top, i).contains(vx, vy)) {
                return i;
            }
        }
        return -1;
    }

    private int armColor(int index) {
        return switch (overrides[index]) {
            case AUTO -> currentConnections[index] ? ModernUi.SUCCESS : ModernUi.withAlpha(ModernUi.BORDER_SOFT, 0x44);
            case FORCE_ON -> ModernUi.ACCENT;
            case FORCE_OFF -> 0x66E07A8A;
        };
    }

    private int cellBorderColor(int index, boolean hovered) {
        return switch (overrides[index]) {
            case AUTO -> hovered ? ModernUi.BORDER_STRONG : ModernUi.BORDER_SOFT;
            case FORCE_ON -> ModernUi.ACCENT;
            case FORCE_OFF -> 0xFFE07A8A;
        };
    }

    private boolean effectiveConnection(int index) {
        return switch (overrides[index]) {
            case AUTO -> currentConnections[index];
            case FORCE_ON -> true;
            case FORCE_OFF -> false;
        };
    }

    private int overrideColor(int index) {
        return switch (overrides[index]) {
            case AUTO -> currentConnections[index] ? ModernUi.SUCCESS : ModernUi.TEXT_MUTED;
            case FORCE_ON -> ModernUi.ACCENT;
            case FORCE_OFF -> 0xFFE07A8A;
        };
    }

    private void loadCurrentConnectedSkin() {
        if (ClientSkinCache.get(pos) instanceof ConnectedSkinData connected) {
            currentVisualState = connected.visualMaterialState();
            selected = findMatchingState(currentVisualState);
            overrides[0] = connected.north();
            overrides[1] = connected.east();
            overrides[2] = connected.south();
            overrides[3] = connected.west();
            System.arraycopy(overrides, 0, initialOverrides, 0, overrides.length);
        }
    }

    /**
     * Material sent with Apply. Falls back to the real block's own material so
     * connection-only edits work without picking a new material; the GUI shows
     * that case as "Same as real block".
     */
    private BlockState materialToApply() {
        if (selected != null) {
            return selected;
        }
        if (currentVisualState != null) {
            return currentVisualState;
        }
        return realState == null ? null : realState.getBlock().getDefaultState();
    }

    /** Synthetic state for the center preview: material + final visual connections. */
    private BlockState previewCenterState() {
        BlockState material = materialToApply();
        if (material == null) {
            return null;
        }
        BlockState state = material;
        if (state.contains(Properties.NORTH)) {
            state = state.with(Properties.NORTH, effectiveConnection(0));
        }
        if (state.contains(Properties.EAST)) {
            state = state.with(Properties.EAST, effectiveConnection(1));
        }
        if (state.contains(Properties.SOUTH)) {
            state = state.with(Properties.SOUTH, effectiveConnection(2));
        }
        if (state.contains(Properties.WEST)) {
            state = state.with(Properties.WEST, effectiveConnection(3));
        }
        if (state.contains(Properties.NORTH_WALL_SHAPE)) {
            state = state.with(Properties.NORTH_WALL_SHAPE, effectiveConnection(0) ? WallShape.LOW : WallShape.NONE);
        }
        if (state.contains(Properties.EAST_WALL_SHAPE)) {
            state = state.with(Properties.EAST_WALL_SHAPE, effectiveConnection(1) ? WallShape.LOW : WallShape.NONE);
        }
        if (state.contains(Properties.SOUTH_WALL_SHAPE)) {
            state = state.with(Properties.SOUTH_WALL_SHAPE, effectiveConnection(2) ? WallShape.LOW : WallShape.NONE);
        }
        if (state.contains(Properties.WEST_WALL_SHAPE)) {
            state = state.with(Properties.WEST_WALL_SHAPE, effectiveConnection(3) ? WallShape.LOW : WallShape.NONE);
        }
        if (state.contains(Properties.UP)) {
            state = state.with(Properties.UP, true);
        }
        return state;
    }

    private boolean materialChanged() {
        if (selected == null) {
            return false;
        }
        return !statesEqual(selected, currentVisualState);
    }

    private boolean overridesChanged() {
        for (int i = 0; i < overrides.length; i++) {
            if (overrides[i] != initialOverrides[i]) {
                return true;
            }
        }
        return false;
    }

    private Text overrideText(ConnectionOverride value) {
        return switch (value) {
            case AUTO -> Text.translatable("screen.blockreskinner.connected.override.auto");
            case FORCE_ON -> Text.translatable("screen.blockreskinner.connected.override.connected");
            case FORCE_OFF -> Text.translatable("screen.blockreskinner.connected.override.disconnected");
        };
    }

    private Text shortOverrideText(ConnectionOverride value) {
        return switch (value) {
            case AUTO -> Text.translatable("screen.blockreskinner.connected.override.short.auto");
            case FORCE_ON -> Text.translatable("screen.blockreskinner.connected.override.short.on");
            case FORCE_OFF -> Text.translatable("screen.blockreskinner.connected.override.short.off");
        };
    }
}
