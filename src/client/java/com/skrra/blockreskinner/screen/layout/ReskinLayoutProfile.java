package com.skrra.blockreskinner.screen.layout;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;

/**
 * Layout profile in the style of Atmosphere+'s LayoutProfile: instead of laying
 * out against the raw scaled screen (which starves at GUI scale 3), layout math
 * runs against a virtual canvas whose effective GUI scale is clamped to 2.0.
 * The UI is drawn in virtual coordinates and shrunk to the real screen with a
 * matrix scale; mouse coordinates are divided by the same renderScale.
 *
 * <p>This makes 1920x1080 at GUI scale 2 and 3 resolve to the same 960x540
 * virtual canvas, and 3440x1440 at scale 2 and 3 to the same ~1720x720 canvas.
 */
public final class ReskinLayoutProfile {
    public enum Mode {
        TINY,
        COMPACT,
        NORMAL,
        WIDE,
        ULTRAWIDE
    }

    private static final double MAX_EFFECTIVE_GUI_SCALE = 2.0D;

    public final Mode mode;
    /** Virtual canvas size; all layout math uses these, never the real scaled size. */
    public final int virtualWidth;
    public final int virtualHeight;
    public final double guiScale;
    /** Multiply virtual coordinates by this to get real scaled-screen coordinates. */
    public final double renderScale;
    public final int realScaledWidth;
    public final int realScaledHeight;
    private final int framebufferWidth;
    private final int framebufferHeight;

    private ReskinLayoutProfile(Mode mode, int virtualWidth, int virtualHeight, double guiScale, double renderScale,
                                int realScaledWidth, int realScaledHeight, int framebufferWidth, int framebufferHeight) {
        this.mode = mode;
        this.virtualWidth = virtualWidth;
        this.virtualHeight = virtualHeight;
        this.guiScale = guiScale;
        this.renderScale = renderScale;
        this.realScaledWidth = realScaledWidth;
        this.realScaledHeight = realScaledHeight;
        this.framebufferWidth = framebufferWidth;
        this.framebufferHeight = framebufferHeight;
    }

    public static ReskinLayoutProfile create(int scaledWidth, int scaledHeight) {
        MinecraftClient client = MinecraftClient.getInstance();
        int framebufferWidth = scaledWidth;
        int framebufferHeight = scaledHeight;
        double guiScale = 1.0D;
        if (client != null && client.getWindow() != null) {
            Window window = client.getWindow();
            framebufferWidth = window.getFramebufferWidth();
            framebufferHeight = window.getFramebufferHeight();
            guiScale = window.getScaleFactor();
        }

        double targetGuiScale = guiScale > 0 ? Math.min(guiScale, MAX_EFFECTIVE_GUI_SCALE) : MAX_EFFECTIVE_GUI_SCALE;
        double renderScale = guiScale > 0 ? targetGuiScale / guiScale : 1.0D;

        int virtualWidth = scaledWidth;
        int virtualHeight = scaledHeight;
        if (renderScale > 0 && renderScale != 1.0D) {
            virtualWidth = Math.max(1, Math.round((float) (scaledWidth / renderScale)));
            virtualHeight = Math.max(1, Math.round((float) (scaledHeight / renderScale)));
        }

        Mode mode;
        if (virtualWidth <= 540 || virtualHeight <= 340) {
            mode = Mode.TINY;
        } else if (virtualWidth <= 800 || virtualHeight <= 440) {
            mode = Mode.COMPACT;
        } else if (virtualWidth <= 1120) {
            mode = Mode.NORMAL;
        } else if (virtualWidth >= 1600) {
            mode = Mode.ULTRAWIDE;
        } else {
            mode = Mode.WIDE;
        }

        return new ReskinLayoutProfile(mode, virtualWidth, virtualHeight, guiScale, renderScale,
                scaledWidth, scaledHeight, framebufferWidth, framebufferHeight);
    }

    /** Rebuild trigger, like Atmosphere+'s layout.key(). */
    public String key() {
        return mode.name() + ":" + virtualWidth + "x" + virtualHeight + ":" + framebufferWidth + "x" + framebufferHeight
                + ":" + Math.round(guiScale * 100.0D);
    }

    public int windowWidth() {
        int margin = outerMargin() * 2;
        return switch (mode) {
            case TINY -> Math.max(1, virtualWidth - margin);
            case COMPACT -> Math.min(720, virtualWidth - margin);
            case NORMAL -> Math.min(920, virtualWidth - margin);
            case WIDE -> Math.min(1150, virtualWidth - margin);
            case ULTRAWIDE -> Math.min(1400, virtualWidth - margin);
        };
    }

    public int windowHeight() {
        int margin = outerMargin() * 2;
        return switch (mode) {
            case TINY -> Math.max(1, virtualHeight - margin);
            case COMPACT -> Math.min(430, virtualHeight - margin);
            case NORMAL -> Math.min(510, virtualHeight - margin);
            case WIDE -> Math.min(620, virtualHeight - margin);
            case ULTRAWIDE -> Math.min(680, virtualHeight - margin);
        };
    }

    public int outerMargin() {
        return mode == Mode.TINY ? 4 : 12;
    }

    public int padding() {
        return switch (mode) {
            case TINY -> 8;
            case COMPACT -> 10;
            default -> 12;
        };
    }

    public int headerHeight() {
        return mode == Mode.TINY || mode == Mode.COMPACT ? 38 : 44;
    }

    public int searchHeight() {
        return mode == Mode.TINY || mode == Mode.COMPACT ? 24 : 26;
    }

    public int footerHeight() {
        return mode == Mode.TINY || mode == Mode.COMPACT ? 36 : 40;
    }

    public int selectedPanelHeight() {
        return mode == Mode.TINY || mode == Mode.COMPACT ? 56 : 64;
    }

    public int buttonHeight() {
        return mode == Mode.TINY || mode == Mode.COMPACT ? 24 : 26;
    }

    public int sidebarWidth() {
        return switch (mode) {
            case TINY -> 0;
            case COMPACT -> 128;
            case NORMAL -> 150;
            default -> 165;
        };
    }

    public int sidePanelWidth(int windowWidth) {
        return Math.min(320, Math.max(250, windowWidth / 3));
    }

    public int categoryRowHeight() {
        return 24;
    }

    public int minCardSize() {
        return 54;
    }

    public int preferredCardSize() {
        return mode == Mode.TINY || mode == Mode.COMPACT ? 60 : 66;
    }

    public int maxCardSize() {
        return 78;
    }

    public int cardGap() {
        return 7;
    }
}
