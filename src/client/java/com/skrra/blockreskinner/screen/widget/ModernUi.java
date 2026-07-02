package com.skrra.blockreskinner.screen.widget;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.util.List;

/**
 * Modern chrome helpers for the Block Reskinner screens, adapted from
 * Atmosphere+'s UiRender/V2DesignTokens (fixed palette, no theme system).
 * All coordinates are virtual-canvas coordinates.
 */
public final class ModernUi {
    public static final int WINDOW_BACKGROUND = 0xF20A0F1F;
    public static final int WINDOW_BACKGROUND_DEEP = 0xF8050914;
    public static final int PANEL_BACKGROUND = 0xCC11192E;
    public static final int CARD_BACKGROUND = 0xC7142038;
    public static final int CARD_HOVER_BACKGROUND = 0xE01C2B4C;
    public static final int SELECTED_BACKGROUND = 0xD52A3F8F;
    public static final int BORDER = 0xAA33466F;
    public static final int BORDER_STRONG = 0xCC637DFF;
    public static final int BORDER_SOFT = 0x774E67A2;
    public static final int ACCENT = 0xFF6D88FF;
    public static final int ACCENT_ALT = 0xFFE76DFF;
    public static final int ACCENT_SOFT = 0x773A52C9;
    public static final int TEXT_PRIMARY = 0xFFEFF4FF;
    public static final int TEXT_MUTED = 0xFF9CA9C8;
    public static final int TEXT_DISABLED = 0xFF66708D;
    public static final int SUCCESS = 0xFF6BFFB8;
    public static final int WARNING = 0xFFFFD166;

    private ModernUi() {
    }

