package com.expensetracker.ui;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Shared typography and rendering-quality helpers for the marketing and authentication screens. */
final class UiKit {
    private static final Set<String> INSTALLED_FONTS = new HashSet<>(Arrays.asList(
            GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
    private static final String TEXT_FAMILY = pickFont("Segoe UI", "Calibri", "SansSerif");
    private static final String TEXT_SEMIBOLD = pickFont("Segoe UI Semibold");

    private UiKit() {
    }

    private static String pickFont(String... candidates) {
        for (String name : candidates) {
            if (INSTALLED_FONTS.contains(name)) {
                return name;
            }
        }
        return null;
    }

    /**
     * Weight scale: Bold for page headings (30px+) and small UI labels/buttons, Semibold for
     * mid-size card titles (17-29px), regular for body text.
     */
    static Font font(int style, float size) {
        String base = TEXT_FAMILY == null ? "SansSerif" : TEXT_FAMILY;
        if ((style & Font.BOLD) != 0) {
            if (size >= 17f && size < 30f && TEXT_SEMIBOLD != null) {
                return new Font(TEXT_SEMIBOLD, Font.PLAIN, 12).deriveFont(size);
            }
            return new Font(base, Font.BOLD, 12).deriveFont(size);
        }
        return new Font(base, Font.PLAIN, 12).deriveFont(size);
    }

    /** Medium-weight (semibold) text, e.g. form labels and secondary buttons. */
    static Font medium(float size) {
        return TEXT_SEMIBOLD != null
                ? new Font(TEXT_SEMIBOLD, Font.PLAIN, 12).deriveFont(size)
                : font(Font.BOLD, size);
    }

    /** Anti-aliasing, pure stroke control and the desktop's native text hints. */
    static void quality(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        Object hints = Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
        if (hints instanceof Map<?, ?> map) {
            g2.addRenderingHints(map);
        } else {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    }
}
