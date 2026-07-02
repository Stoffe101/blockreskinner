package com.skrra.blockreskinner.screen.layout;

/**
 * Computed rectangles (in virtual canvas coordinates) for the Block Reskinner
 * screens. Built once per {@link ReskinLayoutProfile#key()} change.
 */
public final class ReskinLayout {
    public record Rect(int x, int y, int w, int h) {
        public int right() {
            return x + w;
        }

        public int bottom() {
            return y + h;
        }

        public boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    public final ReskinLayoutProfile profile;
    public final Rect window;
    public final Rect search;
    /** Category sidebar (simple screen) - null when hidden or on the connected screen. */
    public final Rect sidebar;
    public final Rect grid;
    /** Selected info panel (simple screen) - null on the connected screen. */
    public final Rect selected;
    /** Right-side details panel (connected screen) - null on the simple screen. */
    public final Rect sidePanel;
    public final Rect footer;
    public final int cardSize;
    public final int cardGap;
    public final int columns;

    private ReskinLayout(ReskinLayoutProfile profile, Rect window, Rect search, Rect sidebar, Rect grid,
                         Rect selected, Rect sidePanel, Rect footer, int cardSize, int cardGap, int columns) {
        this.profile = profile;
        this.window = window;
        this.search = search;
        this.sidebar = sidebar;
        this.grid = grid;
        this.selected = selected;
        this.sidePanel = sidePanel;
        this.footer = footer;
        this.cardSize = cardSize;
        this.cardGap = cardGap;
        this.columns = columns;
    }

    public static ReskinLayout forSimple(ReskinLayoutProfile profile) {
        Rect window = windowRect(profile);
        int pad = profile.padding();
        Rect search = searchRect(profile, window);
        Rect footer = footerRect(profile, window);

        int selectedH = profile.selectedPanelHeight();
        Rect selected = new Rect(window.x() + pad, footer.y() - 8 - selectedH, window.w() - pad * 2, selectedH);

        int contentTop = search.bottom() + 8;
        int contentH = Math.max(60, selected.y() - 8 - contentTop);

        Rect sidebar = null;
        int gridX = window.x() + pad;
        int sidebarW = profile.sidebarWidth();
        if (sidebarW > 0) {
            sidebar = new Rect(window.x() + pad, contentTop, sidebarW, contentH);
            gridX = sidebar.right() + 10;
        }
        Rect grid = new Rect(gridX, contentTop, window.right() - pad - gridX, contentH);

        int gap = profile.cardGap();
        int columns = gridColumns(profile, grid, gap);
        int cardSize = cardSize(profile, grid, gap, columns);
        return new ReskinLayout(profile, window, search, sidebar, grid, selected, null, footer, cardSize, gap, columns);
    }

    public static ReskinLayout forConnected(ReskinLayoutProfile profile) {
        Rect window = windowRect(profile);
        int pad = profile.padding();
        Rect search = searchRect(profile, window);
        Rect footer = footerRect(profile, window);

        int contentTop = search.bottom() + 8;
        int contentH = Math.max(140, footer.y() - 8 - contentTop);

        int sideW = profile.sidePanelWidth(window.w());
        Rect sidePanel = new Rect(window.right() - pad - sideW, contentTop, sideW, contentH);
        Rect grid = new Rect(window.x() + pad, contentTop, sidePanel.x() - 10 - (window.x() + pad), contentH);

        int gap = profile.cardGap();
        int columns = gridColumns(profile, grid, gap);
        int cardSize = cardSize(profile, grid, gap, columns);
        return new ReskinLayout(profile, window, search, null, grid, null, sidePanel, footer, cardSize, gap, columns);
    }

    private static Rect windowRect(ReskinLayoutProfile profile) {
        int w = profile.windowWidth();
        int h = profile.windowHeight();
        return new Rect((profile.virtualWidth - w) / 2, (profile.virtualHeight - h) / 2, w, h);
    }

    private static Rect searchRect(ReskinLayoutProfile profile, Rect window) {
        int pad = profile.padding();
        return new Rect(window.x() + pad, window.y() + profile.headerHeight() + 8, window.w() - pad * 2, profile.searchHeight());
    }

    private static Rect footerRect(ReskinLayoutProfile profile, Rect window) {
        int footerH = profile.footerHeight();
        return new Rect(window.x() + 1, window.bottom() - footerH - 1, window.w() - 2, footerH);
    }

    private static int gridColumns(ReskinLayoutProfile profile, Rect grid, int gap) {
        int inner = grid.w() - 12;
        return Math.max(1, (inner + gap) / (profile.preferredCardSize() + gap));
    }

    private static int cardSize(ReskinLayoutProfile profile, Rect grid, int gap, int columns) {
        int inner = grid.w() - 12;
        int size = (inner - (columns - 1) * gap) / columns;
        return Math.max(profile.minCardSize(), Math.min(profile.maxCardSize(), size));
    }

    /** Inner grid origin (cards start here, inside the grid panel padding). */
    public int gridInnerX() {
        return grid.x() + 6;
    }

    public int gridInnerY() {
        return grid.y() + 6;
    }

    public int gridInnerHeight() {
        return grid.h() - 12;
    }

    public int visibleGridRows() {
        return Math.max(1, (gridInnerHeight() + cardGap) / (cardSize + cardGap));
    }
}
