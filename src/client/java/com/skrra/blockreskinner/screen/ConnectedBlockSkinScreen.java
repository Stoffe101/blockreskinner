package com.skrra.blockreskinner.screen;

import com.skrra.blockreskinner.networking.payload.ApplyConnectedSkinPayload;
import com.skrra.blockreskinner.screen.layout.ReskinLayout;
import com.skrra.blockreskinner.screen.layout.ReskinLayoutProfile;
import com.skrra.blockreskinner.screen.widget.ConnectionEditorWidget;
import com.skrra.blockreskinner.screen.widget.ModernButtonWidget;
import com.skrra.blockreskinner.screen.widget.ModernUi;
import com.skrra.blockreskinner.skin.ConnectionOverride;
import com.skrra.blockreskinner.skin.SkinQueries;
import com.skrra.blockreskinner.util.BlockStateUtil;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Connected block (fence/wall/pane) skin picker. Shares the virtual-canvas
 * chrome with BlockSkinScreen; the category sidebar is replaced by a
 * right-side details panel holding the selected material and the four
 * visual-connection override rows.
 */
public class ConnectedBlockSkinScreen extends BlockSkinScreen {
    private static final int ROW_PITCH = 26;
    private static final int ROW_HEIGHT = 24;

    private final boolean[] currentConnections;
    private final ConnectionOverride[] overrides = {
            ConnectionOverride.AUTO, ConnectionOverride.AUTO, ConnectionOverride.AUTO, ConnectionOverride.AUTO
    };
    private final List<ModernButtonWidget> overrideButtons = new ArrayList<>(4);

