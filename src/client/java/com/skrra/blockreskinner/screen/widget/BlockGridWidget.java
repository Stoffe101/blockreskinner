package com.skrra.blockreskinner.screen.widget;

public final class BlockGridWidget {
    public static final int MIN_TILE = 52;
    public static final int PREFERRED_TILE = 62;

    private BlockGridWidget() {
    }

    public static int columns(int width) {
        return columns(width, PREFERRED_TILE);
    }

    public static int columns(int width, int tile) {
        return Math.max(1, width / tile);
    }
}
