package com.skrra.blockreskinner.screen;

import com.skrra.blockreskinner.networking.payload.ApplyPlayerHeadSkinPayload;
import com.skrra.blockreskinner.networking.payload.ApplySimpleSkinPayload;
import com.skrra.blockreskinner.networking.payload.ClearSkinPayload;
import com.skrra.blockreskinner.render.BlockPreviewGuiElementRenderState;
import com.skrra.blockreskinner.render.head.PlayerHeadProfiles;
import com.skrra.blockreskinner.screen.layout.ReskinLayout;
import com.skrra.blockreskinner.screen.layout.ReskinLayoutProfile;
import com.skrra.blockreskinner.screen.widget.ModernButtonWidget;
import com.skrra.blockreskinner.screen.widget.ModernUi;
import com.skrra.blockreskinner.skin.ClientSkinCache;
import com.skrra.blockreskinner.skin.PlayerHeadSkinData;
import com.skrra.blockreskinner.skin.SimpleSkinData;
import com.skrra.blockreskinner.skin.SkinCategory;
import com.skrra.blockreskinner.skin.SkinQueries;
import com.skrra.blockreskinner.util.BlockStateUtil;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.AbstractSkullBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SkullBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationPropertyHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Block skin picker on an Atmosphere+-style virtual canvas: layout math runs
 * against ReskinLayoutProfile's clamped-scale virtual size, rendering applies
 * the profile's renderScale matrix, and every input handler divides mouse
 * coordinates by the same renderScale.
 */
public class BlockSkinScreen extends Screen {
    protected final BlockPos pos;
    protected final List<BlockState> allStates;
    protected final List<BlockState> filteredStates = new ArrayList<>();
    protected final BlockState realState;
    protected BlockState currentVisualState;
    protected BlockState selected;
    protected int scrollRows;
    protected int categoryScroll;
    protected FilterCategory activeCategory = FilterCategory.ALL;
    protected String searchQuery = "";
    protected boolean searchFocused;
    protected Text searchPlaceholder = Text.translatable("screen.blockreskinner.search.placeholder");
    protected Text headerTitle = Text.translatable("screen.blockreskinner.header");
    protected Text headerSubtitle = Text.translatable("screen.blockreskinner.simple.description");

    protected ReskinLayout layout;
    private String layoutKey = "";
    protected final List<ModernButtonWidget> footerButtons = new ArrayList<>();
    protected ModernButtonWidget applyButton;
    protected ModernButtonWidget clearButton;
    private BlockState hoveredState;
    private boolean hoveringSelectedId;
    private boolean tallDetails;

    // Player head editor state (Skulls & Heads -> Player Head entry).
    private static final Direction[] HEAD_ROTATIONS = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    protected String playerName = "";
    protected boolean playerNameFocused;
    private int playerHeadRotationIndex;
    private String initialPlayerName = "";
    private int initialPlayerHeadRotation = -1;
    private boolean currentIsPlayerHead;
    private String resolveRequestedName;
    private ModernButtonWidget rotationButton;

    public BlockSkinScreen(BlockPos pos) {
        this(pos, SkinQueries.simpleVisualStates(), Text.translatable("screen.blockreskinner.simple.title"));
    }

    protected BlockSkinScreen(BlockPos pos, List<BlockState> allStates, Text title) {
        super(title);
        this.pos = pos;
        this.allStates = allStates;
        this.filteredStates.addAll(allStates);
        this.realState = currentRealState(pos);
        this.currentVisualState = currentSimpleVisual(pos);
        this.selected = findMatchingState(currentVisualState);
        this.rotationButton = new ModernButtonWidget(0, 0, 110, 18, Text.empty(), ModernButtonWidget.Style.SECONDARY, () -> {
            playerHeadRotationIndex = (playerHeadRotationIndex + 1) % HEAD_ROTATIONS.length;
            updateApplyButton();
        });
        if (ClientSkinCache.get(pos) instanceof PlayerHeadSkinData playerHead) {
            currentIsPlayerHead = true;
            playerName = playerHead.playerName();
            initialPlayerName = playerHead.playerName();
            initialPlayerHeadRotation = playerHead.rotation();
            for (int i = 0; i < HEAD_ROTATIONS.length; i++) {
                if (RotationPropertyHelper.fromDirection(HEAD_ROTATIONS[i]) == playerHead.rotation()) {
                    playerHeadRotationIndex = i;
                }
            }
            currentVisualState = Blocks.PLAYER_HEAD.getDefaultState();
            selected = findMatchingState(currentVisualState);
            resolveRequestedName = playerHead.playerName().trim();
        }
    }

    protected boolean isPlayerHeadSelected() {
        return selected != null && selected.getBlock() == Blocks.PLAYER_HEAD;
    }

    protected int playerHeadRotationValue() {
        return RotationPropertyHelper.fromDirection(HEAD_ROTATIONS[playerHeadRotationIndex]);
    }

