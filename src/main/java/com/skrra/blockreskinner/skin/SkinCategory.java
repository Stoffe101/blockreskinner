package com.skrra.blockreskinner.skin;

public enum SkinCategory {
    FULL_BLOCKS("screen.blockreskinner.category.full_blocks"),
    LOGS_AND_PILLARS("screen.blockreskinner.category.logs_and_pillars"),
    LEAVES("screen.blockreskinner.category.leaves"),
    TRANSPARENT("screen.blockreskinner.category.transparent"),
    CUTOUT("screen.blockreskinner.category.cutout"),
    CONNECTED_FENCES("screen.blockreskinner.category.connected_fences"),
    CONNECTED_WALLS("screen.blockreskinner.category.connected_walls"),
    CONNECTED_PANES_AND_BARS("screen.blockreskinner.category.connected_panes_and_bars");

    private final String translationKey;

    SkinCategory(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }
}
