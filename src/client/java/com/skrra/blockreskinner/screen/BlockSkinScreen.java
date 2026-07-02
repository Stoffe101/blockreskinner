package com.skrra.blockreskinner.screen;

import com.skrra.blockreskinner.networking.payload.ApplySimpleSkinPayload;
import com.skrra.blockreskinner.networking.payload.ClearSkinPayload;
import com.skrra.blockreskinner.render.BlockPreviewGuiElementRenderState;
import com.skrra.blockreskinner.screen.widget.BlockGridWidget;
import com.skrra.blockreskinner.screen.widget.BlockSearchWidget;
import com.skrra.blockreskinner.screen.widget.ModernScreenUtil;
import com.skrra.blockreskinner.skin.SkinCategory;
import com.skrra.blockreskinner.skin.SkinQueries;
import com.skrra.blockreskinner.util.BlockStateUtil;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BlockSkinScreen extends Screen {
    protected static final int PAD = 14;
    protected static final int GAP = 8;
    protected static final int HEADER_HEIGHT = 46;
    protected static final int SEARCH_HEIGHT = 20;
    protected static final int FOOTER_HEIGHT = 34;
    protected static final int CATEGORY_ROW = 22;

    protected final BlockPos pos;
    protected final List<BlockState> allStates;
    protected final List<BlockState> filteredStates = new ArrayList<>();
    protected BlockSearchWidget search;
    protected ButtonWidget applyButton;
    protected BlockState selected;
    protected int scrollRows;
    protected int categoryScroll;
    protected FilterCategory activeCategory = FilterCategory.ALL;
    private BlockState hoveredState;

    public BlockSkinScreen(BlockPos pos) {
        this(pos, SkinQueries.simpleVisualStates(), Text.translatable("screen.blockreskinner.simple.title"));
    }

    protected BlockSkinScreen(BlockPos pos, List<BlockState> allStates, Text title) {
        super(title);
        this.pos = pos;
        this.allStates = allStates;
        this.filteredStates.addAll(allStates);
        this.selected = allStates.isEmpty() ? null : allStates.get(0);
    }

    @Override
    protected void init() {
        Layout layout = layout();
        search = new BlockSearchWidget(textRenderer, layout.searchX(), layout.searchY(), layout.searchW(), SEARCH_HEIGHT);
        search.setPlaceholder(Text.translatable("screen.blockreskinner.search.placeholder"));
        search.setChangedListener(this::filter);
        addDrawableChild(search);

        int buttonY = layout.footerY() + 7;
        int buttonW = Math.min(110, (layout.contentW() - GAP * 2) / 3);
        applyButton = ButtonWidget.builder(Text.translatable("screen.blockreskinner.apply"), button -> apply())
                .dimensions(layout.contentX(), buttonY, buttonW, 20)
                .build();
        addDrawableChild(applyButton);
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.blockreskinner.clear"), button -> clear())
                .dimensions(layout.contentX() + buttonW + GAP, buttonY, buttonW, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), button -> close())
                .dimensions(layout.contentRight() - buttonW, buttonY, buttonW, 20)
                .build());
        updateApplyButton();
    }

    protected void apply() {
        if (selected != null) {
            ClientPlayNetworking.send(new ApplySimpleSkinPayload(pos, BlockStateUtil.toString(selected)));
            close();
        }
    }

    protected void clear() {
        ClientPlayNetworking.send(new ClearSkinPayload(pos));
        close();
    }

    protected void filter(String query) {
        String needle = query.toLowerCase(Locale.ROOT).trim();
        filteredStates.clear();
        for (BlockState state : allStates) {
            if (activeCategory.accepts(state) && matches(state, needle)) {
                filteredStates.add(state);
            }
        }
        scrollRows = 0;
        if (!filteredStates.contains(selected)) {
            selected = filteredStates.isEmpty() ? null : filteredStates.get(0);
        }
        updateApplyButton();
    }

    private boolean matches(BlockState state, String needle) {
        if (needle.isEmpty()) {
            return true;
        }
        String id = Registries.BLOCK.getId(state.getBlock()).toString();
        String category = Text.translatable(SkinQueries.category(state).translationKey()).getString();
        SkinQueries.TextKey axisLabel = SkinQueries.axisLabelKey(state);
        String axis = axisLabel == null ? "" : Text.translatable(axisLabel.labelKey()).getString() + " " + axisLabel.debugText();
        return id.toLowerCase(Locale.ROOT).contains(needle)
                || state.getBlock().getName().getString().toLowerCase(Locale.ROOT).contains(needle)
                || category.toLowerCase(Locale.ROOT).contains(needle)
                || axis.toLowerCase(Locale.ROOT).contains(needle);
    }

    private void updateApplyButton() {
        if (applyButton != null) {
            applyButton.active = selected != null;
        }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.renderBackground(context, mouseX, mouseY, deltaTicks);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        hoveredState = null;
        Layout layout = layout();
        renderChrome(context, layout);
        renderCategories(context, layout, mouseX, mouseY);
        renderGrid(context, layout, mouseX, mouseY);
        renderSelectedPanel(context, layout);
        super.render(context, mouseX, mouseY, deltaTicks);
        renderHoveredTooltip(context, mouseX, mouseY);
    }

    protected void renderChrome(DrawContext context, Layout layout) {
        ModernScreenUtil.panel(context, layout.panelX(), layout.panelY(), layout.panelW(), layout.panelH());
        context.drawTextWithShadow(textRenderer, Text.translatable("screen.blockreskinner.header"), layout.contentX(), layout.panelY() + 11, ModernScreenUtil.TEXT);
        context.drawTextWithShadow(textRenderer, Text.translatable("screen.blockreskinner.simple.description"), layout.contentX(), layout.panelY() + 26, ModernScreenUtil.MUTED);
        context.fill(layout.contentX(), layout.panelY() + HEADER_HEIGHT, layout.contentRight(), layout.panelY() + HEADER_HEIGHT + 1, 0x553A4555);
        ModernScreenUtil.softCard(context, layout.searchX() - 1, layout.searchY() - 1, layout.searchW() + 2, SEARCH_HEIGHT + 2);
    }

    protected void renderCategories(DrawContext context, Layout layout, int mouseX, int mouseY) {
        if (!layout.hasCategories()) {
            return;
        }
        ModernScreenUtil.card(context, layout.categoryX(), layout.contentY(), layout.categoryW(), layout.contentH());
        context.drawTextWithShadow(textRenderer, Text.translatable("screen.blockreskinner.categories"), layout.categoryX() + 8, layout.contentY() + 8, ModernScreenUtil.MUTED);

        FilterCategory[] categories = FilterCategory.values();
        int visible = visibleCategoryRows(layout);
        categoryScroll = Math.max(0, Math.min(categoryScroll, categories.length - visible));
        int y = layout.contentY() + 24;
        for (int i = categoryScroll; i < Math.min(categories.length, categoryScroll + visible); i++) {
            FilterCategory category = categories[i];
            boolean hovered = mouseX >= layout.categoryX() + 6 && mouseX < layout.categoryX() + layout.categoryW() - 6
                    && mouseY >= y && mouseY < y + 20;
            boolean selectedCategory = activeCategory == category;
            int fill = selectedCategory ? ModernScreenUtil.ACCENT_SOFT : hovered ? 0x332B3442 : 0;
            if (fill != 0) {
                context.fill(layout.categoryX() + 6, y, layout.categoryX() + layout.categoryW() - 6, y + 20, fill);
            }
            int color = selectedCategory ? ModernScreenUtil.TEXT : ModernScreenUtil.MUTED;
            String label = ModernScreenUtil.trim(textRenderer, Text.translatable(category.translationKey()).getString(), layout.categoryW() - 24);
            context.drawTextWithShadow(textRenderer, Text.literal(label), layout.categoryX() + 11, y + 6, color);
            y += CATEGORY_ROW;
        }

        if (categories.length > visible) {
            int trackX = layout.categoryX() + layout.categoryW() - 4;
            int trackY = layout.contentY() + 24;
            int trackH = visible * CATEGORY_ROW - 2;
            context.fill(trackX, trackY, trackX + 2, trackY + trackH, 0x33121722);
            int barH = Math.max(10, trackH * visible / categories.length);
            int barY = trackY + (trackH - barH) * categoryScroll / Math.max(1, categories.length - visible);
            context.fill(trackX, barY, trackX + 2, barY + barH, 0x884E5F77);
        }
    }

    protected int visibleCategoryRows(Layout layout) {
        return Math.max(1, (layout.contentH() - 30) / CATEGORY_ROW);
    }

    protected void renderGrid(DrawContext context, Layout layout, int mouseX, int mouseY) {
        ModernScreenUtil.card(context, layout.gridX(), layout.contentY(), layout.gridW(), layout.contentH());
        int tile = layout.tile();
        int columns = BlockGridWidget.columns(layout.gridW() - 8, tile);
        int gridX = layout.gridX() + 5;
        int gridY = layout.contentY() + 5;
        int gridW = columns * tile;
        int gridH = layout.contentH() - 10;
        context.enableScissor(gridX, gridY, gridX + Math.min(gridW, layout.gridW() - 10), gridY + gridH);

        int start = scrollRows * columns;
        int visibleRows = Math.max(1, gridH / tile + 1);
        int end = Math.min(filteredStates.size(), start + visibleRows * columns);
        for (int i = start; i < end; i++) {
            BlockState state = filteredStates.get(i);
            int local = i - start;
            int x = gridX + (local % columns) * tile;
            int y = gridY + (local / columns) * tile;
            if (y > gridY + gridH) {
                continue;
            }
            boolean hovered = mouseX >= x && mouseX < x + tile - 4 && mouseY >= y && mouseY < y + tile - 4
                    && mouseY >= gridY && mouseY < gridY + gridH;
            if (hovered) {
                hoveredState = state;
            }
            renderGridTile(context, state, x, y, tile - 4, hovered, state == selected);
        }

        context.disableScissor();
        if (filteredStates.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.blockreskinner.no_results"), layout.gridX() + layout.gridW() / 2, layout.contentY() + layout.contentH() / 2 - 5, ModernScreenUtil.SUBTLE);
        }
    }

    protected void renderGridTile(DrawContext context, BlockState state, int x, int y, int size, boolean hovered, boolean selectedState) {
        int border = selectedState ? ModernScreenUtil.ACCENT : hovered ? 0xFF93A4B8 : 0xFF2D3542;
        int fill = selectedState ? 0xEE1D2D3D : hovered ? 0xEE242B36 : 0xDD171A22;
        context.fill(x, y, x + size, y + size, border);
        context.fill(x + 1, y + 1, x + size - 1, y + size - 1, fill);

        boolean showLabel = size >= 48;
        int preview = size - (showLabel ? 18 : 8);
        drawBlockPreview(context, state, x + (size - preview) / 2, y + 4, preview);

        if (showLabel) {
            SkinQueries.TextKey axisLabel = SkinQueries.axisLabelKey(state);
            String label;
            int color;
            if (axisLabel != null) {
                label = Text.translatable(axisLabel.labelKey()).getString();
                color = ModernScreenUtil.WARN;
            } else {
                label = state.getBlock().getName().getString();
                color = ModernScreenUtil.MUTED;
            }
            label = ModernScreenUtil.trim(textRenderer, label, size - 6);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(label), x + size / 2, y + size - 12, color);
        }
    }

    /**
     * Queues a crisp 3D preview of the block state. Rendered offscreen at
     * native window resolution by BlockPreviewGuiElementRenderer, so it stays
     * sharp at any GUI scale.
     */
    protected void drawBlockPreview(DrawContext context, BlockState state, int x, int y, int size) {
        context.state.addSpecialElement(new BlockPreviewGuiElementRenderState(
                state, x, y, x + size, y + size, size, context.scissorStack.peekLast()));
    }

    protected void renderSelectedPanel(DrawContext context, Layout layout) {
        int selectedH = layout.selectedH();
        ModernScreenUtil.card(context, layout.contentX(), layout.selectedY(), layout.contentW(), selectedH);
        context.drawTextWithShadow(textRenderer, Text.translatable("screen.blockreskinner.selected"), layout.contentX() + 10, layout.selectedY() + 7, ModernScreenUtil.MUTED);
        if (selected == null) {
            context.drawTextWithShadow(textRenderer, Text.translatable("screen.blockreskinner.no_selection"), layout.contentX() + 10, layout.selectedY() + 21, ModernScreenUtil.SUBTLE);
            return;
        }

        int previewSize = selectedH - 16;
        int previewX = layout.contentRight() - previewSize - 10;
        drawBlockPreview(context, selected, previewX, layout.selectedY() + 8, previewSize);

        int textW = layout.contentW() - previewSize - 34;
        SkinQueries.TextKey axisLabel = SkinQueries.axisLabelKey(selected);
        String name = selected.getBlock().getName().getString();
        int lineY = layout.selectedY() + 19;
        context.drawTextWithShadow(textRenderer, Text.literal(ModernScreenUtil.trim(textRenderer, name, textW)), layout.contentX() + 10, lineY, ModernScreenUtil.TEXT);
        lineY += 12;
        if (axisLabel != null) {
            String variant = Text.translatable(axisLabel.labelKey()).getString() + " (" + axisLabel.debugText() + ")";
            context.drawTextWithShadow(textRenderer, Text.translatable("screen.blockreskinner.selected_variant", Text.literal(variant)), layout.contentX() + 10, lineY, ModernScreenUtil.WARN);
        } else {
            Text category = Text.translatable(SkinQueries.category(selected).translationKey());
            context.drawTextWithShadow(textRenderer, Text.translatable("screen.blockreskinner.selected_category", category), layout.contentX() + 10, lineY, ModernScreenUtil.MUTED);
        }
        lineY += 12;
        String state = ModernScreenUtil.trim(textRenderer, BlockStateUtil.toString(selected), textW);
        context.drawTextWithShadow(textRenderer, Text.literal(state), layout.contentX() + 10, lineY, ModernScreenUtil.SUBTLE);
    }

    private void renderHoveredTooltip(DrawContext context, int mouseX, int mouseY) {
        if (hoveredState == null) {
            return;
        }
        List<Text> lines = new ArrayList<>();
        lines.add(hoveredState.getBlock().getName());
        lines.add(Text.literal(Registries.BLOCK.getId(hoveredState.getBlock()).toString()).formatted(Formatting.GRAY));
        SkinQueries.TextKey axisLabel = SkinQueries.axisLabelKey(hoveredState);
        if (axisLabel != null) {
            lines.add(Text.translatable("screen.blockreskinner.tooltip.variant", Text.translatable(axisLabel.labelKey())).formatted(Formatting.YELLOW));
            lines.add(Text.literal(axisLabel.debugText()).formatted(Formatting.GRAY));
        }
        lines.add(Text.translatable(SkinQueries.category(hoveredState).translationKey()).formatted(Formatting.DARK_AQUA));
        context.drawTooltip(textRenderer, lines, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true;
        }
        if (search != null && !search.isMouseOver(click.x(), click.y())) {
            search.setFocused(false);
        }
        FilterCategory category = categoryAt(click.x(), click.y());
        if (category != null) {
            activeCategory = category;
            filter(search == null ? "" : search.getText());
            return true;
        }
        BlockState clicked = stateAt(click.x(), click.y());
        if (clicked != null) {
            selected = clicked;
            updateApplyButton();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE && search != null && search.isFocused()) {
            search.setFocused(false);
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        Layout layout = layout();
        if (layout.hasCategories() && mouseX >= layout.categoryX() && mouseX < layout.categoryX() + layout.categoryW()
                && mouseY >= layout.contentY() && mouseY <= layout.contentY() + layout.contentH()) {
            int visible = visibleCategoryRows(layout);
            int max = Math.max(0, FilterCategory.values().length - visible);
            categoryScroll = Math.max(0, Math.min(max, categoryScroll - (int) Math.signum(verticalAmount)));
            return true;
        }
        if (mouseX >= layout.gridX() && mouseX <= layout.gridX() + layout.gridW() && mouseY >= layout.contentY() && mouseY <= layout.contentY() + layout.contentH()) {
            int columns = BlockGridWidget.columns(layout.gridW() - 8, layout.tile());
            int visibleRows = Math.max(1, (layout.contentH() - 10) / layout.tile());
            int rows = Math.max(0, (filteredStates.size() + columns - 1) / columns - visibleRows);
            scrollRows = Math.max(0, Math.min(rows, scrollRows - (int) Math.signum(verticalAmount)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    protected BlockState stateAt(double mouseX, double mouseY) {
        Layout layout = layout();
        int tile = layout.tile();
        int gridX = layout.gridX() + 5;
        int gridY = layout.contentY() + 5;
        if (mouseX < gridX || mouseX >= layout.gridX() + layout.gridW() - 5 || mouseY < gridY || mouseY >= layout.contentY() + layout.contentH() - 5) {
            return null;
        }
        int columns = BlockGridWidget.columns(layout.gridW() - 8, tile);
        int col = ((int) mouseX - gridX) / tile;
        int row = ((int) mouseY - gridY) / tile;
        if (col >= columns) {
            return null;
        }
        int index = (scrollRows + row) * columns + col;
        return index >= 0 && index < filteredStates.size() ? filteredStates.get(index) : null;
    }

    private FilterCategory categoryAt(double mouseX, double mouseY) {
        Layout layout = layout();
        if (!layout.hasCategories()) {
            return null;
        }
        FilterCategory[] categories = FilterCategory.values();
        int visible = visibleCategoryRows(layout);
        int y = layout.contentY() + 24;
        for (int i = categoryScroll; i < Math.min(categories.length, categoryScroll + visible); i++) {
            if (mouseX >= layout.categoryX() + 6 && mouseX < layout.categoryX() + layout.categoryW() - 6 && mouseY >= y && mouseY < y + 20) {
                return categories[i];
            }
            y += CATEGORY_ROW;
        }
        return null;
    }

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
        int selectedH = panelH < 420 ? 56 : 70;
        int selectedY = panelY + panelH - FOOTER_HEIGHT - GAP - selectedH;
        int contentY = searchY + SEARCH_HEIGHT + GAP;
        int contentH = Math.max(72, selectedY - GAP - contentY);
        boolean hasCategories = panelW >= 560;
        int categoryW = hasCategories ? Math.min(132, Math.max(112, panelW / 7)) : 0;
        int gridX = hasCategories ? contentX + categoryW + GAP : contentX;
        int gridW = contentX + contentW - gridX;
        int tile = panelH < 390 ? BlockGridWidget.MIN_TILE : BlockGridWidget.PREFERRED_TILE;
        return new Layout(panelX, panelY, panelW, panelH, contentX, contentW, searchY, contentY, contentH, selectedY, selectedH, categoryW, gridX, gridW, tile, hasCategories);
    }

    protected MinecraftClient clientOrNull() {
        return MinecraftClient.getInstance();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    protected record Layout(int panelX, int panelY, int panelW, int panelH, int contentX, int contentW, int searchY,
                            int contentY, int contentH, int selectedY, int selectedH, int categoryW, int gridX,
                            int gridW, int tile, boolean hasCategories) {
        int contentRight() {
            return contentX + contentW;
        }

        int searchX() {
            return contentX;
        }

        int searchW() {
            return contentW;
        }

        int categoryX() {
            return contentX;
        }

        int footerY() {
            return panelY + panelH - FOOTER_HEIGHT;
        }
    }

    protected enum FilterCategory {
        ALL("screen.blockreskinner.category.all"),
        FULL_BLOCKS("screen.blockreskinner.category.full_blocks"),
        LOGS_AND_PILLARS("screen.blockreskinner.category.logs_and_pillars"),
        TRANSPARENT("screen.blockreskinner.category.transparent"),
        LEAVES("screen.blockreskinner.category.leaves"),
        CUTOUT("screen.blockreskinner.category.cutout"),
        ORES("screen.blockreskinner.category.ores"),
        STONE_BRICKS("screen.blockreskinner.category.stone_bricks"),
        WOOD("screen.blockreskinner.category.wood"),
        COPPER("screen.blockreskinner.category.copper"),
        NETHER_END("screen.blockreskinner.category.nether_end"),
        MODDED("screen.blockreskinner.category.modded");

        private final String translationKey;

        FilterCategory(String translationKey) {
            this.translationKey = translationKey;
        }

        String translationKey() {
            return translationKey;
        }

        boolean accepts(BlockState state) {
            Identifier id = Registries.BLOCK.getId(state.getBlock());
            String path = id.getPath().toLowerCase(Locale.ROOT);
            SkinCategory category = SkinQueries.category(state);
            return switch (this) {
                case ALL -> true;
                case FULL_BLOCKS -> category == SkinCategory.FULL_BLOCKS;
                case LOGS_AND_PILLARS -> category == SkinCategory.LOGS_AND_PILLARS;
                case TRANSPARENT -> category == SkinCategory.TRANSPARENT;
                case LEAVES -> category == SkinCategory.LEAVES;
                case CUTOUT -> category == SkinCategory.CUTOUT;
                case ORES -> path.endsWith("_ore") || path.equals("ancient_debris")
                        || (path.startsWith("raw_") && path.endsWith("_block"));
                case STONE_BRICKS -> !path.endsWith("_ore")
                        && (path.contains("stone") || path.contains("deepslate") || path.contains("granite")
                        || path.contains("diorite") || path.contains("andesite") || path.contains("tuff")
                        || path.contains("calcite") || path.contains("brick") || path.contains("cobble")
                        || path.contains("mud") || path.contains("prismarine"));
                case WOOD -> path.contains("planks") || path.contains("bookshelf") || path.contains("_wood")
                        || path.contains("_hyphae") || path.contains("bamboo");
                case COPPER -> path.contains("copper");
                case NETHER_END -> path.contains("nether") || path.contains("blackstone") || path.contains("basalt")
                        || path.contains("soul_") || path.contains("warped_") || path.contains("crimson_")
                        || path.startsWith("end_") || path.contains("purpur") || path.contains("quartz")
                        || path.contains("glowstone") || path.contains("shroomlight") || path.contains("magma")
                        || path.contains("obsidian");
                case MODDED -> !id.getNamespace().equals("minecraft");
            };
        }
    }
}
