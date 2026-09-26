package com.expensetracker.ui;

import com.expensetracker.ui.theme.ThemeColors;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public final class ReceiptViewerDialog extends JDialog {
    private BufferedImage originalImage;
    private double zoomFactor = 1.0;
    private int rotationDegrees = 0;
    private final ImageCanvas canvas;
    private final JLabel statusLabel = new JLabel("Zoom: 100% | Rotation: 0°");

    public ReceiptViewerDialog(Window owner, String title, byte[] data, String filename) {
        super(owner, title != null ? title : "Receipt Viewer", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(900, 700);
        setMinimumSize(new Dimension(650, 500));
        setLocationRelativeTo(owner);

        loadImage(data, filename);

        canvas = new ImageCanvas();
        JScrollPane scrollPane = new JScrollPane(canvas);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(new Color(0xF1, 0xF5, 0xF9));

        JPanel toolbar = buildToolbar();

        setLayout(new BorderLayout());
        add(toolbar, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        footer.setBackground(ThemeColors.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeColors.BORDER));
        statusLabel.setForeground(ThemeColors.SECONDARY_TEXT);
        footer.add(statusLabel);
        add(footer, BorderLayout.SOUTH);

        SwingUtilities.invokeLater(this::fitToWindow);
    }

    public static void showReceipt(Window owner, String title, byte[] data, String filename) {
        ReceiptViewerDialog dialog = new ReceiptViewerDialog(owner, title, data, filename);
        dialog.setVisible(true);
    }

    private void loadImage(byte[] data, String filename) {
        if (data == null || data.length == 0) {
            originalImage = createPlaceholder("No receipt data available");
            return;
        }

        String lower = (filename != null ? filename : "").toLowerCase();
        if (lower.endsWith(".pdf")) {
            try (PDDocument doc = Loader.loadPDF(data)) {
                if (doc.getNumberOfPages() > 0) {
                    PDFRenderer renderer = new PDFRenderer(doc);
                    originalImage = renderer.renderImageWithDPI(0, 150);
                } else {
                    originalImage = createPlaceholder("PDF document contains no pages");
                }
            } catch (Exception ex) {
                originalImage = createPlaceholder("Unable to render PDF receipt: " + ex.getMessage());
            }
        } else {
            try {
                originalImage = ImageIO.read(new ByteArrayInputStream(data));
                if (originalImage == null) {
                    originalImage = createPlaceholder("Unsupported image format");
                }
            } catch (IOException ex) {
                originalImage = createPlaceholder("Failed to read image: " + ex.getMessage());
            }
        }
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        bar.setBackground(ThemeColors.WHITE);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeColors.BORDER));

        JButton zoomInBtn = new JButton("Zoom In (+)");
        zoomInBtn.addActionListener(e -> {
            zoomFactor = Math.min(zoomFactor * 1.25, 5.0);
            updateView();
        });

        JButton zoomOutBtn = new JButton("Zoom Out (-)");
        zoomOutBtn.addActionListener(e -> {
            zoomFactor = Math.max(zoomFactor / 1.25, 0.2);
            updateView();
        });

        JButton fitBtn = new JButton("Fit to Window");
        fitBtn.addActionListener(e -> fitToWindow());

        JButton rotateLeftBtn = new JButton("Rotate Left (↺)");
        rotateLeftBtn.addActionListener(e -> {
            rotationDegrees = (rotationDegrees - 90 + 360) % 360;
            updateView();
        });

        JButton rotateRightBtn = new JButton("Rotate Right (↻)");
        rotateRightBtn.addActionListener(e -> {
            rotationDegrees = (rotationDegrees + 90) % 360;
            updateView();
        });

        JButton resetBtn = new JButton("Reset");
        resetBtn.addActionListener(e -> {
            zoomFactor = 1.0;
            rotationDegrees = 0;
            updateView();
        });

        bar.add(zoomInBtn);
        bar.add(zoomOutBtn);
        bar.add(fitBtn);
        bar.add(Box.createHorizontalStrut(10));
        bar.add(rotateLeftBtn);
        bar.add(rotateRightBtn);
        bar.add(resetBtn);

        return bar;
    }

    private void fitToWindow() {
        if (originalImage == null) return;
        Dimension vp = getContentPane().getSize();
        int availableW = Math.max(vp.width - 60, 200);
        int availableH = Math.max(vp.height - 120, 200);

        boolean isSideways = (rotationDegrees == 90 || rotationDegrees == 270);
        int imgW = isSideways ? originalImage.getHeight() : originalImage.getWidth();
        int imgH = isSideways ? originalImage.getWidth() : originalImage.getHeight();

        double scaleW = (double) availableW / imgW;
        double scaleH = (double) availableH / imgH;
        zoomFactor = Math.min(scaleW, scaleH);
        if (zoomFactor > 1.5) zoomFactor = 1.0;
        updateView();
    }

    private void updateView() {
        statusLabel.setText(String.format("Zoom: %d%% | Rotation: %d°", (int) (zoomFactor * 100), rotationDegrees));
        canvas.revalidate();
        canvas.repaint();
    }

    private BufferedImage createPlaceholder(String text) {
        BufferedImage img = new BufferedImage(500, 300, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(0xF8, 0xFA, 0xFC));
        g.fillRect(0, 0, 500, 300);
        g.setColor(new Color(0x64, 0x74, 0x8B));
        g.setFont(ThemeColors.FONT_SUBHEADING);
        g.drawString(text, 40, 150);
        g.dispose();
        return img;
    }

    private final class ImageCanvas extends JPanel {
        @Override
        public Dimension getPreferredSize() {
            if (originalImage == null) return new Dimension(400, 300);
            boolean isSideways = (rotationDegrees == 90 || rotationDegrees == 270);
            int w = (int) ((isSideways ? originalImage.getHeight() : originalImage.getWidth()) * zoomFactor);
            int h = (int) ((isSideways ? originalImage.getWidth() : originalImage.getHeight()) * zoomFactor);
            return new Dimension(w + 40, h + 40);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (originalImage == null) return;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Dimension size = getSize();
            boolean isSideways = (rotationDegrees == 90 || rotationDegrees == 270);
            int targetW = (int) ((isSideways ? originalImage.getHeight() : originalImage.getWidth()) * zoomFactor);
            int targetH = (int) ((isSideways ? originalImage.getWidth() : originalImage.getHeight()) * zoomFactor);

            int x = Math.max((size.width - targetW) / 2, 20);
            int y = Math.max((size.height - targetH) / 2, 20);

            AffineTransform tx = new AffineTransform();
            tx.translate(x + targetW / 2.0, y + targetH / 2.0);
            tx.rotate(Math.toRadians(rotationDegrees));
            tx.scale(zoomFactor, zoomFactor);
            tx.translate(-originalImage.getWidth() / 2.0, -originalImage.getHeight() / 2.0);

            g2.drawImage(originalImage, tx, null);
            g2.dispose();
        }
    }
}
