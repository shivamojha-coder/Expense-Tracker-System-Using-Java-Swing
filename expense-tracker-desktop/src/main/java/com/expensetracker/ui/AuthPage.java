package com.expensetracker.ui;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

/**
 * Split-screen sign-in / registration page: product pitch over a workspace photo on the left,
 * the credentials card on the right.
 */
public final class AuthPage extends JPanel {

    public enum Mode { LOGIN, REGISTER }

    /** Callbacks wired by the window; {@code forgotPassword} is only used in LOGIN mode. */
    public record Actions(Runnable submit, Runnable switchMode, Runnable back, Runnable forgotPassword) {
    }

    private static final Color C_INK = new Color(0x13, 0x1F, 0x1A);
    private static final Color C_BRAND = new Color(0x0A, 0x5E, 0x46);
    private static final Color C_BRAND_HOV = new Color(0x07, 0x4A, 0x37);
    private static final Color C_GREEN = new Color(0x0E, 0x8F, 0x5F);
    private static final Color C_TEXT2 = new Color(0x4F, 0x5F, 0x58);
    private static final Color C_FIELD_BORDER = new Color(0xD3, 0xDC, 0xD8);
    private static final Color C_TILE = new Color(0xE3, 0xF3, 0xEA);
    private static final double SPLIT = 0.542;
    /** Fraction of the laptop image's width occupied by its transparent left margin plus the bezel. */
    private static final double LAPTOP_BEZEL = 0.22;

    private final JTextField emailField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final AuthButton submitButton;
    private final JLabel statusLabel = new JLabel(" ");

    private final SmoothImage background = new SmoothImage(load("/images/auth-background.png"));
    private final SmoothImage panelBackground = new SmoothImage(load("/images/auth-panel-background.png"));
    private final SmoothImage laptop = new SmoothImage(load("/images/auth-laptop.png"));
    private final SmoothImage logo = new SmoothImage(load("/images/app-logo.png"));

    private final JPanel leftContent;
    private final RoundedCard card;
    private int laptopX;
    private int laptopW;

    public AuthPage(Mode mode, boolean cloudConfigured, Actions actions) {
        setLayout(null);
        setBackground(new Color(0xF4, 0xF8, 0xF5));
        boolean login = mode == Mode.LOGIN;

        submitButton = new AuthButton(login ? "Log in" : "Create account");
        submitButton.addActionListener(e -> actions.submit().run());
        emailField.addActionListener(e -> actions.submit().run());
        passwordField.addActionListener(e -> actions.submit().run());

        leftContent = buildLeft(actions);
        card = buildCard(login, cloudConfigured, actions);
        add(leftContent);
        add(card);
    }

    // ---------- public API used by the window ----------

    public String email() {
        return emailField.getText();
    }

    public char[] password() {
        return passwordField.getPassword();
    }

    public void clearPassword() {
        passwordField.setText("");
    }

    public void setBusy(boolean busy, String status) {
        submitButton.setEnabled(!busy);
        emailField.setEnabled(!busy);
        passwordField.setEnabled(!busy);
        statusLabel.setText(status == null || status.isBlank() ? " " : status);
    }

    // ---------- left column ----------