    private Text playerHeadRotationLabel() {
        return Text.translatable("screen.blockreskinner.rotation",
                Text.translatable("screen.blockreskinner.facing." + HEAD_ROTATIONS[playerHeadRotationIndex].asString()));
    }

    /** Heads get a taller details panel: bigger previews plus the player-head editor. */
    private boolean wantTallDetails() {
        return activeCategory == FilterCategory.HEADS
                || currentIsPlayerHead
                || (selected != null && selected.getBlock() instanceof AbstractSkullBlock);
    }

    @Override
    protected void init() {
        layoutKey = "";
        ensureLayout();
    }

    protected void ensureLayout() {
        tallDetails = wantTallDetails();
        ReskinLayoutProfile profile = ReskinLayoutProfile.create(width, height);
        String key = profile.key() + (tallDetails ? ":tall" : "");
        if (!key.equals(layoutKey)) {
            layoutKey = key;
            layout = buildLayout(profile);
            rebuildUi();
            clampScroll();
        }
    }

    protected ReskinLayout buildLayout(ReskinLayoutProfile profile) {
        return ReskinLayout.forSimple(profile, tallDetails);
    }

    protected void rebuildUi() {
        footerButtons.clear();
        ReskinLayoutProfile profile = layout.profile;
        int pad = profile.padding();
        int buttonH = profile.buttonHeight();
        int buttonW = Math.min(120, Math.max(90, (layout.window.w() - pad * 2 - 16) / 4));
        int buttonY = layout.footer.y() + (layout.footer.h() - buttonH) / 2;
        applyButton = new ModernButtonWidget(layout.window.x() + pad, buttonY, buttonW, buttonH,
                Text.translatable("screen.blockreskinner.apply"), ModernButtonWidget.Style.PRIMARY, this::apply);
        footerButtons.add(applyButton);
        clearButton = new ModernButtonWidget(layout.window.x() + pad + buttonW + 8, buttonY, buttonW, buttonH,
                Text.translatable("screen.blockreskinner.clear"), ModernButtonWidget.Style.SECONDARY, this::clear);
        footerButtons.add(clearButton);
        footerButtons.add(new ModernButtonWidget(layout.window.right() - pad - buttonW, buttonY, buttonW, buttonH,
                Text.translatable("gui.cancel"), ModernButtonWidget.Style.NEUTRAL, this::close));
        updateApplyButton();
    }

    protected void clampScroll() {
        scrollRows = Math.max(0, Math.min(scrollRows, maxScrollRows()));
        categoryScroll = Math.max(0, Math.min(categoryScroll, maxCategoryScroll()));
    }

    protected void apply() {
        if (isPlayerHeadSelected()) {
            String name = playerName.trim();
            if (SkinQueries.isValidPlayerName(name)) {
                ClientPlayNetworking.send(new ApplyPlayerHeadSkinPayload(pos, name, playerHeadRotationValue()));
                close();
            }
            return;
        }
        if (selected != null) {
            ClientPlayNetworking.send(new ApplySimpleSkinPayload(pos, BlockStateUtil.toString(selected)));
            close();
        }
    }

    protected void clear() {
        ClientPlayNetworking.send(new ClearSkinPayload(pos));
        close();
    }

    protected void refilter() {
        String needle = searchQuery.toLowerCase(Locale.ROOT).trim();
        filteredStates.clear();
        for (BlockState state : allStates) {
            if (activeCategory.accepts(state) && matches(state, needle)) {
                filteredStates.add(state);
            }
        }
        scrollRows = 0;
        updateApplyButton();
    }

    private boolean matches(BlockState state, String needle) {
        if (needle.isEmpty()) {
            return true;
        }
        String id = Registries.BLOCK.getId(state.getBlock()).toString();
        SkinCategory skinCategory = SkinQueries.category(state);
        String category = Text.translatable(skinCategory.translationKey()).getString();
        SkinQueries.TextKey variantLabel = SkinQueries.variantLabelKey(state);
        String variant = variantLabel == null ? "" : Text.translatable(variantLabel.labelKey()).getString() + " " + variantLabel.debugText();
        // Extra alias terms so e.g. "fern", "plant" or "crystal" match the category.
        String aliases = switch (skinCategory) {
            case LEAVES -> "plants plant leaves leaf fern flower grass sapling mushroom roots";
            case CRYSTALS -> "crystal crystals amethyst cluster bud buds";
            case HEADS -> "skull skulls head heads mob skeleton wither zombie creeper piglin dragon player";
            default -> "";
        };
        return id.toLowerCase(Locale.ROOT).contains(needle)
                || state.getBlock().getName().getString().toLowerCase(Locale.ROOT).contains(needle)
                || category.toLowerCase(Locale.ROOT).contains(needle)
                || variant.toLowerCase(Locale.ROOT).contains(needle)
                || aliases.contains(needle);
    }

