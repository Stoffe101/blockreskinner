package com.skrra.blockreskinner.screen.widget;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Lightweight modern button drawn inside the virtual canvas. Not a vanilla
 * ClickableWidget on purpose: vanilla children render outside the scaled
 * matrix and would ignore the virtual coordinate system.
 */
public class ModernButtonWidget {
    public enum Style {
        PRIMARY,
        SECONDARY,
        NEUTRAL
    }

    private int x;
    private int y;
    private int width;
    private int height;
    private Text label;
    private final Style style;
    private final Runnable onPress;
    private boolean enabled = true;

    public ModernButtonWidget(int x, int y, int width, int height, Text label, Style style, Runnable onPress) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.label = label;
        this.style = style;
        this.onPress = onPress;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void setLabel(Text label) {
        this.label = label;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void render(DrawContext ctx, TextRenderer textRenderer, double mouseX, double mouseY) {
        boolean hovered = enabled && ModernUi.hovered(mouseX, mouseY, x, y, width, height);
        int fill;
        int border;
        int textColor;
        if (!enabled) {
            fill = 0x8811192E;
            border = ModernUi.withAlpha(ModernUi.BORDER_SOFT, 0x44);
            textColor = ModernUi.TEXT_DISABLED;
        } else {
            switch (style) {
                case PRIMARY -> {
                    fill = hovered ? ModernUi.SELECTED_BACKGROUND : ModernUi.ACCENT_SOFT;
                    border = hovered ? ModernUi.ACCENT : ModernUi.BORDER_STRONG;
                    textColor = ModernUi.TEXT_PRIMARY;
                }
                case SECONDARY -> {
                    fill = hovered ? ModernUi.CARD_HOVER_BACKGROUND : ModernUi.CARD_BACKGROUND;
                    border = hovered ? ModernUi.BORDER_STRONG : ModernUi.BORDER;
                    textColor = ModernUi.TEXT_PRIMARY;
                }
                default -> {
                    fill = hovered ? ModernUi.CARD_HOVER_BACKGROUND : ModernUi.PANEL_BACKGROUND;
                    border = hovered ? ModernUi.BORDER_STRONG : ModernUi.BORDER_SOFT;
                    textColor = hovered ? ModernUi.TEXT_PRIMARY : ModernUi.TEXT_MUTED;
                }
            }
        }
        ctx.fill(x + 1, y + 2, x + width + 1, y + height + 2, 0x26000000);
        ctx.fill(x, y, x + width, y + height, fill);
        ctx.fill(x, y, x + width, y + 1, 0x22FFFFFF);
        ModernUi.border(ctx, x, y, width, height, border);

        String shown = ModernUi.trim(textRenderer, label.getString(), width - 10);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(shown), x + width / 2, y + (height - 8) / 2, textColor);
    }

    /** Returns true and fires onPress when the (virtual) click lands on the button. */
    public boolean mouseClicked(double mouseX, double mouseY) {
        if (enabled && ModernUi.hovered(mouseX, mouseY, x, y, width, height)) {
            onPress.run();
            return true;
        }
        return false;
    }
}
