package com.expensetracker.ui;

import javax.swing.JComponent;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.util.ArrayList;
import java.util.List;

/** Word-wrapped paragraph whose height follows its current width. */
final class WrappedText extends JComponent {
    private final String text;
    private final float lineFactor;
    private final int maxWidth;
    private final int align;
    private int lastNeed = -1;

    WrappedText(String text, Font font, Color color, float lineFactor, int maxWidth, int align) {
        this.text = text;
        this.lineFactor = lineFactor;
        this.maxWidth = maxWidth;
        this.align = align;
        setFont(font);
        setForeground(color);
        setOpaque(false);
    }

    private int lineHeight() {
        return Math.round(getFont().getSize2D() * lineFactor);
    }

    private List<String> wrap(int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (line.length() > 0 && measure(candidate) > width) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines;
    }

    private double measure(String s) {
        return getFont().getStringBounds(s, new FontRenderContext(null, true, true)).getWidth();
    }

    private int effectiveWidth() {
        int w = getWidth() > 0 ? getWidth() : (maxWidth > 0 ? maxWidth : 280);
        return maxWidth > 0 ? Math.min(w, maxWidth) : w;
    }

    private int heightFor(int width) {
        return wrap(width).size() * lineHeight();
    }

    @Override
    public Dimension getPreferredSize() {
        int w = effectiveWidth();
        return new Dimension(maxWidth > 0 ? maxWidth : w, heightFor(w));
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(0, getPreferredSize().height);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(maxWidth > 0 ? maxWidth : Integer.MAX_VALUE, getPreferredSize().height);
    }

    @Override
    public void setBounds(int x, int y, int w, int h) {
        super.setBounds(x, y, w, h);
        int need = heightFor(Math.max(1, maxWidth > 0 ? Math.min(w, maxWidth) : w));
        if (need != lastNeed) {
            lastNeed = need;
            if (need != h) {
                revalidate();
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        UiKit.quality(g2);
        g2.setFont(getFont());
        g2.setColor(getForeground());
        FontMetrics fm = g2.getFontMetrics();
        int lh = lineHeight();
        int width = effectiveWidth();
        int y = 0;
        for (String line : wrap(width)) {
            float x = align == SwingConstants.CENTER ? (float) ((getWidth() - measure(line)) / 2) : 0f;
            g2.drawString(line, x, y + (lh - (fm.getAscent() + fm.getDescent())) / 2f + fm.getAscent());
            y += lh;
        }
        g2.dispose();
    }
}