    protected void updateApplyButton() {
        if (applyButton != null) {
            if (isPlayerHeadSelected()) {
                String name = playerName.trim();
                boolean changed = !currentIsPlayerHead
                        || !name.equalsIgnoreCase(initialPlayerName.trim())
                        || playerHeadRotationValue() != initialPlayerHeadRotation;
                applyButton.setEnabled(SkinQueries.isValidPlayerName(name) && changed);
            } else {
                applyButton.setEnabled(selected != null && !statesEqual(selected, currentVisualState));
            }
        }
        if (clearButton != null) {
            clearButton.setEnabled(currentVisualState != null);
        }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.renderBackground(context, mouseX, mouseY, deltaTicks);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        ensureLayout();
        hoveredState = null;
        hoveringSelectedId = false;

        double renderScale = layout.profile.renderScale;
        int uiMouseX = Math.round((float) (mouseX / renderScale));
        int uiMouseY = Math.round((float) (mouseY / renderScale));

        context.getMatrices().pushMatrix();
        context.getMatrices().scale((float) renderScale);

        renderChrome(context);
        renderSidebar(context, uiMouseX, uiMouseY);
        renderGrid(context, uiMouseX, uiMouseY);
        renderDetailsPanel(context, uiMouseX, uiMouseY);
        renderFooter(context, uiMouseX, uiMouseY);
        renderTooltips(context, uiMouseX, uiMouseY);

        context.getMatrices().popMatrix();
        super.render(context, mouseX, mouseY, deltaTicks);
    }

    protected void renderChrome(DrawContext context) {
        ReskinLayout.Rect window = layout.window;
        ReskinLayoutProfile profile = layout.profile;
        int pad = profile.padding();
        ModernUi.window(context, window.x(), window.y(), window.w(), window.h());
        ModernUi.headerBand(context, window.x() + 1, window.y() + 1, window.w() - 2, profile.headerHeight());
        int titleY = window.y() + (profile.headerHeight() >= 44 ? 10 : 7);
        context.drawTextWithShadow(textRenderer, headerTitle, window.x() + pad, titleY, ModernUi.TEXT_PRIMARY);
        String subtitle = ModernUi.trim(textRenderer, headerSubtitle.getString(), window.w() - pad * 2);
        context.drawTextWithShadow(textRenderer, Text.literal(subtitle), window.x() + pad, titleY + 13, ModernUi.TEXT_MUTED);
        ModernUi.rule(context, window.x() + pad, window.y() + profile.headerHeight() - 1, window.w() - pad * 2, 90);

        ReskinLayout.Rect search = layout.search;
        ModernUi.searchBox(context, textRenderer, search.x(), search.y(), search.w(), search.h(),
                searchQuery, searchPlaceholder, searchFocused);
    }

    protected void renderSidebar(DrawContext context, int mouseX, int mouseY) {
        ReskinLayout.Rect sidebar = layout.sidebar;
        if (sidebar == null) {
            return;
        }
        ModernUi.panel(context, sidebar.x(), sidebar.y(), sidebar.w(), sidebar.h());
        int headerX = sidebar.x() + 6;
        int headerY = sidebar.y() + 5;
        int headerW = sidebar.w() - 12;
        int headerH = 18;
        context.fill(headerX + 1, headerY + 1, headerX + headerW + 1, headerY + headerH + 1, 0x22000000);
        context.fill(headerX, headerY, headerX + headerW, headerY + headerH, ModernUi.withAlpha(ModernUi.CARD_BACKGROUND, 0x88));
        context.fill(headerX, headerY, headerX + headerW, headerY + 1, 0x22FFFFFF);
        ModernUi.border(context, headerX, headerY, headerW, headerH, ModernUi.BORDER_SOFT);
        ModernUi.gradientHorizontal(context, headerX + 1, headerY, Math.min(54, headerW - 2), 1, ModernUi.ACCENT, ModernUi.ACCENT_ALT);
        context.drawTextWithShadow(textRenderer, Text.translatable("screen.blockreskinner.categories"),
                headerX + 7, headerY + 5, ModernUi.TEXT_MUTED);

        FilterCategory[] categories = FilterCategory.values();
        int rowH = layout.profile.categoryRowHeight();
        int listTop = categoryListTop();
        int visible = visibleCategoryRows();
        categoryScroll = Math.max(0, Math.min(categoryScroll, maxCategoryScroll()));

        context.enableScissor(sidebar.x() + 1, listTop, sidebar.right() - 1, listTop + visible * rowH);
        for (int i = categoryScroll; i < Math.min(categories.length, categoryScroll + visible); i++) {
            FilterCategory category = categories[i];
            int y = listTop + (i - categoryScroll) * rowH;
            boolean selectedCategory = activeCategory == category;
            boolean hovered = ModernUi.hovered(mouseX, mouseY, sidebar.x() + 4, y, sidebar.w() - 8, rowH - 2);
            if (selectedCategory) {
                context.fill(sidebar.x() + 4, y, sidebar.right() - 4, y + rowH - 2, ModernUi.ACCENT_SOFT);
                ModernUi.gradientVertical(context, sidebar.x() + 4, y, 2, rowH - 2, ModernUi.ACCENT, ModernUi.ACCENT_ALT);
            } else if (hovered) {
                context.fill(sidebar.x() + 4, y, sidebar.right() - 4, y + rowH - 2, ModernUi.withAlpha(ModernUi.CARD_HOVER_BACKGROUND, 0x66));
            }
            int color = selectedCategory ? ModernUi.TEXT_PRIMARY : hovered ? ModernUi.TEXT_PRIMARY : ModernUi.TEXT_MUTED;
            String label = ModernUi.trim(textRenderer, Text.translatable(category.translationKey()).getString(), sidebar.w() - 24);
            context.drawTextWithShadow(textRenderer, Text.literal(label), sidebar.x() + 11, y + (rowH - 9) / 2, color);
        }
        context.disableScissor();

        ModernUi.scrollbar(context, sidebar.right() - 5, listTop, visible * rowH,
                categories.length, visible, categoryScroll);
    }

