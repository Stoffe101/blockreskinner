package com.skrra.blockreskinner.screen.widget;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public final class ModernScreenUtil {
    public static final int PANEL = 0xD80A0C12;
    public static final int CARD = 0xE3171A23;
    public static final int CARD_SOFT = 0xB51D2230;
    public static final int BORDER = 0x804E5F77;
    public static final int BORDER_SOFT = 0x4CFFFFFF;
    public static final int ACCENT = 0xFF72D9FF;
    public static final int ACCENT_SOFT = 0x55359CCC;
    public static final int TEXT = 0xFFF4F7FB;
    public static final int MUTED = 0xFFB7C0CC;
    public static final int SUBTLE = 0xFF7F8A98;
    public static final int GOOD = 0xFF86E08F;
    public static final int WARN = 0xFFFFD166;

    private ModernScreenUtil() {
    }

    public static void panel(DrawContext context, int x, int y, int width, int height) {
        context.fill(x, y, x + width, y + height, PANEL);
        context.drawStrokedRectangle(x, y, width, height, BORDER);
    }

    public static void card(DrawContext context, int x, int y, int width, int height) {
        context.fill(x, y, x + width, y + height, CARD);
        context.drawStrokedRectangle(x, y, width, height, BORDER_SOFT);
    }

    public static void softCard(DrawContext context, int x, int y, int width, int height) {
        context.fill(x, y, x + width, y + height, CARD_SOFT);
    }

    public static String trim(TextRenderer textRenderer, String text, int width) {
        if (textRenderer.getWidth(text) <= width) {
            return text;
        }
        return textRenderer.trimToWidth(text, Math.max(0, width - textRenderer.getWidth("..."))) + "...";
    }

    public static void label(DrawContext context, TextRenderer textRenderer, Text text, int x, int y, int color) {
        context.drawTextWithShadow(textRenderer, text, x, y, color);
    }
}
