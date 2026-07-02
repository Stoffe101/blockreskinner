package com.skrra.blockreskinner.screen.widget;

public final class BlockGridWidget {
    public static final int TILE = 26;
    public static final int ICON_OFFSET = 5;

    private BlockGridWidget() {
    }

    public static int columns(int width) {
        return Math.max(1, width / TILE);
    }
}
