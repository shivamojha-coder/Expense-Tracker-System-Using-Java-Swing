package com.expensetracker.ui;

import com.expensetracker.ui.theme.AppTheme;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LandingPageVisualTest {

    @Test
    void rendersNavbarLogoWithoutClipping() {
        AppTheme.install();
        LandingPage page = new LandingPage(() -> {}, () -> {});
        page.setSize(1280, 800);
        layoutRecursively(page);

        BufferedImage img = new BufferedImage(1280, 800, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        page.printAll(g);
        g.dispose();

        assertNotNull(LandingPage.class.getResource("/images/app-logo.png"));
        assertTrue(countGreenPixels(img, new Rectangle(48, 8, 84, 80)) > 150);
    }

    private void layoutRecursively(Container container) {
        container.doLayout();
        for (Component component : container.getComponents()) {
            if (component instanceof Container child) {
                layoutRecursively(child);
            }
        }
    }

    private int countGreenPixels(BufferedImage image, Rectangle area) {
        int count = 0;
        for (int y = area.y; y < area.y + area.height; y++) {
            for (int x = area.x; x < area.x + area.width; x++) {
                Color color = new Color(image.getRGB(x, y));
                if (color.getGreen() > color.getRed() + 20 && color.getGreen() > color.getBlue() + 10) {
                    count++;
                }
            }
        }
        return count;
    }
}
