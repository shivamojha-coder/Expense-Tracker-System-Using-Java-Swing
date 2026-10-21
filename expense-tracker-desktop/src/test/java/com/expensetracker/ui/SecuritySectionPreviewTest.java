package com.expensetracker.ui;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class SecuritySectionPreviewTest {

    @Test
    public void generatePreview() throws Exception {
        System.setProperty("java.awt.headless", "true");
        LandingPage page = new LandingPage(() -> {}, () -> {});
        JPanel sec = page.getSecuritySection();

        if (sec != null) {
            sec.setSize(1240, 520);
            layoutTree(sec);
            BufferedImage img = new BufferedImage(1240, 520, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            sec.paint(g2);
            g2.dispose();

            File out = new File("C:/Users/Shivam Ojha/.gemini/antigravity-ide/brain/9904d46e-12e1-4892-be69-0c02285ba772/security_preview.png");
            ImageIO.write(img, "png", out);
            System.out.println("Saved security preview to: " + out.getAbsolutePath());
        }
    }

    private void layoutTree(Container c) {
        c.doLayout();
        for (Component child : c.getComponents()) {
            if (child instanceof Container cont) {
                layoutTree(cont);
            }
        }
    }
}
