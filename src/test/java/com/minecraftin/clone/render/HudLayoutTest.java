package com.minecraftin.clone.render;

import com.minecraftin.clone.world.BlockType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HudLayoutTest {
    @Test
    void everyEntryIsVisibleAndClickableAtAllSizesAndDpiScales() {
        int count = BlockType.creativePalette().length;
        for (int[] size : new int[][] {{400, 800}, {800, 600}, {1600, 900}, {80, 1600}}) {
            int width = size[0], height = size[1];
            var layout = HudRenderer.creativeLayout((float) width / height);
            assertTrue(layout.startX() >= -1);
            assertTrue(layout.startX() + layout.width() <= 1);
            assertTrue(layout.slotY(HudRenderer.CREATIVE_ROWS - 1) >= -1);
            for (int page = 0; page < HudRenderer.creativeTotalPages(count); page++) {
                for (int slot = 0; slot < HudRenderer.CREATIVE_SLOTS_PER_PAGE; slot++) {
                    double x = layout.slotX(slot % HudRenderer.CREATIVE_COLUMNS) + layout.slotWidth() * 0.5;
                    double y = layout.slotY(slot / HudRenderer.CREATIVE_COLUMNS) + layout.slotHeight() * 0.5;
                    int expected = page * HudRenderer.CREATIVE_SLOTS_PER_PAGE + slot;
                    if (expected >= count) expected = -1;
                    for (int dpi : new int[] {1, 2}) {
                        // Scaling both cursor coordinates and dimensions preserves the NDC hit.
                        assertEquals(expected, HudRenderer.creativeSlotAt((x + 1) * width * dpi / 2,
                                (1 - y) * height * dpi / 2, width * dpi, height * dpi, page, count));
                    }
                }
            }
        }
    }

    @Test
    void longErrorMessagesWrapWithoutLosingTheirText() {
        String message = "Save failed: the world destination is currently unavailable. Press R to retry or Escape to return to the game.";
        var lines = HudRenderer.wrapDebugLine(message);
        assertTrue(lines.stream().allMatch(line -> line.length() <= 63));
        assertEquals(message, String.join(" ", lines));
        assertEquals(java.util.List.of(""), HudRenderer.wrapDebugLine(null));
    }

    @Test
    void outsidePanelAndInvalidInputsDoNotSelectBlocks() {
        int count = BlockType.creativePalette().length;
        assertEquals(-1, HudRenderer.creativeSlotAt(-1, 400, 400, 800, 0, count));
        assertEquals(-1, HudRenderer.creativeSlotAt(401, 400, 400, 800, 0, count));
        assertEquals(-1, HudRenderer.creativeSlotAt(200, 0, 400, 800, 0, count));
        assertEquals(-1, HudRenderer.creativeSlotAt(Double.NaN, 1, 400, 800, 0, count));
        assertEquals(-1, HudRenderer.creativeSlotAt(1, 1, 0, 800, 0, count));
    }
}