    public static void window(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x + 3, y + 3, x + w + 3, y + h + 3, 0x55000000);
        ctx.fill(x, y, x + w, y + h, WINDOW_BACKGROUND);
        gradientHorizontal(ctx, x + 1, y + 1, w - 2, 1, ACCENT, ACCENT_ALT);
        border(ctx, x, y, w, h, BORDER);
    }

    /** Header band: deep gradient with a bottom hairline; drawn inside the window. */
    public static void headerBand(DrawContext ctx, int x, int y, int w, int h) {
        gradientHorizontal(ctx, x, y, w, h, WINDOW_BACKGROUND_DEEP, PANEL_BACKGROUND);
        ctx.fill(x, y + h - 1, x + w, y + h, BORDER);
    }

    public static void panel(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x + 1, y + 2, x + w + 1, y + h + 2, 0x33000000);
        ctx.fill(x, y, x + w, y + h, PANEL_BACKGROUND);
        ctx.fill(x, y, x + w, y + 1, 0x22FFFFFF);
        border(ctx, x, y, w, h, BORDER_SOFT);
    }

    /**
     * Card in the Atmosphere+ v2 style: idle cards have no outline; hover gets a
     * soft border; selection gets the accent border plus a gradient left rail.
     */
    public static void card(DrawContext ctx, int x, int y, int w, int h, boolean hovered, boolean selected) {
        int fill = selected ? SELECTED_BACKGROUND : hovered ? CARD_HOVER_BACKGROUND : CARD_BACKGROUND;
        ctx.fill(x + 1, y + 2, x + w + 1, y + h + 2, 0x1E000000);
        ctx.fill(x, y, x + w, y + h, fill);
        ctx.fill(x, y, x + w, y + 1, 0x22FFFFFF);
        if (selected) {
            border(ctx, x, y, w, h, ACCENT);
            gradientVertical(ctx, x, y, 3, h, ACCENT, ACCENT_ALT);
        } else if (hovered) {
            border(ctx, x, y, w, h, BORDER_SOFT);
        }
    }

    /** 1px divider with a short accent gradient at its start. */
    public static void rule(DrawContext ctx, int x, int y, int w, int accentWidth) {
        ctx.fill(x, y, x + w, y + 1, BORDER);
        gradientHorizontal(ctx, x, y, Math.min(w, Math.max(24, accentWidth)), 1, ACCENT, ACCENT_ALT);
    }

    public static void border(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    public static void gradientHorizontal(DrawContext ctx, int x, int y, int w, int h, int leftColor, int rightColor) {
        int slices = Math.max(1, Math.min(w, 32));
        for (int i = 0; i < slices; i++) {
            float p = slices <= 1 ? 0f : (float) i / (float) (slices - 1);
            int sx = x + i * w / slices;
            int ex = x + (i + 1) * w / slices;
            ctx.fill(sx, y, ex, y + h, lerpColor(leftColor, rightColor, p));
        }
    }

    public static void gradientVertical(DrawContext ctx, int x, int y, int w, int h, int topColor, int bottomColor) {
        int slices = Math.max(1, Math.min(h, 32));
        for (int i = 0; i < slices; i++) {
            float p = slices <= 1 ? 0f : (float) i / (float) (slices - 1);
            int sy = y + i * h / slices;
            int ey = y + (i + 1) * h / slices;
            ctx.fill(x, sy, x + w, ey, lerpColor(topColor, bottomColor, p));
        }
    }

    public static void searchBox(DrawContext ctx, TextRenderer textRenderer, int x, int y, int w, int h,
                                 String query, Text placeholder, boolean focused) {
        boolean hasQuery = !query.isEmpty();
        ctx.fill(x + 1, y + 2, x + w + 1, y + h + 2, 0x26000000);
        ctx.fill(x, y, x + w, y + h, 0xE0081020);
        border(ctx, x, y, w, h, focused ? ACCENT : hasQuery ? ACCENT_ALT : BORDER);

        int textY = y + (h - 8) / 2;
        if (hasQuery) {
            String shown = trim(textRenderer, query, w - 30);
            ctx.drawTextWithShadow(textRenderer, Text.literal(shown), x + 8, textY, TEXT_PRIMARY);
            if (focused && caretVisible()) {
                int caretX = Math.min(x + w - 26, x + 9 + textRenderer.getWidth(shown));
                ctx.fill(caretX, textY - 1, caretX + 1, textY + 9, ACCENT);
            }
            // clear button
            ctx.drawTextWithShadow(textRenderer, Text.literal("x"), x + w - 14, textY, TEXT_MUTED);
        } else {
            ctx.drawTextWithShadow(textRenderer, placeholder, x + 8, textY, TEXT_MUTED);
            if (focused && caretVisible()) {
                ctx.fill(x + 8, textY - 1, x + 9, textY + 9, ACCENT);
            }
        }
    }

    public static boolean overSearchClear(double mx, double my, int x, int y, int w, int h, String query) {
        return !query.isEmpty() && hovered(mx, my, x + w - 20, y, 20, h);
    }

    /** Thin vertical scrollbar; offset/total/viewport are in rows or pixels, any consistent unit. */
    public static void scrollbar(DrawContext ctx, int x, int y, int h, int total, int viewport, int offset) {
        if (total <= viewport) {
            return;
        }
        ctx.fill(x, y, x + 2, y + h, 0x33121C33);
        int barH = Math.max(10, h * viewport / total);
        int maxOffset = total - viewport;
        int barY = y + (h - barH) * Math.min(offset, maxOffset) / Math.max(1, maxOffset);
        ctx.fill(x, barY, x + 2, barY + barH, BORDER_STRONG);
    }

    public static String trim(TextRenderer textRenderer, String text, int maxWidth) {
        if (textRenderer.getWidth(text) <= maxWidth) {
            return text;
        }
        String result = text;
        while (result.length() > 3 && textRenderer.getWidth(result + "...") > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result + "...";
    }

    /** Custom tooltip drawn in virtual coordinates and clamped to the virtual canvas. */
    public static void tooltip(DrawContext ctx, TextRenderer textRenderer, List<Text> lines, int mouseX, int mouseY,
                               int virtualWidth, int virtualHeight) {
        if (lines.isEmpty()) {
            return;
        }
        int w = 0;
        for (Text line : lines) {
            w = Math.max(w, textRenderer.getWidth(line));
        }
        int h = lines.size() * 11 + 7;
        int x = mouseX + 10;
        int y = mouseY - 4;
        if (x + w + 8 > virtualWidth) {
            x = mouseX - w - 18;
        }
        x = Math.max(2, x);
        y = Math.max(2, Math.min(y, virtualHeight - h - 2));

        ctx.fill(x + 1, y + 2, x + w + 9, y + h + 2, 0x44000000);
        ctx.fill(x, y, x + w + 8, y + h, 0xF6081020);
        gradientHorizontal(ctx, x + 1, y + 1, w + 6, 1, ACCENT, ACCENT_ALT);
        border(ctx, x, y, w + 8, h, BORDER);
        int lineY = y + 4;
        for (Text line : lines) {
            ctx.drawTextWithShadow(textRenderer, line, x + 4, lineY, TEXT_PRIMARY);
            lineY += 11;
        }
    }

    public static boolean hovered(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    public static boolean caretVisible() {
        return Util.getMeasuringTimeMs() / 500L % 2L == 0L;
    }

    public static int withAlpha(int color, int alpha) {
        return ((alpha & 255) << 24) | (color & 0x00FFFFFF);
    }

    public static int lerpColor(int from, int to, float progress) {
        progress = Math.max(0f, Math.min(1f, progress));
        int a = lerp((from >>> 24) & 255, (to >>> 24) & 255, progress);
        int r = lerp((from >>> 16) & 255, (to >>> 16) & 255, progress);
        int g = lerp((from >>> 8) & 255, (to >>> 8) & 255, progress);
        int b = lerp(from & 255, to & 255, progress);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int lerp(int a, int b, float progress) {
        return (int) (a + (b - a) * progress);
    }
}