    private JPanel buildLeft(Actions actions) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.X_AXIS));
        brand.add(new ImageBox(logo, 58, 44));
        brand.add(Box.createHorizontalStrut(12));
        JLabel name = new JLabel("Expense Tracker");
        name.setFont(UiKit.medium(21f));
        name.setForeground(C_INK);
        brand.add(name);
        brand.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        brand.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                actions.back().run();
            }
        });
        p.add(left(brand));
        p.add(Box.createVerticalStrut(46));

        Font h = UiKit.font(Font.BOLD, 52f);
        p.add(left(new TextLine("Take Control", h, C_INK, 1.12f, -0.025f)));
        p.add(left(new SegmentLine(h, 1.12f, -0.025f, new String[]{"of Your ", "Expenses."}, new Color[]{C_INK, C_GREEN})));
        p.add(Box.createVerticalStrut(20));
        p.add(left(new WrappedText(
                "Track spending, organize your budget, and make smarter financial decisions — all in one simple, private desktop app.",
                UiKit.font(Font.PLAIN, 16.5f), C_TEXT2, 1.55f, 370, javax.swing.SwingConstants.LEFT)));
        p.add(Box.createVerticalStrut(26));
        p.add(left(new FeatureRow("chart", "Track Expenses", "See where your money goes.")));
        p.add(Box.createVerticalStrut(12));
        p.add(left(new FeatureRow("pie", "Visual Insights", "Understand your spending.")));
        p.add(Box.createVerticalStrut(12));
        p.add(left(new FeatureRow("lock", "Your Data, Private", "Offline mode,\nno credit card required.")));
        return p;
    }

    // ---------- credentials card ----------

    private RoundedCard buildCard(boolean login, boolean cloudConfigured, Actions actions) {
        RoundedCard c = new RoundedCard();
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        c.setBorder(new EmptyBorder(26, 40, 34, 40));

        c.add(centerRow(new ImageBox(logo, 84, 63)));
        c.add(Box.createVerticalStrut(4));
        c.add(centerRow(new TextLine("Expense Tracker", UiKit.font(Font.BOLD, 32f), C_INK, 1.2f, -0.015f)));
        c.add(Box.createVerticalStrut(4));
        c.add(centerRow(new TextLine(
                login ? "Sign in to your private financial workspace" : "Create your private financial workspace",
                UiKit.font(Font.PLAIN, 15.5f), C_TEXT2, 1.4f)));
        c.add(Box.createVerticalStrut(26));

        c.add(left(fieldLabel("Email address")));
        c.add(Box.createVerticalStrut(7));
        emailField.putClientProperty("JTextField.placeholderText", "Enter your email");
        c.add(left(new FieldBox("mail", emailField, false)));
        c.add(Box.createVerticalStrut(16));

        c.add(left(fieldLabel("Password")));
        c.add(Box.createVerticalStrut(7));
        passwordField.putClientProperty("JTextField.placeholderText", login ? "Enter your password" : "Create a password");
        c.add(left(new FieldBox("lock", passwordField, true)));

        if (login) {
            c.add(Box.createVerticalStrut(10));
            c.add(left(rightAligned(new LinkLabel("Forgot password?", 13.5f, actions.forgotPassword()))));
            c.add(Box.createVerticalStrut(14));
        } else {
            c.add(Box.createVerticalStrut(24));
        }

        submitButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.add(submitButton);

        statusLabel.setFont(UiKit.font(Font.PLAIN, 12.5f));
        statusLabel.setForeground(new Color(0xB4, 0x53, 0x09));
        statusLabel.setHorizontalAlignment(JLabel.CENTER);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        statusLabel.setPreferredSize(new Dimension(10, 22));
        c.add(statusLabel);

        c.add(Box.createVerticalStrut(18));
        c.add(centerRow(new LinkRow(
                login ? "New here?  " : "Already have an account?  ",
                login ? "Create an account" : "Log in",
                actions.switchMode())));

        if (!cloudConfigured) {
            c.add(Box.createVerticalStrut(10));
            JLabel hint = new JLabel("Cloud service not configured");
            hint.setFont(UiKit.font(Font.PLAIN, 11.5f));
            hint.setForeground(new Color(0xB4, 0x53, 0x09));
            c.add(centerRow(hint));
        }
        return c;
    }

    /** Full-width row that centres its content; keeps every card child on the same (left) alignment axis. */
    private static JPanel centerRow(JComponent content) {
        JPanel row = new JPanel(new java.awt.GridBagLayout());
        row.setOpaque(false);
        row.add(content);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, content.getPreferredSize().height));
        return row;
    }

    private static JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UiKit.medium(14f));
        l.setForeground(C_INK);
        return l;
    }

    private static JPanel rightAligned(JComponent c) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(c, BorderLayout.EAST);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        return p;
    }

    private static <T extends JComponent> T left(T c) {  // BoxLayout children share one alignment axis
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    private static BufferedImage load(String path) {
        URL url = AuthPage.class.getResource(path);
        if (url == null) {
            throw new IllegalStateException("Missing application resource: " + path);
        }
        try {
            return ImageIO.read(url);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load application resource: " + path, e);
        }
    }

    // ---------- layout and painting ----------

    @Override
    public void doLayout() {
        int w = getWidth();
        int h = getHeight();
        int split = (int) (w * SPLIT);

        Dimension lp = leftContent.getPreferredSize();
        int lx = Math.max(40, (int) (w * 0.048));
        leftContent.setBounds(lx, Math.max(26, (int) (h * 0.05)), Math.min(lp.width, split - lx - 10), lp.height);

        int cardW = Math.max(420, Math.min(540, (int) (w * 0.372) + 20));
        int cardH = card.getPreferredSize().height;
        card.setBounds(split + (w - split - cardW) / 2, Math.max(16, (h - cardH) / 2), cardW, cardH);

        // The laptop sits between the feature list and the card: wide when there is room, smaller when not.
        int textRight = lx;
        for (Component c : leftContent.getComponents()) {
            if (c instanceof FeatureRow) {
                textRight = Math.max(textRight, lx + c.getPreferredSize().width);
            }
        }
        int roomRight = card.getX() + 8;
        int fitWidth = (int) ((roomRight - (textRight + 18)) / (1 - LAPTOP_BEZEL));
        laptopW = Math.max(300, Math.min((int) (w * 0.45), fitWidth));
        laptopX = Math.max((int) (w * 0.13), textRight + 18 - (int) (LAPTOP_BEZEL * laptopW));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        UiKit.quality(g2);
        int w = getWidth();
        int h = getHeight();
        int split = (int) (w * SPLIT);

        Graphics2D left = (Graphics2D) g2.create();
        left.clipRect(0, 0, split, h);
        cover(left, background, 0, 0, split, h, 0.04, 1.0);
        left.dispose();

        Graphics2D right = (Graphics2D) g2.create();
        right.clipRect(split, 0, w - split, h);
        cover(right, panelBackground, split, 0, w - split, h, 1.0, 0.0);
        right.dispose();

        int lw = laptopW > 0 ? laptopW : (int) (w * 0.45);
        int lh = lw * laptop.height() / laptop.width();
        int lx = laptopW > 0 ? laptopX : (int) (w * 0.13);
        int ly = (int) (h * 0.945) - lh;
        laptop.drawShadow(g2, lx, ly, lw, lh, 22, 20, 0.30f);
        laptop.drawShadow(g2, lx, ly, lw, lh, 6, 8, 0.26f);
        laptop.draw(g2, lx, ly, lw, lh);
        g2.dispose();
    }

    private static void cover(Graphics2D g2, SmoothImage img, int x, int y, int w, int h, double ax, double ay) {
        double scale = Math.max(w / (double) img.width(), h / (double) img.height());
        int dw = (int) Math.ceil(img.width() * scale);
        int dh = (int) Math.ceil(img.height() * scale);
        img.draw(g2, x - (int) Math.round((dw - w) * ax), y - (int) Math.round((dh - h) * ay), dw, dh);
    }

    // ---------- components ----------

    private static final class RoundedCard extends JPanel {
        RoundedCard() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            UiKit.quality(g2);
            float x = 10;
            float y = 6;
            float w = getWidth() - 20;
            float h = getHeight() - 22;
            for (int i = 0; i < 8; i++) {
                g2.setColor(new Color(10, 50, 38, 11 - i));
                g2.fill(new RoundRectangle2D.Float(x - i, y + 6 - i + i * 0.5f, w + 2 * i, h + 2 * i, 26 + i, 26 + i));
            }
            g2.setColor(Color.WHITE);
            g2.fill(new RoundRectangle2D.Float(x, y, w, h, 26, 26));
            g2.dispose();
        }
    }

    private static final class ImageBox extends JComponent {
        private final SmoothImage image;
        private final int w;
        private final int h;

        ImageBox(SmoothImage image, int w, int h) {
            this.image = image;
            this.w = w;
            this.h = h;
            setOpaque(false);
            Dimension d = new Dimension(w, h);
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            image.draw(g2, 0, 0, w, h);
            g2.dispose();
        }
    }

    /** One heading line made of differently coloured segments. */
    private static final class SegmentLine extends JComponent {
        private final String[] parts;
        private final Color[] colors;
        private final float lineFactor;
        private final float trackingEm;

        SegmentLine(Font font, float lineFactor, float trackingEm, String[] parts, Color[] colors) {
            this.parts = parts;
            this.colors = colors;
            this.lineFactor = lineFactor;
            this.trackingEm = trackingEm;
            setFont(font);
            setOpaque(false);
        }

        private Font tracked() {
            return getFont().deriveFont(java.util.Map.of(java.awt.font.TextAttribute.TRACKING, trackingEm));
        }

        @Override
        public Dimension getPreferredSize() {
            double width = 0;
            java.awt.font.FontRenderContext frc = new java.awt.font.FontRenderContext(null, true, true);
            for (String part : parts) {
                width += new java.awt.font.TextLayout(part, tracked(), frc).getAdvance();
            }
            return new Dimension((int) Math.ceil(width) + 2, (int) Math.ceil(getFont().getSize2D() * lineFactor));
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            UiKit.quality(g2);
            float x = 1f;
            for (int i = 0; i < parts.length; i++) {
                java.awt.font.TextLayout layout = new java.awt.font.TextLayout(parts[i], tracked(), g2.getFontRenderContext());
                float baseline = (getHeight() - (layout.getAscent() + layout.getDescent())) / 2f + layout.getAscent();
                g2.setColor(colors[i]);
                layout.draw(g2, x, baseline);
                x += layout.getAdvance();
            }
            g2.dispose();
        }
    }

    private static final class FeatureRow extends JPanel {
        FeatureRow(String icon, String title, String subtitle) {
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            add(new Tile(icon));
            add(Box.createHorizontalStrut(14));
            JPanel text = new JPanel();
            text.setOpaque(false);
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
            JLabel t = new JLabel(title);
            t.setFont(UiKit.medium(16f));
            t.setForeground(C_INK);
            t.setAlignmentX(Component.LEFT_ALIGNMENT);
            text.add(t);
            text.add(Box.createVerticalStrut(1));
            for (String line : subtitle.split("\n")) {
                JLabel s = new JLabel(line);
                s.setFont(UiKit.font(Font.PLAIN, 13f));
                s.setForeground(C_TEXT2);
                s.setAlignmentX(Component.LEFT_ALIGNMENT);
                text.add(s);
            }
            add(text);
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }
    }

    private static final class Tile extends JComponent {
        private final String icon;

        Tile(String icon) {
            this.icon = icon;
            Dimension d = new Dimension(46, 46);
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            UiKit.quality(g2);
            g2.setColor(C_TILE);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));
            SvgIcons.paint(g2, icon, (getWidth() - 23) / 2.0, (getHeight() - 23) / 2.0, 23, C_BRAND);
            g2.dispose();
        }
    }

    /** Bordered input with a leading icon and, for passwords, a show/hide toggle. */
    private static final class FieldBox extends JPanel {
        private boolean focused;

        FieldBox(String icon, JTextField field, boolean passwordToggle) {
            setOpaque(false);
            setLayout(new BorderLayout(10, 0));
            setBorder(new EmptyBorder(0, 14, 0, 10));
            setPreferredSize(new Dimension(100, 46));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));

            field.setBorder(BorderFactory.createEmptyBorder());
            field.setOpaque(false);
            field.setFont(UiKit.font(Font.PLAIN, 15f));
            field.setForeground(C_INK);
            field.setCaretColor(C_INK);
            field.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    focused = true;
                    repaint();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    focused = false;
                    repaint();
                }
            });

            add(new IconView(icon, 20), BorderLayout.WEST);
            add(field, BorderLayout.CENTER);
            if (passwordToggle && field instanceof JPasswordField pw) {
                add(new EyeToggle(pw), BorderLayout.EAST);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            UiKit.quality(g2);
            RoundRectangle2D rr = new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, 12, 12);
            g2.setColor(Color.WHITE);
            g2.fill(rr);
            if (focused) {
                g2.setColor(new Color(0x2E, 0xCC, 0x8F, 60));
                g2.setStroke(new BasicStroke(4f));
                g2.draw(rr);
            }
            g2.setColor(focused ? C_GREEN : C_FIELD_BORDER);
            g2.setStroke(new BasicStroke(focused ? 1.6f : 1.2f));
            g2.draw(rr);
            g2.dispose();
        }
    }

    private static final class IconView extends JComponent {
        private final String icon;
        private final int size;

        IconView(String icon, int size) {
            this.icon = icon;
            this.size = size;
            Dimension d = new Dimension(size, size);
            setPreferredSize(d);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            UiKit.quality(g2);
            SvgIcons.paint(g2, icon, 0, (getHeight() - size) / 2.0, size, C_TEXT2);
            g2.dispose();
        }
    }

    private static final class EyeToggle extends JButton {
        private boolean shown;
        private boolean hover;

        EyeToggle(JPasswordField field) {
            char hidden = field.getEchoChar();
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setFocusable(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Show or hide password");
            setPreferredSize(new Dimension(30, 30));
            addActionListener(e -> {
                shown = !shown;
                field.setEchoChar(shown ? (char) 0 : hidden);
                repaint();
            });
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            UiKit.quality(g2);
            SvgIcons.paint(g2, shown ? "eye" : "eye-off", (getWidth() - 20) / 2.0, (getHeight() - 20) / 2.0, 20,
                    hover ? C_INK : C_TEXT2);
            g2.dispose();
        }
    }

    /** Full-width primary action button with a trailing arrow. */
    private static final class AuthButton extends JButton {
        private boolean hover;

        AuthButton(String text) {
            super(text);
            setFont(UiKit.font(Font.BOLD, 16f));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(100, 48));
            setMinimumSize(new Dimension(60, 48));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            UiKit.quality(g2);
            int w = getWidth();
            int h = getHeight();
            RoundRectangle2D rr = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 12, 12);
            g2.setColor(!isEnabled() ? new Color(0x7A, 0xA6, 0x97) : hover ? C_BRAND_HOV : C_BRAND);
            g2.fill(rr);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int iconSize = 20;
            int gap = 10;
            int x = (w - (fm.stringWidth(getText()) + iconSize + gap)) / 2;
            g2.setColor(Color.WHITE);
            g2.drawString(getText(), x, (h - (fm.getAscent() + fm.getDescent())) / 2 + fm.getAscent());
            SvgIcons.paint(g2, "arrow-right", x + fm.stringWidth(getText()) + gap, (h - iconSize) / 2.0, iconSize, Color.WHITE);
            g2.dispose();
        }
    }

    private static final class LinkLabel extends JComponent {
        private final String text;
        private boolean hover;

        LinkLabel(String text, float size, Runnable action) {
            this.text = text;
            setFont(UiKit.font(Font.PLAIN, size));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (action != null) {
                        action.run();
                    }
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(fm.stringWidth(text) + 2, fm.getHeight() + 2);
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            UiKit.quality(g2);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(hover ? C_BRAND : C_GREEN);
            int baseline = (getHeight() - (fm.getAscent() + fm.getDescent())) / 2 + fm.getAscent();
            g2.drawString(text, 0, baseline);
            g2.setStroke(new BasicStroke(1f));
            g2.drawLine(0, baseline + 2, fm.stringWidth(text), baseline + 2);
            g2.dispose();
        }
    }

    private static final class LinkRow extends JPanel {
        LinkRow(String prefix, String link, Runnable action) {
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            JLabel p = new JLabel(prefix);
            p.setFont(UiKit.font(Font.PLAIN, 14f));
            p.setForeground(C_TEXT2);
            add(p);
            add(new LinkLabel(link, 14f, action));
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }
    }
}