    protected void renderGrid(DrawContext context, int mouseX, int mouseY) {
        ReskinLayout.Rect grid = layout.grid;
        ModernUi.panel(context, grid.x(), grid.y(), grid.w(), grid.h());
        int columns = layout.columns;
        int size = layout.cardSize;
        int gap = layout.cardGap;
        int step = size + gap;
        int gx = layout.gridInnerX();
        int gy = layout.gridInnerY();

        boolean mouseInGrid = grid.contains(mouseX, mouseY);
        context.enableScissor(grid.x() + 1, grid.y() + 1, grid.right() - 1, grid.bottom() - 1);
        int start = scrollRows * columns;
        int visibleRows = layout.gridInnerHeight() / step + 2;
        int end = Math.min(filteredStates.size(), start + visibleRows * columns);
        for (int i = start; i < end; i++) {
            BlockState state = filteredStates.get(i);
            int local = i - start;
            int x = gx + (local % columns) * step;
            int y = gy + (local / columns) * step;
            if (y > grid.bottom()) {
                continue;
            }
            boolean hovered = mouseInGrid && ModernUi.hovered(mouseX, mouseY, x, y, size, size);
            if (hovered) {
                hoveredState = state;
            }
            renderCard(context, state, x, y, size, hovered, statesEqual(state, selected));
        }
        context.disableScissor();

        int totalRows = (filteredStates.size() + columns - 1) / columns;
        ModernUi.scrollbar(context, grid.right() - 5, grid.y() + 4, grid.h() - 8,
                totalRows, layout.visibleGridRows(), scrollRows);

        if (filteredStates.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.blockreskinner.no_results"),
                    grid.x() + grid.w() / 2, grid.y() + grid.h() / 2 - 5, ModernUi.TEXT_MUTED);
        }
    }

    protected void renderCard(DrawContext context, BlockState state, int x, int y, int size, boolean hovered, boolean selectedState) {
        ModernUi.card(context, x, y, size, size, hovered, selectedState);
        int preview = size - 20;
        drawBlockPreview(context, state, x + (size - preview) / 2, y + 3, preview);

        SkinQueries.TextKey variantLabel = SkinQueries.variantLabelKey(state);
        String label;
        int color;
        if (SkinQueries.category(state) == SkinCategory.HEADS) {
            // Heads: the card names the head type; direction shows as a small
            // corner chip (full detail lives in the tooltip + details panel).
            label = state.getBlock().getName().getString();
            color = selectedState ? ModernUi.TEXT_PRIMARY : ModernUi.TEXT_MUTED;
            if (variantLabel != null) {
                String direction = Text.translatable(variantLabel.labelKey()).getString();
                if (!direction.isEmpty()) {
                    context.drawTextWithShadow(textRenderer, Text.literal(direction.substring(0, 1)),
                            x + size - 9, y + 4, ModernUi.WARNING);
                }
            }
        } else if (variantLabel != null) {
            label = Text.translatable(variantLabel.labelKey()).getString();
            color = ModernUi.WARNING;
        } else {
            label = state.getBlock().getName().getString();
            color = selectedState ? ModernUi.TEXT_PRIMARY : ModernUi.TEXT_MUTED;
        }
        label = ModernUi.trim(textRenderer, label, size - 8);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(label), x + size / 2, y + size - 12, color);
    }

    /**
     * Queues a 3D block preview special element. Special GUI elements draw at
     * raw screen coordinates (their pose ignores the virtual-canvas matrix), so
     * virtual coordinates are converted to real ones here; the scissor rect on
     * the stack is already stored transformed, i.e. in real coordinates.
     */
    protected void drawBlockPreview(DrawContext context, BlockState state, int x, int y, int size) {
        drawBlockPreview(context, state, null, x, y, size);
    }

    protected void drawBlockPreview(DrawContext context, BlockState state, String previewPlayerName, int x, int y, int size) {
        double renderScale = layout.profile.renderScale;
        int rx = (int) Math.round(x * renderScale);
        int ry = (int) Math.round(y * renderScale);
        int rsize = (int) Math.round(size * renderScale);
        context.state.addSpecialElement(new BlockPreviewGuiElementRenderState(
                state, previewPlayerName, rx, ry, rx + rsize, ry + rsize, rsize, context.scissorStack.peekLast()));
    }

    protected int detailsPreviewCardW(ReskinLayout.Rect panel) {
        return tallDetails
                ? Math.min(84, Math.max(54, (panel.w() - 40) / 6))
                : Math.min(54, Math.max(44, (panel.w() - 40) / 7));
    }

    /** Right editor column X, mirrored by playerNameFieldRect and the renderer. */
    private int detailsRightColumnX(ReskinLayout.Rect panel) {
        int textX = panel.x() + 10;
        int cardW = detailsPreviewCardW(panel);
        int beforeX = panel.right() - cardW - 8 - cardW - 6;
        int textW = beforeX - textX - 10;
        int leftW = Math.max(150, textW / 2 - 8);
        return textX + leftW + 12;
    }

    protected ReskinLayout.Rect playerNameFieldRect() {
        ReskinLayout.Rect panel = layout.selected;
        int rightX = detailsRightColumnX(panel);
        int cardW = detailsPreviewCardW(panel);
        int beforeX = panel.right() - cardW - 8 - cardW - 6;
        int w = Math.max(90, Math.min(150, beforeX - rightX - 10));
        return new ReskinLayout.Rect(rightX, panel.y() + 20, w, 18);
    }

    protected void renderDetailsPanel(DrawContext context, int mouseX, int mouseY) {
        ReskinLayout.Rect panel = layout.selected;
        if (panel == null) {
            return;
        }
        ModernUi.panel(context, panel.x(), panel.y(), panel.w(), panel.h());
        int textX = panel.x() + 10;
        int y = panel.y() + 6;
        int previewCardW = detailsPreviewCardW(panel);
        int previewCardH = panel.h() - 12;
        int afterPreviewX = panel.right() - previewCardW - 8;
        int beforePreviewX = afterPreviewX - previewCardW - 6;
        int textW = beforePreviewX - textX - 10;
        int leftW = Math.max(150, textW / 2 - 8);
        int rightX = textX + leftW + 12;
        int rightW = Math.max(80, beforePreviewX - rightX - 10);

        // Before = how the block looks right now (current skin if applied, else the real block);
        // After = the pending selection, or a "-" placeholder while nothing is picked.
        BlockState beforeState = currentVisualState != null ? currentVisualState : realState;
        String beforePlayer = currentIsPlayerHead ? initialPlayerName : null;
        BlockState afterState = selected;
        String afterPlayer = null;
        if (isPlayerHeadSelected()) {
            afterState = selected.contains(SkullBlock.ROTATION)
                    ? selected.with(SkullBlock.ROTATION, playerHeadRotationValue())
                    : selected;
            afterPlayer = SkinQueries.isValidPlayerName(playerName.trim()) ? playerName.trim() : null;
        }
        renderPreviewCard(context, Text.translatable("screen.blockreskinner.preview.before"), beforeState, beforePlayer,
                beforePreviewX, panel.y() + 6, previewCardW, previewCardH);
        renderPreviewCard(context, Text.translatable("screen.blockreskinner.preview.after"), afterState, afterPlayer,
                afterPreviewX, panel.y() + 6, previewCardW, previewCardH);

        context.drawTextWithShadow(textRenderer, Text.translatable("screen.blockreskinner.editing"), textX, y, ModernUi.TEXT_MUTED);
        drawStateLine(context, Text.translatable("screen.blockreskinner.real_block"), realState, textX, y + 12, leftW, ModernUi.TEXT_PRIMARY);
        drawIdLine(context, realState, textX, y + 24, leftW);
        if (currentIsPlayerHead) {
            String current = Text.translatable("screen.blockreskinner.current_visual_skin").getString()
                    + ": " + Blocks.PLAYER_HEAD.getName().getString() + " (" + initialPlayerName + ")";
            context.drawTextWithShadow(textRenderer, Text.literal(ModernUi.trim(textRenderer, current, leftW)),
                    textX, y + 38, ModernUi.TEXT_MUTED);
        } else {
            drawStateLine(context, Text.translatable("screen.blockreskinner.current_visual_skin"), currentVisualState, textX, y + 38, leftW, ModernUi.TEXT_MUTED);
        }

        context.drawTextWithShadow(textRenderer, Text.translatable("screen.blockreskinner.new_visual_skin"), rightX, y, ModernUi.TEXT_MUTED);
        if (isPlayerHeadSelected()) {
            renderPlayerHeadEditor(context, panel, rightX, mouseX, mouseY);
            return;
        }
        if (selected == null) {
            context.drawTextWithShadow(textRenderer, Text.translatable("screen.blockreskinner.no_new_skin_selected"),
                    rightX, y + 13, ModernUi.TEXT_DISABLED);
            context.drawTextWithShadow(textRenderer, Text.translatable("screen.blockreskinner.choose_skin_from_grid"),
                    rightX, y + 26, ModernUi.TEXT_MUTED);
            return;
        }

        String name = ModernUi.trim(textRenderer, selected.getBlock().getName().getString(), rightW);
        context.drawTextWithShadow(textRenderer, Text.literal(name), rightX, y + 12, ModernUi.TEXT_PRIMARY);
        SkinQueries.TextKey variantLabel = SkinQueries.variantLabelKey(selected);
        if (variantLabel != null) {
            String variant = Text.translatable(variantLabel.labelKey()).getString() + " (" + variantLabel.debugText() + ")";
            context.drawTextWithShadow(textRenderer,
                    Text.translatable("screen.blockreskinner.selected_variant", Text.literal(variant)),
                    rightX, y + 24, ModernUi.WARNING);
        } else {
            Text category = Text.translatable(SkinQueries.category(selected).translationKey());
            context.drawTextWithShadow(textRenderer,
                    Text.translatable("screen.blockreskinner.selected_category", category),
                    rightX, y + 24, ModernUi.TEXT_MUTED);
        }

        String fullId = BlockStateUtil.toString(selected);
        String shownId = ModernUi.trim(textRenderer, fullId, rightW);
        context.drawTextWithShadow(textRenderer, Text.literal(shownId), rightX, y + 36, ModernUi.TEXT_DISABLED);
        if (!shownId.equals(fullId) && ModernUi.hovered(mouseX, mouseY, rightX, y + 34, rightW, 12)) {
            hoveringSelectedId = true;
        }
    }

    protected void renderPreviewCard(DrawContext context, Text label, BlockState state, int x, int y, int w, int h) {
        renderPreviewCard(context, label, state, null, x, y, w, h);
    }

    protected void renderPreviewCard(DrawContext context, Text label, BlockState state, String previewPlayerName, int x, int y, int w, int h) {
        context.fill(x, y, x + w, y + h, ModernUi.withAlpha(ModernUi.CARD_BACKGROUND, 0xAA));
        ModernUi.border(context, x, y, w, h, ModernUi.BORDER_SOFT);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal(ModernUi.trim(textRenderer, label.getString(), w - 6)), x + w / 2, y + 4, ModernUi.TEXT_MUTED);
        if (state != null) {
            int preview = Math.min(w - 10, h - 18);
            drawBlockPreview(context, state, previewPlayerName, x + (w - preview) / 2, y + h - preview - 5, preview);
        } else {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("-"), x + w / 2, y + h / 2 + 3, ModernUi.TEXT_DISABLED);
        }
    }

    private void renderPlayerHeadEditor(DrawContext context, ReskinLayout.Rect panel, int rightX, int mouseX, int mouseY) {
        ReskinLayout.Rect field = playerNameFieldRect();
        ModernUi.searchBox(context, textRenderer, field.x(), field.y(), field.w(), field.h(),
                playerName, Text.translatable("screen.blockreskinner.enter_player_name"), playerNameFocused);

        Text status;
        int statusColor;
        String name = playerName.trim();
        if (!SkinQueries.isValidPlayerName(name)) {
            status = Text.translatable("screen.blockreskinner.head_status.enter_name");
            statusColor = ModernUi.TEXT_DISABLED;
        } else if (resolveRequestedName == null || !resolveRequestedName.equalsIgnoreCase(name)) {
            status = Text.translatable("screen.blockreskinner.head_status.press_enter");
            statusColor = ModernUi.TEXT_MUTED;
        } else {
            switch (PlayerHeadProfiles.status(name)) {
                case RESOLVING -> {
                    status = Text.translatable("screen.blockreskinner.head_status.resolving");
                    statusColor = ModernUi.WARNING;
                }
                case RESOLVED -> {
                    status = Text.translatable("screen.blockreskinner.head_status.resolved");
                    statusColor = ModernUi.SUCCESS;
                }
                default -> {
                    status = Text.translatable("screen.blockreskinner.head_status.failed");
                    statusColor = 0xFFE07A8A;
                }
            }
        }
        int cardW = detailsPreviewCardW(panel);
        int beforeX = panel.right() - cardW - 8 - cardW - 6;
        int statusW = Math.max(60, beforeX - rightX - 10);
        context.drawTextWithShadow(textRenderer,
                Text.literal(ModernUi.trim(textRenderer, status.getString(), statusW)),
                rightX, field.bottom() + 5, statusColor);

        rotationButton.setPosition(rightX, field.bottom() + 17);
        rotationButton.setSize(Math.min(130, field.w()), 18);
        rotationButton.setLabel(playerHeadRotationLabel());
        rotationButton.render(context, textRenderer, mouseX, mouseY);
    }

    protected void renderFooter(DrawContext context, int mouseX, int mouseY) {
        ReskinLayout.Rect footer = layout.footer;
        context.fill(footer.x(), footer.y(), footer.right(), footer.y() + 1, ModernUi.BORDER);
        ModernUi.gradientHorizontal(context, footer.x(), footer.y(), Math.min(footer.w(), 90), 1, ModernUi.ACCENT, ModernUi.ACCENT_ALT);
        for (ModernButtonWidget button : footerButtons) {
            button.render(context, textRenderer, mouseX, mouseY);
        }
    }

    protected void renderTooltips(DrawContext context, int mouseX, int mouseY) {
        if (hoveredState != null) {
            List<Text> lines = new ArrayList<>();
            lines.add(hoveredState.getBlock().getName());
            lines.add(Text.literal(Registries.BLOCK.getId(hoveredState.getBlock()).toString()).formatted(Formatting.GRAY));
            SkinQueries.TextKey variantLabel = SkinQueries.variantLabelKey(hoveredState);
            if (variantLabel != null) {
                lines.add(Text.translatable("screen.blockreskinner.tooltip.variant", Text.translatable(variantLabel.labelKey())).formatted(Formatting.YELLOW));
                lines.add(Text.literal(variantLabel.debugText()).formatted(Formatting.GRAY));
            }
            lines.add(Text.translatable(SkinQueries.category(hoveredState).translationKey()).formatted(Formatting.DARK_AQUA));
            ModernUi.tooltip(context, textRenderer, lines, mouseX, mouseY, layout.profile.virtualWidth, layout.profile.virtualHeight);
        } else if (hoveringSelectedId && selected != null) {
            ModernUi.tooltip(context, textRenderer, List.of(Text.literal(BlockStateUtil.toString(selected))),
                    mouseX, mouseY, layout.profile.virtualWidth, layout.profile.virtualHeight);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        ensureLayout();
        double renderScale = layout.profile.renderScale;
        double vx = click.x() / renderScale;
        double vy = click.y() / renderScale;
        if (handleVirtualClick(vx, vy)) {
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    protected boolean handleVirtualClick(double vx, double vy) {
        for (ModernButtonWidget button : footerButtons) {
            if (button.mouseClicked(vx, vy)) {
                return true;
            }
        }
        if (handleExtraClick(vx, vy)) {
            return true;
        }
        if (isPlayerHeadSelected() && layout.selected != null) {
            if (rotationButton.mouseClicked(vx, vy)) {
                return true;
            }
            ReskinLayout.Rect field = playerNameFieldRect();
            if (field.contains(vx, vy)) {
                if (ModernUi.overSearchClear(vx, vy, field.x(), field.y(), field.w(), field.h(), playerName)) {
                    playerName = "";
                    resolveRequestedName = null;
                    updateApplyButton();
                }
                playerNameFocused = true;
                searchFocused = false;
                return true;
            }
        }
        if (playerNameFocused) {
            playerNameFocused = false;
            requestPlayerNameResolve();
        }
        ReskinLayout.Rect search = layout.search;
        if (search.contains(vx, vy)) {
            if (ModernUi.overSearchClear(vx, vy, search.x(), search.y(), search.w(), search.h(), searchQuery)) {
                searchQuery = "";
                refilter();
            }
            searchFocused = true;
            return true;
        }
        searchFocused = false;

        FilterCategory category = categoryAt(vx, vy);
        if (category != null) {
            activeCategory = category;
            refilter();
            return true;
        }
        BlockState clicked = stateAt(vx, vy);
        if (clicked != null) {
            selected = clicked;
            updateApplyButton();
            return true;
        }
        return layout.window.contains(vx, vy);
    }

    protected static BlockState currentRealState(BlockPos pos) {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.world == null ? null : client.world.getBlockState(pos);
    }

    protected BlockState currentSimpleVisual(BlockPos pos) {
        if (ClientSkinCache.get(pos) instanceof SimpleSkinData simple) {
            return simple.visualState();
        }
        return null;
    }

    protected BlockState findMatchingState(BlockState state) {
        if (state == null) {
            return null;
        }
        for (BlockState candidate : allStates) {
            if (statesEqual(candidate, state)) {
                return candidate;
            }
        }
        return null;
    }

    protected boolean statesEqual(BlockState left, BlockState right) {
        return left == right || (left != null && left.equals(right));
    }

    protected void drawStateLine(DrawContext context, Text label, BlockState state, int x, int y, int width, int valueColor) {
        String value = state == null ? Text.translatable("screen.blockreskinner.none").getString() : state.getBlock().getName().getString();
        String line = label.getString() + ": " + value;
        context.drawTextWithShadow(textRenderer, Text.literal(ModernUi.trim(textRenderer, line, width)), x, y, valueColor);
    }

    protected void drawIdLine(DrawContext context, BlockState state, int x, int y, int width) {
        if (state == null) {
            return;
        }
        String id = BlockStateUtil.toString(state);
        context.drawTextWithShadow(textRenderer, Text.literal(ModernUi.trim(textRenderer, id, width)), x, y, ModernUi.TEXT_DISABLED);
    }

    /** Subclass hook for additional virtual-coordinate click targets. */
    protected boolean handleExtraClick(double vx, double vy) {
        return false;
    }

    protected BlockState stateAt(double vx, double vy) {
        ReskinLayout.Rect grid = layout.grid;
        if (!grid.contains(vx, vy)) {
            return null;
        }
        int step = layout.cardSize + layout.cardGap;
        int col = ((int) vx - layout.gridInnerX()) / step;
        int row = ((int) vy - layout.gridInnerY()) / step;
        if (col < 0 || col >= layout.columns || row < 0) {
            return null;
        }
        int inCol = ((int) vx - layout.gridInnerX()) % step;
        int inRow = ((int) vy - layout.gridInnerY()) % step;
        if (inCol >= layout.cardSize || inRow >= layout.cardSize) {
            return null;
        }
        int index = (scrollRows + row) * layout.columns + col;
        return index >= 0 && index < filteredStates.size() ? filteredStates.get(index) : null;
    }

    protected FilterCategory categoryAt(double vx, double vy) {
        ReskinLayout.Rect sidebar = layout.sidebar;
        if (sidebar == null || !sidebar.contains(vx, vy)) {
            return null;
        }
        FilterCategory[] categories = FilterCategory.values();
        int rowH = layout.profile.categoryRowHeight();
        int listTop = categoryListTop();
        int visible = visibleCategoryRows();
        for (int i = categoryScroll; i < Math.min(categories.length, categoryScroll + visible); i++) {
            int y = listTop + (i - categoryScroll) * rowH;
            if (ModernUi.hovered(vx, vy, sidebar.x() + 4, y, sidebar.w() - 8, rowH - 2)) {
                return categories[i];
            }
        }
        return null;
    }

    protected int categoryListTop() {
        return layout.sidebar.y() + 29;
    }

    protected int visibleCategoryRows() {
        return Math.max(1, (layout.sidebar.bottom() - 4 - categoryListTop()) / layout.profile.categoryRowHeight());
    }

    protected int maxCategoryScroll() {
        if (layout == null || layout.sidebar == null) {
            return 0;
        }
        return Math.max(0, FilterCategory.values().length - visibleCategoryRows());
    }

    protected int maxScrollRows() {
        if (layout == null) {
            return 0;
        }
        int totalRows = (filteredStates.size() + layout.columns - 1) / layout.columns;
        return Math.max(0, totalRows - layout.visibleGridRows());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        ensureLayout();
        double renderScale = layout.profile.renderScale;
        double vx = mouseX / renderScale;
        double vy = mouseY / renderScale;
        if (layout.sidebar != null && layout.sidebar.contains(vx, vy)) {
            categoryScroll = Math.max(0, Math.min(maxCategoryScroll(), categoryScroll - (int) Math.signum(verticalAmount)));
            return true;
        }
        if (layout.grid.contains(vx, vy)) {
            scrollRows = Math.max(0, Math.min(maxScrollRows(), scrollRows - (int) Math.signum(verticalAmount)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    /** Kick off (memoized, async) profile resolution for the typed name. */
    protected void requestPlayerNameResolve() {
        String name = playerName.trim();
        if (SkinQueries.isValidPlayerName(name)) {
            resolveRequestedName = name;
        }
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (playerNameFocused) {
            if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
                playerNameFocused = false;
                requestPlayerNameResolve();
                return true;
            }
            if (input.key() == GLFW.GLFW_KEY_ENTER || input.key() == GLFW.GLFW_KEY_KP_ENTER) {
                requestPlayerNameResolve();
                return true;
            }
            if (input.key() == GLFW.GLFW_KEY_BACKSPACE) {
                if (!playerName.isEmpty()) {
                    playerName = playerName.substring(0, playerName.length() - 1);
                    updateApplyButton();
                }
                return true;
            }
        }
        if (searchFocused) {
            if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
                searchFocused = false;
                return true;
            }
            if (input.key() == GLFW.GLFW_KEY_BACKSPACE) {
                if (!searchQuery.isEmpty()) {
                    searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                    refilter();
                }
                return true;
            }
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (playerNameFocused && input.isValidChar() && playerName.length() < 16) {
            String typed = input.asString();
            char c = typed.isEmpty() ? ' ' : typed.charAt(0);
            if (c == '_' || (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                playerName += typed;
                updateApplyButton();
            }
            return true;
        }
        if (searchFocused && input.isValidChar() && searchQuery.length() < 48) {
            searchQuery += input.asString();
            refilter();
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    protected enum FilterCategory {
        ALL("screen.blockreskinner.category.all"),
        FULL_BLOCKS("screen.blockreskinner.category.full_blocks"),
        LOGS_AND_PILLARS("screen.blockreskinner.category.logs_and_pillars"),
        TRANSPARENT("screen.blockreskinner.category.transparent"),
        LEAVES("screen.blockreskinner.category.leaves"),
        CRYSTALS("screen.blockreskinner.category.crystals"),
        HEADS("screen.blockreskinner.category.heads"),
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
                case CRYSTALS -> category == SkinCategory.CRYSTALS;
                case HEADS -> category == SkinCategory.HEADS;
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
