package com.expensetracker.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/** Image that is down-scaled once per target size (device pixels) and cached. */
final class SmoothImage {
    private final BufferedImage src;
    private final Map<String, BufferedImage> shadows = new java.util.HashMap<>();
    private BufferedImage cache;
    private int cacheW;
    private int cacheH;

    SmoothImage(BufferedImage src) {
        this.src = src;
    }

    int width() {
        return src.getWidth();
    }

    int height() {
        return src.getHeight();
    }

    void draw(Graphics2D g2, int x, int y, int w, int h) {
        int pw = Math.max(1, (int) Math.round(w * g2.getTransform().getScaleX()));
        int ph = Math.max(1, (int) Math.round(h * g2.getTransform().getScaleY()));
        if (cache == null || cacheW != pw || cacheH != ph) {
            cache = scale(src, pw, ph);
            cacheW = pw;
            cacheH = ph;
        }
        g2.drawImage(cache, x, y, w, h, null);
    }

    /** Draws a blurred, dark copy of the image's silhouette (cached per size and blur) under the picture. */
    void drawShadow(Graphics2D g2, int x, int y, int w, int h, int blur, int dy, float alpha) {
        double sx = g2.getTransform().getScaleX();
        double sy = g2.getTransform().getScaleY();
        int pw = Math.max(1, (int) Math.round(w * sx));
        int ph = Math.max(1, (int) Math.round(h * sy));
        String key = pw + "x" + ph + "/" + blur;
        BufferedImage shadow = shadows.get(key);
        int pad = (int) Math.ceil(blur * 2 * sx);
        if (shadow == null) {
            if (cache == null || cacheW != pw || cacheH != ph) {
                cache = scale(src, pw, ph);
                cacheW = pw;
                cacheH = ph;
                shadows.clear();
            }
            shadow = blurSilhouette(cache, pad, Math.max(1, (int) Math.round(blur * sx)));
            shadows.put(key, shadow);
        }
        Graphics2D g = (Graphics2D) g2.create();
        g.setComposite(java.awt.AlphaComposite.SrcOver.derive(alpha));
        g.drawImage(shadow, x - (int) Math.round(pad / sx), y + dy - (int) Math.round(pad / sy),
                (int) Math.round(shadow.getWidth() / sx), (int) Math.round(shadow.getHeight() / sy), null);
        g.dispose();
    }

    private static BufferedImage blurSilhouette(BufferedImage in, int pad, int radius) {
        BufferedImage out = new BufferedImage(in.getWidth() + pad * 2, in.getHeight() + pad * 2, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(in, pad, pad, null);
        g.setComposite(java.awt.AlphaComposite.SrcIn);
        g.setColor(new Color(6, 30, 24));
        g.fillRect(0, 0, out.getWidth(), out.getHeight());
        g.dispose();
        int size = radius * 2 + 1;
        float[] data = new float[size];
        java.util.Arrays.fill(data, 1f / size);
        BufferedImage tmp = new java.awt.image.ConvolveOp(new java.awt.image.Kernel(size, 1, data),
                java.awt.image.ConvolveOp.EDGE_NO_OP, null).filter(out, null);
        return new java.awt.image.ConvolveOp(new java.awt.image.Kernel(1, size, data),
                java.awt.image.ConvolveOp.EDGE_NO_OP, null).filter(tmp, null);
    }

    private static BufferedImage scale(BufferedImage in, int w, int h) {
        BufferedImage cur = in;
        int cw = in.getWidth();
        int ch = in.getHeight();
        while (cw / 2 >= w && ch / 2 >= h) {
            cw /= 2;
            ch /= 2;
            cur = step(cur, cw, ch);
        }
        return step(cur, w, h);
    }

    private static BufferedImage step(BufferedImage in, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(in, 0, 0, w, h, null);
        g.dispose();
        return out;
    }
}
