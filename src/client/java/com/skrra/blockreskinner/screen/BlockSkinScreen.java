package com.skrra.blockreskinner.screen;

import com.skrra.blockreskinner.networking.payload.ApplySimpleSkinPayload;
import com.skrra.blockreskinner.networking.payload.ClearSkinPayload;
import com.skrra.blockreskinner.screen.widget.BlockGridWidget;
import com.skrra.blockreskinner.screen.widget.BlockSearchWidget;
import com.skrra.blockreskinner.skin.SkinQueries;
import com.skrra.blockreskinner.util.BlockStateUtil;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BlockSkinScreen extends Screen {
    protected final BlockPos pos;
    protected final List<BlockState> allStates;
    protected final List<BlockState> filteredStates = new ArrayList<>();
    protected BlockSearchWidget search;
    protected ButtonWidget applyButton;
    protected BlockState selected;
    protected int scrollRows;

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
        int panelWidth = Math.min(360, width - 32);
        int left = (width - panelWidth) / 2;
        search = new BlockSearchWidget(textRenderer, left, 28, panelWidth, 20);
        search.setChangedListener(this::filter);
        addDrawableChild(search);

        applyButton = ButtonWidget.builder(Text.translatable("screen.blockreskinner.apply"), button -> apply())
                .dimensions(left, height - 30, 92, 20)
                .build();
        addDrawableChild(applyButton);
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.blockreskinner.clear"), button -> clear())
                .dimensions(left + 100, height - 30, 92, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), button -> close())
                .dimensions(left + panelWidth - 92, height - 30, 92, 20)
                .build());
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
            String id = net.minecraft.registry.Registries.BLOCK.getId(state.getBlock()).toString();
            if (needle.isEmpty() || id.toLowerCase(Locale.ROOT).contains(needle) || state.getBlock().getName().getString().toLowerCase(Locale.ROOT).contains(needle)) {
                filteredStates.add(state);
            }
        }
        scrollRows = 0;
        if (!filteredStates.contains(selected)) {
            selected = filteredStates.isEmpty() ? null : filteredStates.get(0);
        }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.renderBackground(context, mouseX, mouseY, deltaTicks);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, 0xFFFFFF);
        renderGrid(context, mouseX, mouseY);
        renderSelected(context);
    }

    protected void renderGrid(DrawContext context, int mouseX, int mouseY) {
        int left = gridLeft();
        int top = gridTop();
        int gridWidth = gridWidth();
        int gridHeight = gridBottom() - top;
        int columns = BlockGridWidget.columns(gridWidth);
        context.fill(left - 2, top - 2, left + gridWidth + 2, top + gridHeight + 2, 0xAA101014);
        context.enableScissor(left, top, left + gridWidth, top + gridHeight);

        int start = scrollRows * columns;
        int visibleRows = Math.max(1, gridHeight / BlockGridWidget.TILE + 1);
        int end = Math.min(filteredStates.size(), start + visibleRows * columns);
        for (int i = start; i < end; i++) {
            BlockState state = filteredStates.get(i);
            int local = i - start;
            int x = left + (local % columns) * BlockGridWidget.TILE;
            int y = top + (local / columns) * BlockGridWidget.TILE;
            boolean hovered = mouseX >= x && mouseX < x + BlockGridWidget.TILE && mouseY >= y && mouseY < y + BlockGridWidget.TILE;
            int color = state == selected ? 0xFF5CA8FF : hovered ? 0xFFAAAAAA : 0xFF333333;
            context.fill(x, y, x + 24, y + 24, color);
            context.fill(x + 1, y + 1, x + 23, y + 23, 0xFF202025);
            context.drawItem(new ItemStack(state.getBlock().asItem()), x + BlockGridWidget.ICON_OFFSET, y + BlockGridWidget.ICON_OFFSET);
        }

        context.disableScissor();
    }

    protected void renderSelected(DrawContext context) {
        if (selected != null) {
            String label = selected.getBlock().getName().getString();
            String state = BlockStateUtil.toString(selected);
            context.drawTextWithShadow(textRenderer, Text.literal(label), gridLeft(), height - 54, 0xFFFFFF);
            context.drawTextWithShadow(textRenderer, Text.literal(state), gridLeft(), height - 43, 0xBBBBBB);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true;
        }
        BlockState clicked = stateAt(click.x(), click.y());
        if (clicked != null) {
            selected = clicked;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX >= gridLeft() && mouseX <= gridLeft() + gridWidth() && mouseY >= gridTop() && mouseY <= gridBottom()) {
            int columns = BlockGridWidget.columns(gridWidth());
            int rows = Math.max(0, (filteredStates.size() + columns - 1) / columns - (gridBottom() - gridTop()) / BlockGridWidget.TILE);
            scrollRows = Math.max(0, Math.min(rows, scrollRows - (int) Math.signum(verticalAmount)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private BlockState stateAt(double mouseX, double mouseY) {
        if (mouseX < gridLeft() || mouseX >= gridLeft() + gridWidth() || mouseY < gridTop() || mouseY >= gridBottom()) {
            return null;
        }
        int columns = BlockGridWidget.columns(gridWidth());
        int col = ((int) mouseX - gridLeft()) / BlockGridWidget.TILE;
        int row = ((int) mouseY - gridTop()) / BlockGridWidget.TILE;
        int index = (scrollRows + row) * columns + col;
        return index >= 0 && index < filteredStates.size() ? filteredStates.get(index) : null;
    }

    protected int gridLeft() {
        return (width - gridWidth()) / 2;
    }

    protected int gridTop() {
        return 58;
    }

    protected int gridWidth() {
        return Math.min(360, width - 32);
    }

    protected int gridBottom() {
        return height - 60;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
