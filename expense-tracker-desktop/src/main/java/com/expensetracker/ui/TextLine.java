package com.expensetracker.ui;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.util.Map;

/** Single-line text with an explicit line-height factor and optional letter tracking. */
final class TextLine extends JComponent {
    private static final FontRenderContext FRC = new FontRenderContext(null, true, true);
    private final String text;
    private final float lineFactor;
    private final float trackingEm;

    TextLine(String text, Font font, Color color, float lineFactor) {
        this(text, font, color, lineFactor, 0f);
    }

    /** trackingEm is letter spacing in em (negative tightens large headings, positive opens small caps). */
    TextLine(String text, Font font, Color color, float lineFactor, float trackingEm) {
        this.text = text;
        this.lineFactor = lineFactor;
        this.trackingEm = trackingEm;
        setFont(font);
        setForeground(color);
        setOpaque(false);
    }

    private Font tracked() {
        return trackingEm == 0f
                ? getFont()
                : getFont().deriveFont(Map.of(TextAttribute.TRACKING, trackingEm));
    }

    @Override
    public Dimension getPreferredSize() {
        double advance = new TextLayout(text, tracked(), FRC).getAdvance();
        return new Dimension((int) Math.ceil(advance) + 2, (int) Math.ceil(getFont().getSize2D() * lineFactor));
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        UiKit.quality(g2);
        g2.setColor(getForeground());
        TextLayout layout = new TextLayout(text, tracked(), g2.getFontRenderContext());
        float baseline = (getHeight() - (layout.getAscent() + layout.getDescent())) / 2f + layout.getAscent();
        layout.draw(g2, 1f, baseline);
        g2.dispose();
    }
}
