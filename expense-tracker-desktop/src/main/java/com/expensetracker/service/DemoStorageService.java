package com.expensetracker.service;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;

public final class DemoStorageService implements StorageService {
    private final Map<String, byte[]> storage = new ConcurrentHashMap<>();

    public DemoStorageService() {
        seedSampleReceipt();
    }

    private void seedSampleReceipt() {
        try {
            BufferedImage img = new BufferedImage(400, 500, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, 400, 500);

            g.setColor(new Color(0x17, 0x21, 0x1D));
            g.setFont(new Font("Monospaced", Font.BOLD, 18));
            g.drawString("AMAZON ONLINE STORE", 80, 50);

            g.setFont(new Font("Monospaced", Font.PLAIN, 12));
            g.drawString("Order # 112-9842192-4412", 40, 90);
            g.drawString("Date: " + java.time.LocalDate.now().minusDays(3), 40, 110);
            g.drawString("----------------------------------------", 40, 130);
            g.drawString("Mechanical Keyboard          $79.99", 40, 160);
            g.drawString("Shipping & Handling           $0.00", 40, 180);
            g.drawString("Tax (Estimated)               $0.00", 40, 200);
            g.drawString("----------------------------------------", 40, 220);
            g.setFont(new Font("Monospaced", Font.BOLD, 14));
            g.drawString("TOTAL DUE:                   $79.99", 40, 250);
            g.setFont(new Font("Monospaced", Font.PLAIN, 11));
            g.drawString("Payment: Debit Card ending in 4242", 40, 290);
            g.drawString("Status: PAID", 40, 310);
            g.drawString("----------------------------------------", 40, 340);
            g.drawString("Thank you for shopping with us!", 60, 380);
            g.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            storage.put("receipts/amazon-keyboard.png", baos.toByteArray());
        } catch (IOException ignored) {
        }
    }

    @Override
    public String uploadReceipt(UUID expenseId, Path file) {
        try {
            byte[] bytes = Files.readAllBytes(file);
            String path = "receipts/" + expenseId + "-" + file.getFileName().toString();
            storage.put(path, bytes);
            return path;
        } catch (IOException ex) {
            throw new ServiceException("Failed to upload receipt to local storage: " + ex.getMessage(), ex);
        }
    }

    @Override
    public byte[] downloadReceipt(String receiptPath) {
        byte[] data = storage.get(receiptPath);
        if (data == null) {
            throw new ServiceException("Receipt file not found in demo storage: " + receiptPath);
        }
        return data;
    }

    @Override
    public void deleteReceipt(String receiptPath) {
        storage.remove(receiptPath);
    }
}
