package com.dogetennant.dannouncements.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/** Pads chat components with leading spaces so they appear centered in the default chat box. */
public final class CenterUtil {

    // Half of the default chat box's usable pixel width - centering pads from the midpoint out.
    private static final int HALF_LINE_WIDTH_PX = 154;
    private static final int SPACE_WIDTH_PX = 4;
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private CenterUtil() {}

    public static Component center(Component input) {
        String plain = PLAIN.serialize(input);
        if (plain.isEmpty()) return input;

        int paddingPx = HALF_LINE_WIDTH_PX - (pixelWidth(plain) / 2);
        int spaces = paddingPx / SPACE_WIDTH_PX;
        if (spaces <= 0) return input;

        return Component.text(" ".repeat(spaces)).append(input);
    }

    private static int pixelWidth(String plain) {
        int total = 0;
        for (int i = 0; i < plain.length(); i++) {
            total += charWidth(plain.charAt(i));
        }
        return total;
    }

    // Approximate widths by character class rather than an exact per-glyph table - close enough
    // for centering purposes without pretending to reproduce Minecraft's font metrics precisely.
    private static int charWidth(char c) {
        if (c == ' ') return SPACE_WIDTH_PX;
        if ("iIl.,':;!|".indexOf(c) >= 0) return 2;
        if ("mMwW@".indexOf(c) >= 0) return 7;
        return 5;
    }
}