    public ConnectedBlockSkinScreen(BlockPos pos, boolean northConnected, boolean eastConnected, boolean southConnected, boolean westConnected) {
        super(pos, connectedStates(pos), Text.translatable("screen.blockreskinner.connected.title"));
        this.currentConnections = new boolean[]{northConnected, eastConnected, southConnected, westConnected};
        this.searchPlaceholder = Text.translatable("screen.blockreskinner.search.materials");
        this.headerTitle = Text.translatable("screen.blockreskinner.connected.title");
        this.headerSubtitle = Text.translatable("screen.blockreskinner.connected.subtitle");
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
    protected void rebuildUi() {
        super.rebuildUi();
        overrideButtons.clear();
        ReskinLayout.Rect panel = layout.sidePanel;
        int buttonW = overrideButtonWidth();
        int buttonX = panel.right() - 10 - buttonW;
        int top = rowsTop();
        for (int i = 0; i < 4; i++) {
            int index = i;
            int buttonY = top + i * ROW_PITCH + (ROW_HEIGHT - 20) / 2;
            ModernButtonWidget[] holder = new ModernButtonWidget[1];
            holder[0] = new ModernButtonWidget(buttonX, buttonY, buttonW, 20,
                    overrideText(overrides[index]), ModernButtonWidget.Style.SECONDARY, () -> {
                        overrides[index] = ConnectionEditorWidget.next(overrides[index]);
                        holder[0].setLabel(overrideText(overrides[index]));
                    });
            ModernButtonWidget button = holder[0];
            overrideButtons.add(button);
        }
    }

    @Override
    protected void apply() {
        if (selected != null) {
            ClientPlayNetworking.send(new ApplyConnectedSkinPayload(pos, BlockStateUtil.toString(selected),
                    overrides[0], overrides[1], overrides[2], overrides[3]));
            close();
        }
    }

    @Override
    protected void renderDetailsPanel(DrawContext context, int mouseX, int mouseY) {
        ReskinLayout.Rect panel = layout.sidePanel;
        ModernUi.panel(context, panel.x(), panel.y(), panel.w(), panel.h());
        int textX = panel.x() + 10;
        context.drawTextWithShadow(textRenderer, Text.translatable("screen.blockreskinner.connected.material"),
                textX, panel.y() + 8, ModernUi.TEXT_MUTED);

        if (selected != null) {
            drawBlockPreview(context, selected, textX, panel.y() + 20, 36);
            int infoX = textX + 44;
            int infoW = panel.right() - 10 - infoX;
            context.drawTextWithShadow(textRenderer,
                    Text.literal(ModernUi.trim(textRenderer, selected.getBlock().getName().getString(), infoW)),
                    infoX, panel.y() + 25, ModernUi.TEXT_PRIMARY);
            context.drawTextWithShadow(textRenderer,
                    Text.literal(ModernUi.trim(textRenderer, BlockStateUtil.toString(selected), infoW)),
                    infoX, panel.y() + 38, ModernUi.TEXT_DISABLED);
        } else {
            context.drawTextWithShadow(textRenderer, Text.translatable("screen.blockreskinner.no_selection"),
                    textX, panel.y() + 25, ModernUi.TEXT_DISABLED);
        }

        int ruleY = panel.y() + 62;
        ModernUi.rule(context, textX, ruleY, panel.w() - 20, 48);
        context.drawTextWithShadow(textRenderer, Text.translatable("screen.blockreskinner.connection_controls"),
                textX, ruleY + 8, ModernUi.TEXT_PRIMARY);

        int descY = ruleY + 20;
        for (OrderedText line : descriptionLines()) {
            context.drawText(textRenderer, line, textX, descY, ModernUi.TEXT_MUTED, true);
            descY += 9;
        }

        int top = rowsTop();
        Text[] directions = {
                Text.translatable("screen.blockreskinner.direction.north"),
                Text.translatable("screen.blockreskinner.direction.east"),
                Text.translatable("screen.blockreskinner.direction.south"),
                Text.translatable("screen.blockreskinner.direction.west")
        };
        int buttonW = overrideButtonWidth();
        for (int i = 0; i < 4; i++) {
            int rowY = top + i * ROW_PITCH;
            int rowW = panel.w() - 20;
            // alternating subtle row backgrounds
            context.fill(textX, rowY, textX + rowW, rowY + ROW_HEIGHT,
                    i % 2 == 0 ? ModernUi.withAlpha(ModernUi.CARD_BACKGROUND, 0x66) : ModernUi.withAlpha(ModernUi.CARD_BACKGROUND, 0x3A));
            context.drawTextWithShadow(textRenderer, directions[i], textX + 6, rowY + (ROW_HEIGHT - 8) / 2, ModernUi.TEXT_PRIMARY);

            boolean connected = currentConnections[i];
            Text status = connected
                    ? Text.translatable("screen.blockreskinner.connected.current.connected.short")
                    : Text.translatable("screen.blockreskinner.connected.current.disconnected.short");
            int statusColor = connected ? ModernUi.SUCCESS : ModernUi.TEXT_MUTED;
            int statusX = textX + 46;
            int statusW = rowW - buttonW - 58;
            context.drawTextWithShadow(textRenderer,
                    Text.literal(ModernUi.trim(textRenderer, status.getString(), Math.max(30, statusW))),
                    statusX, rowY + (ROW_HEIGHT - 8) / 2, statusColor);
        }

        for (ModernButtonWidget button : overrideButtons) {
            button.render(context, textRenderer, mouseX, mouseY);
        }
    }

    @Override
    protected boolean handleExtraClick(double vx, double vy) {
        for (ModernButtonWidget button : overrideButtons) {
            if (button.mouseClicked(vx, vy)) {
                return true;
            }
        }
        return false;
    }

    private List<OrderedText> descriptionLines() {
        return textRenderer.wrapLines(Text.translatable("screen.blockreskinner.connected.description"), layout.sidePanel.w() - 20);
    }

    private int overrideButtonWidth() {
        return Math.min(92, Math.max(68, layout.sidePanel.w() - 170));
    }

    /** First connection row Y; below the wrapped description, clamped into the panel. */
    private int rowsTop() {
        ReskinLayout.Rect panel = layout.sidePanel;
        int wanted = panel.y() + 62 + 20 + descriptionLines().size() * 9 + 6;
        int latest = panel.bottom() - 6 - 4 * ROW_PITCH + (ROW_PITCH - ROW_HEIGHT);
        return Math.min(wanted, latest);
    }

    private Text overrideText(ConnectionOverride value) {
        return switch (value) {
            case AUTO -> Text.translatable("screen.blockreskinner.connected.override.auto");
            case FORCE_ON -> Text.translatable("screen.blockreskinner.connected.override.connected");
            case FORCE_OFF -> Text.translatable("screen.blockreskinner.connected.override.disconnected");
        };
    }
}
