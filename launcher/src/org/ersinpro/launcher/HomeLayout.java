package org.ersinpro.launcher;

/** Screen dimensions in dp, shared by both app rows. */
final class HomeLayout {
    final int edge, top, bottom, sectionGap, rowGap, headingGap;
    final int cardWidth, cardHeight, cardGap, tile, icon, labelSize;
    private final int height;
    static final int TILE_MARGIN = 4;
    static final int LABEL_GAP = 4;

    HomeLayout(int width, int height, boolean compactCards) {
        this.height = height;
        edge = Math.max(16, width * 55 / 1000);
        top = Math.max(12, height / 20);
        bottom = Math.max(16, height / 20);
        sectionGap = scale(height < 540 ? 15 : 21);
        rowGap = scale(8);
        headingGap = scale(5);
        cardGap = Math.max(4, scale(10));
        // Keep the second label line clear of bottom insets on large TV windows.
        int tileTarget = scale(height < 540 ? 165 : 196) - Math.max(0, height - 720) / 17;
        int preferred = Math.max(36, Math.min(width / 8, tileTarget));
        tile = compactCards ? Math.max(36, Math.round(preferred * 0.85f)) : preferred;
        labelSize = Math.max(11, scale(16));
        cardHeight = tile + TILE_MARGIN + LABEL_GAP + labelSize * 2 + 6;
        cardWidth = tile + TILE_MARGIN * 2;
        icon = tile - Math.max(4, scale(7));
    }

    int scale(int perThousand) {
        return Math.round(height * (perThousand / 1000f));
    }
}
