package com.mechaniciancombat.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** 1.20.1 equivalent of the r396 96x32 GUI sprite, with eight-pixel borders. */
final class GuiNineSlice {
    static void ammoBackground(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height) {
        int[] sourceX = {0, 8, 88}, sourceY = {0, 8, 24};
        int[] targetX = {x, x + 8, x + width - 8}, targetY = {y, y + 8, y + height - 8};
        int[] widths = {8, width - 16, 8}, heights = {8, height - 16, 8};
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                int tileWidth = column == 1 ? 80 : 8;
                int tileHeight = row == 1 ? 16 : 8;
                for (int yy = 0; yy < heights[row]; yy += tileHeight) {
                    for (int xx = 0; xx < widths[column]; xx += tileWidth) {
                        graphics.blit(texture, targetX[column] + xx, targetY[row] + yy,
                                sourceX[column], sourceY[row], Math.min(tileWidth, widths[column] - xx),
                                Math.min(tileHeight, heights[row] - yy), 96, 32);
                    }
                }
            }
        }
    }
    private GuiNineSlice() { }
}
