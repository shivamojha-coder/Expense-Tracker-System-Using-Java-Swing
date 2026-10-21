package com.expensetracker.ui;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.LinearGradientPaint;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

/**
 * Landing page for Expense Tracker Desktop: navbar, hero, features, workflow,
 * security, benefits, call to action and footer. All artwork is drawn with
 * Java2D or taken from /images, and the layout tracks the viewport width.
 */
public final class LandingPage extends JPanel {

    private static final Color C_PAGE = new Color(0xF8, 0xFA, 0xF9);
    private static final Color C_WHITE = Color.WHITE;
    private static final Color C_INK = new Color(0x13, 0x1F, 0x1A);
    private static final Color C_DARK_TEAL = new Color(0x06, 0x3B, 0x2E);
    private static final Color C_BRAND = new Color(0x0A, 0x5E, 0x46);
    private static final Color C_BRAND_HOV = new Color(0x07, 0x4A, 0x37);
    private static final Color C_GREEN = new Color(0x0E, 0x8F, 0x5F);
    private static final Color C_MINT = new Color(0x2E, 0xCC, 0x8F);
    private static final Color C_TEXT2 = new Color(0x4F, 0x5F, 0x58);
    private static final Color C_TEXT3 = new Color(0x8A, 0x9B, 0x94);
    private static final Color C_BORDER = new Color(0xDD, 0xE7, 0xE2);
    private static final Color C_ICON_BG = new Color(0xE6, 0xF6, 0xEE);
    private static final Color C_ICON_BD = new Color(0xC6, 0xEA, 0xD8);
    private static final Color C_FOOTER = new Color(0x05, 0x2B, 0x22);

    private static Font font(int style, float size) {
        return UiKit.font(style, size);
    }

    private final Runnable onSignUp;
    private final Runnable onLogin;

    private JScrollPane scrollPane;
    private JPanel featuresAnchor;
    private JPanel howItWorksSection;
    private JPanel securitySection;
    private JPanel whySection;
    private JPanel ctaSection;

    public LandingPage(Runnable onSignUp, Runnable onLogin) {
        this.onSignUp = onSignUp;
        this.onLogin = onLogin;

        setLayout(new BorderLayout());
        setBackground(C_PAGE);

        ScrollableContentPanel content = new ScrollableContentPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(C_PAGE);

        content.add(buildNavbar());
        content.add(new HeroPanel());
        content.add(buildFeatures());
        content.add(buildHowItWorks());
        content.add(buildSecurity());
        content.add(buildWhyUsers());
        content.add(ctaSection = new CtaPanel());
        content.add(buildFooter());

        scrollPane = new JScrollPane(content);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(24);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getViewport().addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                content.revalidate();
                content.repaint();
            }
        });
        add(scrollPane, BorderLayout.CENTER);
    }

    JPanel getSecuritySection() {
        return securitySection;
    }

    // ==========================================
    // Navbar
    // ==========================================
    private JPanel buildNavbar() {
        JPanel bar = new JPanel(new GridBagLayout()) {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, 76);
            }
        };
        bar.setBackground(C_WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, C_BORDER),
                new EmptyBorder(0, 56, 0, 56)));
        bar.setPreferredSize(new Dimension(1200, 76));

        JPanel brand = row(10,
                new JLabel(new LogoIcon(new SmoothImage(loadImage("/images/app-logo.png")), 56, 42)),
                label("Expense Tracker", Font.BOLD, 17.5f, C_INK));

        JPanel links = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 0));
        links.setOpaque(false);
        links.add(navLink("Features", () -> scrollTo(featuresAnchor)));
        links.add(navLink("How It Works", () -> scrollTo(howItWorksSection)));
        links.add(navLink("Security", () -> scrollTo(securitySection)));
        links.add(navLink("Testimonials", () -> scrollTo(whySection)));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        actions.setOpaque(false);
        actions.add(new ActionButton("Log In", null, false, 90, 38, onLogin));
        actions.add(new ActionButton("Sign Up", null, true, 96, 38, onSignUp));

        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.weightx = 1.0;
        c.anchor = GridBagConstraints.WEST;
        bar.add(brand, c);
        c.gridx = 1;
        c.weightx = 0.0;
        c.anchor = GridBagConstraints.CENTER;
        bar.add(links, c);
        c.gridx = 2;
        c.weightx = 1.0;
        c.anchor = GridBagConstraints.EAST;
        bar.add(actions, c);
        return bar;
    }

    private JButton navLink(String text, Runnable action) {
        JButton btn = new JButton(text);
        btn.setFont(font(Font.PLAIN, 14.5f));
        btn.setForeground(C_INK);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> action.run());
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setForeground(C_GREEN);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setForeground(C_INK);
            }
        });
        return btn;
    }

    // ==========================================
    // Hero
    // ==========================================
    private final class HeroPanel extends JPanel {
        private static final int HEADER_GAP = 46;
        private static final int HERO_FADE = 56;
        private static final int BOTTOM_PAD = 34;
        private final SmoothImage background = new SmoothImage(loadImage("/images/hero-background.png"));
        private final SmoothImage laptop = new SmoothImage(loadImage("/images/hero-laptop-preview.png"));
        private final JPanel text = buildHeroText();
        private final JPanel header = buildFeaturesHeader();

        HeroPanel() {
            setLayout(null);
            setBackground(C_PAGE);
            add(text);
            add(header);
        }

        /** Height of the first screen: the viewport minus the navbar, so the hero fills the window. */
        private int top() {
            int viewport = scrollPane == null ? 0 : scrollPane.getViewport().getHeight();
            return viewport <= 0 ? 700 : Math.max(560, Math.min(1100, viewport - 76));
        }

        private int totalHeight() {
            return top() + HEADER_GAP + header.getPreferredSize().height + BOTTOM_PAD;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(1200, totalHeight());
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, totalHeight());
        }

        /**
         * Size and position of the laptop picture: as large as the window allows while its screen stays
         * clear of the hero text, and with its base resting on the tabletop of the background photo.
         * The screen starts about 24% into the image width, so its left edge is x + 0.243 * width.
         */
        private Rectangle laptopRect(int w, int top, int textRight) {
            double aspect = laptop.width() / (double) laptop.height();
            int rightMargin = (int) (w * 0.05);
            int clearOfText = (int) ((w - rightMargin - textRight - 16) / 0.757);
            int lw = (int) Math.min(Math.min(w * 0.76, clearOfText), top * 0.90 * aspect);
            lw = Math.max(lw, 320);
            int lh = (int) Math.round(lw / aspect);
            int lx = w - lw - rightMargin;

            // The tabletop begins ~80% down the photo; rest the base halfway down the visible table.
            double photoScale = Math.max(w / (double) background.width(), (top + HERO_FADE) / (double) background.height());
            double photoHeight = background.height() * photoScale;
            int tableEdge = (int) ((top + HERO_FADE) - photoHeight + 0.80 * photoHeight);
            int baseY = tableEdge + (int) ((top - tableEdge) * 0.50);
            int ly = baseY - (int) Math.round(lh * 0.983);   // the image has ~2% empty margin below the base
            return new Rectangle(lx, ly, lw, lh);
        }

        @Override
        public void doLayout() {
            Dimension tp = text.getPreferredSize();
            int x = Math.max(40, (int) (getWidth() * 0.055));
            int top = top();

            // The text block spans the laptop's screen: its top lines up with the top of the glass and its
            // bottom with the bottom of the glass, measured on the screen's left edge (the side facing the
            // text): about 12% to 86% of the picture's height.
            Rectangle laptopBounds = laptopRect(getWidth(), top, x + tp.width);
            int screenTop = laptopBounds.y + (int) (laptopBounds.height * 0.12);
            int screenBottom = laptopBounds.y + (int) (laptopBounds.height * 0.86);
            int height = Math.max(tp.height, screenBottom - screenTop);
            text.setBounds(x, (screenTop + screenBottom) / 2 - height / 2, tp.width, height);
            header.setBounds(0, top + HEADER_GAP, getWidth(), header.getPreferredSize().height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            int w = getWidth();
            // The photo (table included) is shown untouched across the first screen. Only a short fade
            // below that edge blends it into the page colour behind the "Everything You Need" heading.
            int top = top();
            g2.setClip(0, 0, w, top + HERO_FADE);
            drawCover(g2, background, w, top + HERO_FADE);
            g2.setPaint(new GradientPaint(0, top, new Color(0xF8, 0xFA, 0xF9, 0),
                    0, top + HERO_FADE, new Color(0xF8, 0xFA, 0xF9, 255)));
            g2.fillRect(0, top, w, HERO_FADE);
            g2.setClip(null);

            Rectangle laptopBounds = laptopRect(w, top, text.getX() + text.getWidth());
            int lx = laptopBounds.x;
            int ly = laptopBounds.y;
            int lw = laptopBounds.width;
            int lh = laptopBounds.height;
            int baseY = ly + (int) Math.round(lh * 0.983);   // lowest visible point of the laptop base

            // Soft contact shadow flattened onto the table, directly under the base.
            Graphics2D contact = (Graphics2D) g2.create();
            float radius = lw * 0.50f;
            contact.translate(lx + lw * 0.52, baseY - lh * 0.01);
            contact.scale(1.0, 0.09);
            contact.setPaint(new RadialGradientPaint(new Point2D.Float(0, 0), radius, new float[]{0f, 0.6f, 1f},
                    new Color[]{new Color(45, 28, 10, 190), new Color(45, 28, 10, 95), new Color(45, 28, 10, 0)}));
            contact.fill(new Ellipse2D.Float(-radius, -radius, radius * 2, radius * 2));
            contact.dispose();

            // Shadows only along the base (below the screen): a wider soft one and a tight dark one that
            // hugs the bottom edge, which is what makes the laptop look planted instead of hovering.
            // The soft one is built from stacked bands that start at different heights, so it fades in
            // gradually instead of ending at a visible horizontal edge.
            for (int band = 0; band < 6; band++) {
                Graphics2D soft = (Graphics2D) g2.create();
                soft.clipRect(lx - 40, ly + (int) (lh * (0.80 + band * 0.025)), lw + 80, lh);
                laptop.drawShadow(soft, lx, ly, lw, lh, 14, 10, 0.10f);
                soft.dispose();
            }
            Graphics2D tight = (Graphics2D) g2.create();
            tight.clipRect(lx - 40, ly + (int) (lh * 0.90), lw + 80, lh);
            laptop.drawShadow(tight, lx, ly, lw, lh, 5, 4, 0.72f);
            tight.dispose();
            laptop.draw(g2, lx, ly, lw, lh);
            g2.dispose();
        }
    }

    /** A vertical gap that keeps at least {@code minimum} pixels but grows when the text block is stretched. */
    private static Component flexGap(int minimum, int stretch) {
        return new Box.Filler(new Dimension(0, minimum), new Dimension(0, minimum), new Dimension(0, minimum + stretch));
    }

    private JPanel buildHeroText() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        p.add(left(new Badge("Desktop App", "monitor", C_GREEN, C_ICON_BG, C_ICON_BD, 12f)));
        p.add(flexGap(18, 60));
        p.add(left(new TextLine("Track Expenses", font(Font.BOLD, 48), C_INK, 1.1f, -0.025f)));
        p.add(left(new TextLine("Easily. Make", font(Font.BOLD, 48), C_INK, 1.1f, -0.025f)));
        p.add(left(new TextLine("Smarter Decisions.", font(Font.BOLD, 48), C_GREEN, 1.1f, -0.025f)));
        p.add(flexGap(16, 60));
        p.add(left(new WrappedText(
                "Scan receipts, track spending in real time, and get clear insights — all in a private, offline-capable desktop app.",
                font(Font.PLAIN, 16f), new Color(0x4A, 0x58, 0x52), 1.55f, 440, SwingConstants.LEFT)));
        p.add(flexGap(26, 60));

        p.add(left(row(12,
                new ActionButton("Download for Windows  →", "download", true, 226, 46, onSignUp),
                new ActionButton("See how it works", "play", false, 176, 46, () -> scrollTo(howItWorksSection)).softBorder())));
        p.add(flexGap(22, 60));

        Font tf = font(Font.PLAIN, 12.5f);
        Color tc = new Color(0x62, 0x70, 0x6A);
        p.add(left(row(22,
                new IconLabel("wifi", "Offline mode", tf, tc, tc, 15, 7),
                new IconLabel("card-off", "No credit card required", tf, tc, tc, 15, 7),
                new IconLabel("lock", "100% private", tf, tc, tc, 15, 7))));
        return p;
    }

    // ==========================================
    // Features
    // ==========================================
    private JPanel buildFeatures() {
        SectionPanel sec = new SectionPanel(C_PAGE, null);
        sec.setLayout(new BoxLayout(sec, BoxLayout.Y_AXIS));
        sec.setBorder(new EmptyBorder(0, 64, 64, 64));

        JPanel grid = new JPanel(new GridLayout(2, 3, 24, 24));
        grid.setOpaque(false);
        grid.add(featureCard("receipt", "Receipt OCR Scanning",
                "Scan or drop receipts and invoices. Built-in Tess4J automatically extracts merchant names, dates, and amounts."));
        grid.add(featureCard("chart", "Smart Visual Insights",
                "Interactive charts to understand where your money goes across categories, payment methods, and timelines."));
        grid.add(featureCard("file", "Executive PDF & CSV Reports",
                "Generate comprehensive financial summaries. Export to PDF or CSV ready for accountants and audits."));
        grid.add(featureCard("search", "Deep Search & Filters",
                "Find any transaction in milliseconds. Filter by category, payment method, date range, and custom notes."));
        grid.add(featureCard("cloud", "Instant Data Mobility",
                "Keep complete ownership of your data. Export your entire transaction archive or filtered sets whenever you need them."));
        grid.add(featureCard("shield", "Row-Level Security & Privacy",
                "Backed by Supabase Auth with strict Row-Level Security policies. Your data is encrypted and accessible only to you."));
        sec.add(grid);
        return sec;
    }

    private JPanel buildFeaturesHeader() {
        featuresAnchor = sectionHeader(null, "Everything You Need for Financial Control",
                "From intelligent receipt scanning to executive reports — Expense Tracker brings all the essential tools together in one place.",
                640);
        return featuresAnchor;
    }

    private JPanel featureCard(String icon, String title, String desc) {
        CardPanel card = new CardPanel(14, new Color(0xFC, 0xFD, 0xFC), C_BORDER, false);
        card.setHoverStyle(new Color(0xF2, 0xFA, 0xF5), C_MINT);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(26, 28, 26, 28));

        card.add(left(new IconBox(icon, 52, 26, false)));
        card.add(Box.createVerticalStrut(20));
        card.add(left(label(title, Font.BOLD, 17.5f, C_INK)));
        card.add(Box.createVerticalStrut(10));
        card.add(left(new WrappedText(desc, font(Font.PLAIN, 14f), C_TEXT2, 1.6f, 0, SwingConstants.LEFT)));
        card.add(Box.createVerticalGlue());
        return card;
    }

    // ==========================================
    // How it works
    // ==========================================
    private JPanel buildHowItWorks() {
        SectionPanel sec = new SectionPanel(new Color(0xF6, 0xFA, 0xF8), new Color(0xE8, 0xF2, 0xEC));
        howItWorksSection = sec;
        sec.setLayout(new BoxLayout(sec, BoxLayout.Y_AXIS));
        sec.setBorder(new EmptyBorder(62, 56, 62, 56));

        sec.add(sectionHeader("SIMPLE WORKFLOW", "How Expense Tracker Works",
                "Go from a receipt in your hand to meaningful financial insights in three simple steps.", 700));
        sec.add(Box.createVerticalStrut(36));

        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.gridy = 0;

        c.gridx = 0;
        c.weightx = 1;
        row.add(stepCard("01", "Capture", "Scan or add a receipt manually.", captureBody()), c);
        c.gridx = 1;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        row.add(new ArrowConnector(), c);
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 2;
        c.weightx = 1;
        row.add(stepCard("02", "Categorize", "Details are extracted automatically.", categorizeBody()), c);
        c.gridx = 3;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        row.add(new ArrowConnector(), c);
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 4;
        c.weightx = 1;
        row.add(stepCard("03", "Understand", "View charts, track budgets and export reports.", understandBody()), c);
        sec.add(row);
        return sec;
    }

    private JPanel stepCard(String number, String title, String subtitle, JComponent body) {
        CardPanel card = new CardPanel(14, C_WHITE, C_BORDER, true) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(300, super.getPreferredSize().height);
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(20, 22, 26, 22));

        card.add(left(new Badge(number, null, C_GREEN, C_ICON_BG, C_ICON_BD, 11f)));
        card.add(Box.createVerticalStrut(10));
        card.add(left(label(title, Font.BOLD, 19f, C_INK)));
        card.add(Box.createVerticalStrut(4));
        card.add(left(label(subtitle, Font.PLAIN, 13f, C_TEXT2)));
        card.add(Box.createVerticalStrut(14));
        body.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(body);
        return card;
    }

    private JComponent captureBody() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        ReceiptArt art = new ReceiptArt();
        art.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(art);
        p.add(Box.createVerticalStrut(12));
        ActionButton scan = new ActionButton("Scan Receipt", "scan", true, 200, 42, null).fullWidth();
        scan.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(scan);
        return p;
    }

    private JComponent categorizeBody() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE6, 0xEE, 0xEA), 1, true),
                new EmptyBorder(8, 12, 8, 12)));
        String[] labels = {"Merchant", "Date", "Amount", "Category", "Payment Method", "Notes"};
        JComponent[] values = {
                plainValue("Starbucks", false), plainValue("Sep 26, 2026", false), plainValue("₹340.00", true),
                new ValueBox("utensils", "Food & Dining", C_INK, true),
                new ValueBox("card", "Credit Card", C_INK, true),
                new ValueBox(null, "Add a note (optional)", C_TEXT3, false)
        };
        for (int i = 0; i < labels.length; i++) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridy = i;
            c.insets = new Insets(5, 0, 5, 8);
            c.anchor = GridBagConstraints.WEST;
            c.weightx = 0.36;
            form.add(label(labels[i], Font.PLAIN, 12f, C_TEXT2), c);
            c.gridx = 1;
            c.weightx = 0.64;
            c.insets = new Insets(5, 0, 5, 0);
            c.fill = GridBagConstraints.HORIZONTAL;
            form.add(values[i], c);
        }
        return form;
    }

    private JComponent plainValue(String text, boolean bold) {
        JLabel l = label(text, bold ? Font.BOLD : Font.PLAIN, 13f, C_INK);
        l.setBorder(new EmptyBorder(0, 2, 0, 0));
        return l;
    }

    private JComponent understandBody() {
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 8));
        p.setOpaque(false);
        p.add(new MonthlyChart());
        p.add(new CategoryDonut());
        return p;
    }

    // ==========================================
    // Security
    // ==========================================
    private JPanel buildSecurity() {
        SecurityPanel sec = new SecurityPanel();
        securitySection = sec;
        sec.setLayout(new GridBagLayout());
        sec.setBorder(new EmptyBorder(48, 56, 48, 32));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(Box.createVerticalGlue());

        // Pill badge "YOUR DATA, YOURS"
        JPanel badge = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                quality(g2);
                g2.setColor(new Color(0x06, 0x48, 0x38));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(new Color(0x34, 0xD3, 0x99, 70));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
            }
        };
        badge.setOpaque(false);
        badge.setLayout(new FlowLayout(FlowLayout.CENTER, 14, 5));
        JLabel badgeLabel = new JLabel("YOUR DATA, YOURS");
        badgeLabel.setFont(font(Font.BOLD, 11f));
        badgeLabel.setForeground(new Color(0x34, 0xD3, 0x99));
        badge.add(badgeLabel);
        badge.setMaximumSize(badge.getPreferredSize());
        text.add(left(badge));
        text.add(Box.createVerticalStrut(14));

        // Heading: white line 1, mint line 2
        text.add(left(new TextLine("Enterprise-Grade Security", font(Font.BOLD, 36f), C_WHITE, 1.15f, -0.02f)));
        text.add(left(new TextLine("& Complete Privacy", font(Font.BOLD, 36f), new Color(0x34, 0xD3, 0x99), 1.15f, -0.02f)));
        text.add(Box.createVerticalStrut(14));

        // Subtitle
        text.add(left(new WrappedText(
                "Every record, receipt scan, and category is protected with modern security standards. "
                        + "Only your authenticated account can access your data, and it never leaves your control.",
                font(Font.PLAIN, 15f), new Color(0xC3, 0xDF, 0xD5), 1.6f, 520, SwingConstants.LEFT)));
        text.add(Box.createVerticalStrut(26));

        // 3 Feature Cards
        JPanel cards = new JPanel(new GridLayout(1, 3, 16, 0));
        cards.setOpaque(false);
        cards.setMaximumSize(new Dimension(610, 120));
        cards.add(securityFeatureCard("lock",
                "Row-Level Security\n(RLS) Isolation",
                "Your data is isolated and\naccessible only to you."));
        cards.add(securityFeatureCard("database",
                "Encrypted Data\nat Rest & in Transit",
                "All sensitive information\nis encrypted using industry\nstandards."));
        cards.add(securityFeatureCard("laptop",
                "Local Offline\nDemo Mode",
                "Explore the app safely with\nsample data, no account\nrequired."));
        text.add(left(cards));
        text.add(Box.createVerticalGlue());

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.gridx = 0;
        c.weightx = 0.54;
        sec.add(text, c);
        c.gridx = 1;
        c.weightx = 0.46;
        sec.add(new ShieldArt(), c);
        return sec;
    }

    /** Creates a security feature card matching the horizontal header + squircle icon style. */
    private JPanel securityFeatureCard(String iconName, String title, String description) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                quality(g2);
                g2.setColor(new Color(0x06, 0x36, 0x2A, 210));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(new Color(0x34, 0xD3, 0x99, 45));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(14, 14, 12, 14));

        // Top row: icon box + title side-by-side
        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        topRow.setOpaque(false);
        topRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JComponent iconBox = new JComponent() {
            {
                Dimension d = new Dimension(34, 34);
                setPreferredSize(d);
                setMinimumSize(d);
                setMaximumSize(d);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                quality(g2);
                g2.setColor(new Color(0x03, 0x24, 0x1C));
                g2.fillRoundRect(0, 0, 34, 34, 10, 10);
                g2.setColor(new Color(0x34, 0xD3, 0x99, 70));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, 33, 33, 10, 10);
                SvgIcons.paint(g2, iconName, 8, 8, 18, new Color(0x34, 0xD3, 0x99));
                g2.dispose();
            }
        };
        topRow.add(iconBox);

        JPanel titleCol = new JPanel();
        titleCol.setOpaque(false);
        titleCol.setLayout(new BoxLayout(titleCol, BoxLayout.Y_AXIS));
        Font titleFont = font(Font.BOLD, 12f);
        Color titleColor = Color.WHITE;
        for (String line : title.split("\n")) {
            JLabel lbl = new JLabel(line);
            lbl.setFont(titleFont);
            lbl.setForeground(titleColor);
            lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            titleCol.add(lbl);
        }
        topRow.add(titleCol);

        card.add(topRow);
        card.add(Box.createVerticalStrut(10));

        Font descFont = font(Font.PLAIN, 10.5f);
        Color descColor = new Color(0xA0, 0xC4, 0xB6);
        for (String line : description.split("\n")) {
            JLabel lbl = new JLabel(line);
            lbl.setFont(descFont);
            lbl.setForeground(descColor);
            lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(lbl);
        }
        return card;
    }

    // ==========================================
    // Why users choose
    // ==========================================
    private JPanel buildWhyUsers() {
        SectionPanel sec = new SectionPanel(C_PAGE, null);
        whySection = sec;
        sec.setLayout(new BoxLayout(sec, BoxLayout.Y_AXIS));
        sec.setBorder(new EmptyBorder(60, 64, 60, 64));
        sec.add(sectionHeader("BUILT FOR REAL USE", "Why Users Choose Expense Tracker", null, 0));
        sec.add(Box.createVerticalStrut(32));

        JPanel grid = new JPanel(new GridLayout(1, 3, 24, 0));
        grid.setOpaque(false);
        grid.add(whyCard("bolt", "Save Time", "Automate receipt entry and categorization with OCR."));
        grid.add(whyCard("pie", "Stay Organized", "Get a clear view of your spending and budgets."));
        grid.add(whyCard("shield", "Maintain Privacy", "Your data stays private, secure, and under your control."));
        sec.add(grid);
        return sec;
    }

    private JPanel whyCard(String icon, String title, String desc) {
        CardPanel card = new CardPanel(14, C_WHITE, C_BORDER, false);
        card.setHoverStyle(new Color(0xF2, 0xFA, 0xF5), C_MINT);
        card.setLayout(new BorderLayout(18, 0));
        card.setBorder(new EmptyBorder(26, 26, 26, 26));
        JPanel iconWrap = new JPanel(new GridBagLayout());
        iconWrap.setOpaque(false);
        iconWrap.add(new IconBox(icon, 56, 26, true));
        card.add(iconWrap, BorderLayout.WEST);

        JPanel t = new JPanel();
        t.setOpaque(false);
        t.setLayout(new BoxLayout(t, BoxLayout.Y_AXIS));
        t.add(left(label(title, Font.BOLD, 17.5f, C_INK)));
        t.add(Box.createVerticalStrut(6));
        t.add(left(new WrappedText(desc, font(Font.PLAIN, 14f), C_TEXT2, 1.6f, 0, SwingConstants.LEFT)));
        card.add(t, BorderLayout.CENTER);
        return card;
    }

    // ==========================================
    // Final call to action
    // ==========================================
    private final class CtaPanel extends JPanel {
        private static final int HEIGHT = 340;
        private final SmoothImage background = new SmoothImage(loadImage("/images/cta-background.png"));
        private final SmoothImage laptop = new SmoothImage(loadImage("/images/hero-laptop-preview.png"));
        private final JPanel text = buildCtaText();

        CtaPanel() {
            setLayout(null);
            setBackground(new Color(0xEA, 0xF3, 0xEE));
            add(text);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(1200, HEIGHT);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, HEIGHT);
        }

        @Override
        public void doLayout() {
            Dimension tp = text.getPreferredSize();
            int x = Math.max(56, (int) (getWidth() * 0.06));
            text.setBounds(x, (getHeight() - tp.height) / 2, tp.width, tp.height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            int w = getWidth();
            int h = getHeight();
            drawCover(g2, background, w, h);
            int lh = (int) (h * 0.86);
            int lw = lh * laptop.width() / laptop.height();
            int lx = (int) (w * 0.875) - lw;
            int ly = h - lh - (int) (h * 0.07);
            int baseY = ly + (int) Math.round(lh * 0.983);

            // Soft contact shadow flattened onto the table directly under the laptop base
            Graphics2D contact = (Graphics2D) g2.create();
            float radius = lw * 0.52f;
            contact.translate(lx + lw * 0.52, baseY - lh * 0.008);
            contact.scale(1.0, 0.085);
            contact.setPaint(new RadialGradientPaint(
                    new Point2D.Float(0, 0), radius, new float[]{0f, 0.45f, 0.8f, 1f},
                    new Color[]{
                            new Color(30, 22, 14, 160),
                            new Color(35, 25, 16, 95),
                            new Color(40, 30, 20, 35),
                            new Color(40, 30, 20, 0)
                    }));
            contact.fill(new Ellipse2D.Float(-radius, -radius, radius * 2, radius * 2));
            contact.dispose();

            // A tighter contact line shadow right along the bottom metal edge touching the desk
            Graphics2D tightContact = (Graphics2D) g2.create();
            float tightRadius = lw * 0.46f;
            tightContact.translate(lx + lw * 0.51, baseY);
            tightContact.scale(1.0, 0.035);
            tightContact.setPaint(new RadialGradientPaint(
                    new Point2D.Float(0, 0), tightRadius, new float[]{0f, 0.5f, 1f},
                    new Color[]{
                            new Color(20, 15, 10, 190),
                            new Color(25, 18, 12, 90),
                            new Color(25, 18, 12, 0)
                    }));
            tightContact.fill(new Ellipse2D.Float(-tightRadius, -tightRadius, tightRadius * 2, tightRadius * 2));
            tightContact.dispose();

            // Shadows along the base only (fading in smoothly without vertical edges)
            for (int band = 0; band < 5; band++) {
                Graphics2D soft = (Graphics2D) g2.create();
                soft.clipRect(lx - 30, ly + (int) (lh * (0.82 + band * 0.025)), lw + 60, lh);
                laptop.drawShadow(soft, lx, ly, lw, lh, 12, 8, 0.12f);
                soft.dispose();
            }
            Graphics2D tight = (Graphics2D) g2.create();
            tight.clipRect(lx - 30, ly + (int) (lh * 0.90), lw + 60, lh);
            laptop.drawShadow(tight, lx, ly, lw, lh, 4, 3, 0.50f);
            tight.dispose();

            laptop.draw(g2, lx, ly, lw, lh);
            g2.dispose();
        }
    }

    private JPanel buildCtaText() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(left(new TextLine("Ready to Take Control", font(Font.BOLD, 38f), C_INK, 1.15f, -0.02f)));
        p.add(left(new TextLine("of Your Financial Life?", font(Font.BOLD, 38f), C_INK, 1.15f, -0.02f)));
        p.add(Box.createVerticalStrut(12));
        p.add(left(new WrappedText(
                "Start tracking spending, scanning receipts with OCR, and making smarter financial decisions today.",
                font(Font.PLAIN, 16f), C_TEXT2, 1.6f, 440, SwingConstants.LEFT)));
        p.add(Box.createVerticalStrut(24));

        p.add(left(row(14,
                new ActionButton("Download for Windows  →", null, true, 230, 48, onSignUp),
                new ActionButton("Log In", null, false, 130, 48, onLogin))));
        return p;
    }

    // ==========================================
    // Footer
    // ==========================================
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(0, 22)) {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        footer.setBackground(C_FOOTER);
        footer.setBorder(new EmptyBorder(38, 64, 26, 64));

        JPanel top = new JPanel(new GridBagLayout());
        top.setOpaque(false);

        JPanel names = new JPanel();
        names.setOpaque(false);
        names.setLayout(new BoxLayout(names, BoxLayout.Y_AXIS));
        names.add(left(label("Expense Tracker", Font.BOLD, 17f, C_WHITE)));
        names.add(Box.createVerticalStrut(2));
        names.add(left(label("A private, offline-capable desktop app for smarter personal finance.",
                Font.PLAIN, 11.5f, new Color(0x9D, 0xBB, 0xAF))));
        JPanel brand = row(12,
                new JLabel(new LogoIcon(new SmoothImage(loadImage("/images/app-logo.png")), 56, 42)), names);

        JPanel links = new JPanel(new FlowLayout(FlowLayout.CENTER, 34, 0));
        links.setOpaque(false);
        links.add(footerLink("Features", () -> scrollTo(featuresAnchor)));
        links.add(footerLink("How It Works", () -> scrollTo(howItWorksSection)));
        links.add(footerLink("Security", () -> scrollTo(securitySection)));
        links.add(footerLink("FAQ", () -> scrollTo(ctaSection)));

        JPanel social = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        social.setOpaque(false);
        social.add(new SocialIcon("github"));
        social.add(new SocialIcon("linkedin"));
        social.add(new SocialIcon("twitter"));

        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 1;
        c.anchor = GridBagConstraints.WEST;
        top.add(brand, c);
        c.gridx = 1;
        c.weightx = 0;
        c.anchor = GridBagConstraints.CENTER;
        top.add(links, c);
        c.gridx = 2;
        c.weightx = 1;
        c.anchor = GridBagConstraints.EAST;
        top.add(social, c);
        footer.add(top, BorderLayout.NORTH);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0x12, 0x45, 0x38)),
                new EmptyBorder(16, 0, 0, 0)));
        Color dim = new Color(0x86, 0xA8, 0x9A);
        bottom.add(label("© 2026 Expense Tracker Desktop. All rights reserved.", Font.PLAIN, 11.5f, dim), BorderLayout.WEST);
        bottom.add(label("Java 22  •  Swing  •  Supabase  •  Tess4J OCR  •  JFreeChart", Font.PLAIN, 11.5f, dim),
                BorderLayout.EAST);
        footer.add(bottom, BorderLayout.SOUTH);
        return footer;
    }

    private JLabel footerLink(String text, Runnable action) {
        Color base = new Color(0xD6, 0xE9, 0xE0);
        JLabel l = label(text, Font.PLAIN, 14f, base);
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        l.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                action.run();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                l.setForeground(C_MINT);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                l.setForeground(base);
            }
        });
        return l;
    }

    // ==========================================
    // Shared helpers
    // ==========================================
    private JPanel sectionHeader(String eyebrow, String title, String subtitle, int subtitleWidth) {
        JPanel h = new JPanel();
        h.setOpaque(false);
        h.setLayout(new BoxLayout(h, BoxLayout.Y_AXIS));
        if (eyebrow != null) {
            h.add(centered(new TextLine(eyebrow, font(Font.BOLD, 12f), C_GREEN, 1.4f, 0.14f)));
            h.add(Box.createVerticalStrut(10));
        }
        h.add(centered(new TextLine(title, font(Font.BOLD, 38f), C_INK, 1.2f, -0.02f)));
        if (subtitle != null) {
            h.add(Box.createVerticalStrut(12));
            h.add(centered(new WrappedText(subtitle, font(Font.PLAIN, 16.5f), C_TEXT2, 1.6f, subtitleWidth,
                    SwingConstants.CENTER)));
        }
        return h;
    }

    private static JLabel label(String text, int style, float size, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font(style, size));
        l.setForeground(color);
        return l;
    }

    private static JPanel row(int gap, Component... items) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        for (int i = 0; i < items.length; i++) {
            if (i > 0) {
                p.add(Box.createHorizontalStrut(gap));
            }
            p.add(items[i]);
        }
        return p;
    }

    private static <T extends JComponent> T left(T c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    private static <T extends JComponent> T centered(T c) {
        c.setAlignmentX(Component.CENTER_ALIGNMENT);
        return c;
    }

    private static BufferedImage loadImage(String resourcePath) {
        URL resource = LandingPage.class.getResource(resourcePath);
        if (resource == null) {
            throw new IllegalStateException("Missing application resource: " + resourcePath);
        }
        try {
            return ImageIO.read(resource);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load application resource: " + resourcePath, exception);
        }
    }

    private static void drawCover(Graphics2D g2, SmoothImage image, int w, int h) {
        double scale = Math.max(w / (double) image.width(), h / (double) image.height());
        int dw = (int) Math.ceil(image.width() * scale);
        int dh = (int) Math.ceil(image.height() * scale);
        image.draw(g2, (w - dw) / 2, h - dh, dw, dh);
    }

    private static void quality(Graphics2D g2) {
        UiKit.quality(g2);
    }

    private void scrollTo(JComponent target) {
        if (target == null || scrollPane == null) {
            return;
        }
        Component view = scrollPane.getViewport().getView();
        int max = Math.max(0, view.getHeight() - scrollPane.getViewport().getHeight());
        int y = javax.swing.SwingUtilities.convertPoint(target, 0, 0, view).y - (target == featuresAnchor ? 40 : 0);
        scrollPane.getViewport().setViewPosition(new Point(0, Math.max(0, Math.min(y, max))));
    }

    // ==========================================
    // Layout / text components
    // ==========================================
    private static final class ScrollableContentPanel extends JPanel implements Scrollable {
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 24;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 80;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    /** Full-width section with a flat or vertical-gradient background that never stretches vertically. */
    private static class SectionPanel extends JPanel {
        private final Color top;
        private final Color bottom;

        SectionPanel(Color top, Color bottom) {
            this.top = top;
            this.bottom = bottom;
            setOpaque(true);
            setBackground(top);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (bottom == null) {
                super.paintComponent(g);
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), bottom));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }

    /** Rounded card with optional soft shadow and hover tint. */
    private static class CardPanel extends JPanel {
        private final int radius;
        private final Color fill;
        private final Color border;
        private final boolean shadow;
        private Color hoverFill;
        private Color hoverBorder;
        private boolean hover;

        CardPanel(int radius, Color fill, Color border, boolean shadow) {
            this.radius = radius;
            this.fill = fill;
            this.border = border;
            this.shadow = shadow;
            setOpaque(false);
        }

        void setHoverStyle(Color hoverFill, Color hoverBorder) {
            this.hoverFill = hoverFill;
            this.hoverBorder = hoverBorder;
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
            quality(g2);
            float x = shadow ? 3 : 0.5f;
            float y = shadow ? 1 : 0.5f;
            float w = getWidth() - (shadow ? 7 : 1);
            float h = getHeight() - (shadow ? 9 : 1);
            if (shadow) {
                for (int i = 0; i < 4; i++) {
                    g2.setColor(new Color(6, 40, 30, 9 - i * 2));
                    g2.fill(new RoundRectangle2D.Float(x - i, y + 3 - i + 0f, w + 2 * i, h + 2 * i, radius + i, radius + i));
                }
            }
            RoundRectangle2D rr = new RoundRectangle2D.Float(x, y, w, h, radius, radius);
            g2.setColor(hover && hoverFill != null ? hoverFill : fill);
            g2.fill(rr);
            g2.setColor(hover && hoverBorder != null ? hoverBorder : border);
            g2.setStroke(new BasicStroke(1f));
            g2.draw(rr);
            g2.dispose();
        }
    }

    private static final class ActionButton extends JButton {
        private final boolean filled;
        private final String icon;
        private boolean hover;
        private boolean softBorder;

        ActionButton(String text, String icon, boolean filled, int width, int height, Runnable action) {
            super(text);
            this.filled = filled;
            this.icon = icon;
            setFont(font(Font.BOLD, 14.5f));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            Dimension d = new Dimension(width, height);
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
            if (action != null) {
                addActionListener(e -> action.run());
            }
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

        ActionButton fullWidth() {
            setMaximumSize(new Dimension(Integer.MAX_VALUE, getPreferredSize().height));
            return this;
        }

        /** Light grey-green outline instead of the strong brand-green one (secondary hero action). */
        ActionButton softBorder() {
            this.softBorder = true;
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            quality(g2);
            int w = getWidth();
            int h = getHeight();
            int arc = 14;
            if (filled) {
                g2.setColor(hover ? C_BRAND_HOV : C_BRAND);
                g2.fillRoundRect(0, 0, w, h, arc, arc);
            } else {
                g2.setColor(hover ? new Color(0xF0, 0xF9, 0xF4) : C_WHITE);
                g2.fillRoundRect(0, 0, w, h, arc, arc);
                g2.setColor(softBorder ? new Color(0xC9, 0xDB, 0xD2) : C_BRAND);
                g2.setStroke(new BasicStroke(softBorder ? 1.2f : 1.5f));
                g2.drawRoundRect(1, 1, w - 3, h - 3, arc - 2, arc - 2);
            }
            Color fg = filled ? C_WHITE : C_DARK_TEAL;
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int iconSize = 16;
            int gap = 9;
            int content = fm.stringWidth(getText()) + (icon != null ? iconSize + gap : 0);
            int x = (w - content) / 2;
            if (icon != null) {
                SvgIcons.paint(g2, icon, x, (h - iconSize) / 2.0, iconSize, fg);
                x += iconSize + gap;
            }
            g2.setColor(fg);
            g2.drawString(getText(), x, (h - (fm.getAscent() + fm.getDescent())) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    private static final class Badge extends JComponent {
        private final String text;
        private final String icon;
        private final Color fg;
        private final Color bg;
        private final Color bd;

        Badge(String text, String icon, Color fg, Color bg, Color bd, float size) {
            this.text = text;
            this.icon = icon;
            this.fg = fg;
            this.bg = bg;
            this.bd = bd;
            setFont(font(Font.BOLD, size));
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(fm.stringWidth(text) + (icon != null ? 20 : 0) + 24, 28);
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            quality(g2);
            RoundRectangle2D rr = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 10, 10);
            g2.setColor(bg);
            g2.fill(rr);
            g2.setColor(bd);
            g2.draw(rr);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int x = 12;
            if (icon != null) {
                SvgIcons.paint(g2, icon, x, (getHeight() - 14) / 2.0, 14, fg);
                x += 20;
            }
            g2.setColor(fg);
            g2.drawString(text, x, (getHeight() - (fm.getAscent() + fm.getDescent())) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    /** Small icon followed by one or more text lines (separated by '\n'). */
    private static final class IconLabel extends JComponent {
        private final String icon;
        private final String[] lines;
        private final Color iconColor;
        private final int iconSize;
        private final int gap;

        IconLabel(String icon, String text, Font font, Color textColor, Color iconColor, int iconSize, int gap) {
            this.icon = icon;
            this.lines = text.split("\n");
            this.iconColor = iconColor;
            this.iconSize = iconSize;
            this.gap = gap;
            setFont(font);
            setForeground(textColor);
            setOpaque(false);
        }

        private int lineHeight() {
            return Math.round(getFont().getSize2D() * 1.4f);
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            int widest = 0;
            for (String l : lines) {
                widest = Math.max(widest, fm.stringWidth(l));
            }
            return new Dimension(iconSize + gap + widest + 2, Math.max(iconSize, lines.length * lineHeight()));
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
            quality(g2);
            SvgIcons.paint(g2, icon, 0, (getHeight() - iconSize) / 2.0, iconSize, iconColor);
            g2.setFont(getFont());
            g2.setColor(getForeground());
            FontMetrics fm = g2.getFontMetrics();
            int lh = lineHeight();
            int y = (getHeight() - lines.length * lh) / 2;
            for (String l : lines) {
                g2.drawString(l, iconSize + gap, y + (lh - (fm.getAscent() + fm.getDescent())) / 2 + fm.getAscent());
                y += lh;
            }
            g2.dispose();
        }
    }

    private static final class IconBox extends JComponent {
        private final String icon;
        private final int box;
        private final int iconSize;
        private final boolean circle;

        IconBox(String icon, int box, int iconSize, boolean circle) {
            this.icon = icon;
            this.box = box;
            this.iconSize = iconSize;
            this.circle = circle;
            Dimension d = new Dimension(box, box);
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            quality(g2);
            Shape s = circle
                    ? new Ellipse2D.Float(0.5f, 0.5f, box - 1, box - 1)
                    : new RoundRectangle2D.Float(0.5f, 0.5f, box - 1, box - 1, 14, 14);
            g2.setColor(C_ICON_BG);
            g2.fill(s);
            g2.setColor(C_ICON_BD);
            g2.draw(s);
            SvgIcons.paint(g2, icon, (box - iconSize) / 2.0, (box - iconSize) / 2.0, iconSize, C_BRAND);
            g2.dispose();
        }
    }

    private static final class ArrowConnector extends JComponent {
        ArrowConnector() {
            setPreferredSize(new Dimension(52, 24));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            quality(g2);
            g2.setColor(C_BRAND);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cy = getHeight() / 2;
            int x0 = 12;
            int x1 = getWidth() - 12;
            g2.drawLine(x0, cy, x1, cy);
            g2.drawLine(x1 - 6, cy - 6, x1, cy);
            g2.drawLine(x1 - 6, cy + 6, x1, cy);
            g2.dispose();
        }
    }

    private static final class SocialIcon extends JComponent {
        private final String type;

        SocialIcon(String type) {
            this.type = type;
            setPreferredSize(new Dimension(22, 22));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            quality(g2);
            SvgIcons.paint(g2, type, 0, 0, 22, new Color(0xE6, 0xF2, 0xEC));
            g2.dispose();
        }
    }

    /** Dropdown-style value box used in the "Categorize" mock form. */
    private static final class ValueBox extends JComponent {
        private final String icon;
        private final String text;
        private final Color color;
        private final boolean chevron;

        ValueBox(String icon, String text, Color color, boolean chevron) {
            this.icon = icon;
            this.text = text;
            this.color = color;
            this.chevron = chevron;
            setFont(font(Font.PLAIN, 12f));
            setPreferredSize(new Dimension(120, 30));
            setMinimumSize(new Dimension(60, 30));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            quality(g2);
            RoundRectangle2D rr = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 8, 8);
            g2.setColor(C_WHITE);
            g2.fill(rr);
            g2.setColor(new Color(0xD5, 0xE0, 0xDB));
            g2.draw(rr);
            int x = 10;
            if (icon != null) {
                SvgIcons.paint(g2, icon, x, (getHeight() - 14) / 2.0, 14, C_BRAND);
                x += 22;
            }
            g2.setFont(getFont());
            g2.setColor(color);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, x, (getHeight() - (fm.getAscent() + fm.getDescent())) / 2 + fm.getAscent());
            if (chevron) {
                g2.setColor(C_TEXT2);
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = getWidth() - 16;
                int cy = getHeight() / 2;
                g2.drawLine(cx - 3, cy - 1, cx, cy + 2);
                g2.drawLine(cx, cy + 2, cx + 3, cy - 1);
            }
            g2.dispose();
        }
    }

    private static final class LogoIcon implements Icon {
        private final SmoothImage image;
        private final int w;
        private final int h;

        LogoIcon(SmoothImage image, int w, int h) {
            this.image = image;
            this.w = w;
            this.h = h;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            image.draw(g2, x, y, w, h);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return w;
        }

        @Override
        public int getIconHeight() {
            return h;
        }
    }

    // ==========================================
    // Illustrations
    // ==========================================
    private static final class ReceiptArt extends JComponent {
        ReceiptArt() {
            setPreferredSize(new Dimension(240, 190));
            setMinimumSize(new Dimension(120, 190));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 190));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            quality(g2);
            int w = getWidth();
            int h = getHeight();
            Shape clip = new RoundRectangle2D.Float(0, 0, w, h, 12, 12);
            g2.setClip(clip);
            g2.setPaint(new GradientPaint(0, 0, new Color(0xDC, 0xC8, 0xA9), w, h, new Color(0xB8, 0x9E, 0x78)));
            g2.fill(clip);

            int rw = (int) (w * 0.52);
            int rh = (int) (h * 0.80);
            int rx = (w - rw) / 2;
            int ry = (h - rh) / 2;
            g2.setColor(new Color(0, 0, 0, 45));
            g2.fillRoundRect(rx + 3, ry + 5, rw, rh, 4, 4);
            g2.setColor(new Color(0xFC, 0xFB, 0xF7));
            g2.fillRect(rx, ry, rw, rh);

            g2.setColor(new Color(0x2A, 0x2A, 0x2A));
            g2.setFont(font(Font.BOLD, 11f));
            centerString(g2, "STARBUCKS", rx + rw / 2, ry + 24);
            g2.setFont(font(Font.PLAIN, 8.5f));
            g2.setColor(new Color(0x70, 0x70, 0x70));
            centerString(g2, "Coffee Shop", rx + rw / 2, ry + 37);

            g2.setColor(new Color(0xD8, 0xD8, 0xD8));
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[]{3f, 3f}, 0f));
            g2.drawLine(rx + 10, ry + 46, rx + rw - 10, ry + 46);
            g2.setStroke(new BasicStroke(1f));
            int[] widths = {58, 44, 52};
            int ly = ry + 58;
            for (int lw : widths) {
                g2.setColor(new Color(0xE2, 0xE2, 0xE2));
                g2.fillRoundRect(rx + 10, ly, (int) (lw * rw / 100.0 * 1.2), 5, 3, 3);
                g2.fillRoundRect(rx + rw - 28, ly, 18, 5, 3, 3);
                ly += 13;
            }
            g2.setColor(new Color(0xD8, 0xD8, 0xD8));
            g2.drawLine(rx + 10, ly + 2, rx + rw - 10, ly + 2);
            g2.setColor(new Color(0x2A, 0x2A, 0x2A));
            g2.setFont(font(Font.BOLD, 10f));
            g2.drawString("Total", rx + 10, ly + 20);
            g2.setFont(font(Font.BOLD, 12f));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString("₹340.00", rx + rw - 10 - fm.stringWidth("₹340.00"), ly + 20);

            g2.setClip(null);
            g2.setColor(C_MINT);
            g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int bx0 = Math.max(10, rx - 16);
            int bx1 = Math.min(w - 10, rx + rw + 16);
            int by0 = Math.max(10, ry - 12);
            int by1 = Math.min(h - 10, ry + rh + 12);
            int len = 22;
            g2.drawLine(bx0, by0 + len, bx0, by0);
            g2.drawLine(bx0, by0, bx0 + len, by0);
            g2.drawLine(bx1 - len, by0, bx1, by0);
            g2.drawLine(bx1, by0, bx1, by0 + len);
            g2.drawLine(bx0, by1 - len, bx0, by1);
            g2.drawLine(bx0, by1, bx0 + len, by1);
            g2.drawLine(bx1 - len, by1, bx1, by1);
            g2.drawLine(bx1, by1, bx1, by1 - len);
            g2.dispose();
        }

        private static void centerString(Graphics2D g2, String s, int cx, int baseline) {
            g2.drawString(s, cx - g2.getFontMetrics().stringWidth(s) / 2, baseline);
        }
    }

    private static final class MonthlyChart extends JComponent {
        private static final double[] VALUES = {2, 3.4, 5, 6.5, 8, 9.6, 11, 12.6, 14.6};
        private static final String[] MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep"};

        MonthlyChart() {
            setPreferredSize(new Dimension(240, 120));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            quality(g2);
            int w = getWidth();
            int h = getHeight();
            g2.setFont(font(Font.BOLD, 11f));
            g2.setColor(C_INK);
            g2.drawString("Monthly Spending", 0, 11);

            int left = 30;
            int top = 22;
            int bottom = h - 16;
            g2.setFont(font(Font.PLAIN, 9f));
            FontMetrics fm = g2.getFontMetrics();
            String[] ticks = {"15k", "10k", "5k", "0"};
            for (int i = 0; i < ticks.length; i++) {
                int y = top + (bottom - top) * i / 3;
                g2.setColor(new Color(0xE9, 0xEF, 0xEC));
                g2.drawLine(left, y, w, y);
                g2.setColor(C_TEXT2);
                g2.drawString(ticks[i], left - 6 - fm.stringWidth(ticks[i]), y + 3);
            }
            int n = VALUES.length;
            int areaW = w - left - 4;
            int slot = areaW / n;
            int bw = Math.max(6, slot - 7);
            for (int i = 0; i < n; i++) {
                int bh = (int) ((bottom - top) * VALUES[i] / 15.0);
                int x = left + 4 + i * slot;
                float t = i / (float) (n - 1);
                Color c = i == n - 1 ? new Color(0x0A, 0x7A, 0x55)
                        : new Color((int) (0xB5 - 0x6B * t), (int) (0xEB - 0x2B * t), (int) (0xD2 - 0x42 * t));
                g2.setColor(c);
                g2.fillRoundRect(x, bottom - bh, bw, bh, 4, 4);
                g2.setColor(C_TEXT2);
                g2.drawString(MONTHS[i], x + (bw - fm.stringWidth(MONTHS[i])) / 2, h - 3);
            }
            g2.dispose();
        }
    }

    private static final class CategoryDonut extends JComponent {
        private static final String[] NAMES = {"Food & Dining", "Travel", "Shopping", "Bills & Utilities", "Entertainment", "Others"};
        private static final int[] PCT = {32, 18, 16, 14, 10, 10};
        private static final Color[] COLORS = {
                new Color(0x10, 0xB9, 0x81), new Color(0x3B, 0x82, 0xF6), new Color(0xF5, 0x9E, 0x0B),
                new Color(0xEF, 0x44, 0x6E), new Color(0xC0, 0x26, 0xD3), new Color(0x64, 0x74, 0x8B)};

        CategoryDonut() {
            setPreferredSize(new Dimension(240, 120));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            quality(g2);
            int w = getWidth();
            int h = getHeight();
            g2.setFont(font(Font.BOLD, 11f));
            g2.setColor(C_INK);
            g2.drawString("Spending by Category", 0, 11);

            int d = Math.min(h - 22, 92);
            int dx = 4;
            int dy = 20 + (h - 20 - d) / 2;
            double start = 90;
            float thick = 15f;
            g2.setStroke(new BasicStroke(thick, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
            for (int i = 0; i < PCT.length; i++) {
                double extent = -PCT[i] * 3.6;
                g2.setColor(COLORS[i]);
                g2.draw(new Arc2D.Double(dx + thick / 2, dy + thick / 2, d - thick, d - thick, start, extent, Arc2D.OPEN));
                start += extent;
            }
            g2.setColor(C_INK);
            g2.setFont(font(Font.BOLD, 11f));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString("₹24,560", dx + (d - fm.stringWidth("₹24,560")) / 2, dy + d / 2);
            g2.setFont(font(Font.PLAIN, 9f));
            fm = g2.getFontMetrics();
            g2.setColor(C_TEXT2);
            g2.drawString("Total", dx + (d - fm.stringWidth("Total")) / 2, dy + d / 2 + 12);

            int lx = dx + d + 18;
            int rowH = 14;
            int ly = dy + (d - rowH * NAMES.length) / 2 + 10;
            g2.setFont(font(Font.PLAIN, 10f));
            fm = g2.getFontMetrics();
            for (int i = 0; i < NAMES.length; i++) {
                g2.setColor(COLORS[i]);
                g2.fillOval(lx, ly - 7, 6, 6);
                g2.setColor(C_TEXT2);
                g2.drawString(NAMES[i], lx + 12, ly);
                String p = PCT[i] + "%";
                g2.drawString(p, w - 4 - fm.stringWidth(p), ly);
                ly += rowH;
            }
            g2.dispose();
        }
    }

    private static final class SecurityPanel extends JPanel {
        private final SmoothImage bgImage = new SmoothImage(loadImage("/images/security-background.png"));

        SecurityPanel() {
            setOpaque(true);
            setBackground(new Color(0x06, 0x3B, 0x2E));
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            quality(g2);
            // Draw gradient base
            g2.setPaint(new LinearGradientPaint(0, 0, getWidth(), 0, new float[]{0f, 0.55f, 1f},
                    new Color[]{new Color(0x05, 0x36, 0x2A), new Color(0x07, 0x4A, 0x39), new Color(0x0B, 0x63, 0x4A)}));
            g2.fillRect(0, 0, getWidth(), getHeight());
            // Overlay background image (orbit circles + dots)
            drawCover(g2, bgImage, getWidth(), getHeight());
            g2.dispose();
        }
    }

    private static final class ShieldArt extends JComponent {
        private final SmoothImage shieldImg = new SmoothImage(loadImage("/images/security-shield.png"));

        // Floating cards
        private static final String[] CARD_LABELS = {
                "Receipts\nEncrypted", "Supabase\nRLS Security",
                "Your Data\nStays Yours", "No Unwanted\nData Sharing"
        };
        private static final String[] CARD_ICONS = {"receipt", "database", "user", "clock"};

        ShieldArt() {
            setPreferredSize(new Dimension(480, 440));
            setMinimumSize(new Dimension(360, 380));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            quality(g2);
            int w = getWidth();
            int h = getHeight();

            // Calculate exact center matching the background image radar circles
            int cx = w / 2;
            int cy = (int) (h * 0.46);
            int r = (int) (h * 0.35);

            Container parent = getParent();
            if (parent != null && parent.getWidth() > 0 && parent.getHeight() > 0) {
                int pw = parent.getWidth();
                int ph = parent.getHeight();
                double scale = Math.max(pw / 1024.0, ph / 409.0);
                int dw = (int) Math.ceil(1024 * scale);
                int dh = (int) Math.ceil(409 * scale);
                int imgX = (pw - dw) / 2;
                int imgY = ph - dh;
                int globalCx = imgX + (int) Math.round(813 * scale);
                int globalCy = imgY + (int) Math.round(204 * scale);
                cx = globalCx - getX();
                cy = globalCy - getY();
                r = (int) Math.round(145 * scale);
            }

            // --- 1. Concentric orbit rings ---
            g2.setColor(new Color(120, 230, 195, 45));
            g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    10f, new float[]{6f, 8f}, 0f));
            g2.draw(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));

            // --- 2. Cardinal Orbit Icon Badges on the dashed circle ---
            drawOrbitBadge(g2, cx, cy - r, "cloud");       // Top
            drawOrbitBadge(g2, cx - r, cy, "lock");        // Left
            drawOrbitBadge(g2, cx + r, cy, "shield");      // Right
            drawOrbitBadge(g2, cx, cy + r, "database");    // Bottom

            // --- 3. 3D Tiered Pedestal at the base of the shield ---
            int pedY = cy + 56;
            drawPedestal(g2, cx, pedY);

            // --- 4. Shield PNG Image ---
            int imgH = (int) (h * 0.53);
            int imgW = (int) (imgH * ((double) shieldImg.width() / shieldImg.height()));
            int imgX = cx - imgW / 2;
            int imgY = pedY - (int) (imgH * 0.76);
            shieldImg.draw(g2, imgX, imgY, imgW, imgH);

            // --- 5. Floating Cards around the shield ---
            int cardW = 126;
            int cardH = 50;
            // Top-left: "Receipts Encrypted"
            drawFloatingCard(g2, cx - 180, cy - 90, cardW, cardH, CARD_ICONS[0], CARD_LABELS[0]);
            // Bottom-left: "Supabase RLS Security"
            drawFloatingCard(g2, cx - 180, cy + 46, cardW, cardH, CARD_ICONS[1], CARD_LABELS[1]);
            // Top-right: "Your Data Stays Yours"
            drawFloatingCard(g2, cx + 56, cy - 90, cardW, cardH, CARD_ICONS[2], CARD_LABELS[2]);
            // Bottom-right: "No Unwanted Data Sharing"
            drawFloatingCard(g2, cx + 56, cy + 46, cardW, cardH, CARD_ICONS[3], CARD_LABELS[3]);

            g2.dispose();
        }

        private void drawOrbitBadge(Graphics2D g2, int bx, int by, String iconName) {
            int d = 30;
            int x = bx - d / 2;
            int y = by - d / 2;

            // Outer glow
            g2.setColor(new Color(52, 211, 153, 40));
            g2.fillOval(x - 3, y - 3, d + 6, d + 6);

            // Disc background
            g2.setColor(new Color(0x06, 0x3E, 0x30));
            g2.fillOval(x, y, d, d);

            // Disc border
            g2.setColor(new Color(0x34, 0xD3, 0x99, 140));
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawOval(x, y, d - 1, d - 1);

            // Icon
            SvgIcons.paint(g2, iconName, x + 7, y + 7, 16, Color.WHITE);
        }

        private void drawPedestal(Graphics2D g2, int cx, int topY) {
            // Ambient base shadow
            Graphics2D sg = (Graphics2D) g2.create();
            sg.translate(cx, topY + 36);
            sg.scale(1.0, 0.35);
            RadialGradientPaint rgp = new RadialGradientPaint(
                    new Point2D.Float(0, 0), 120f,
                    new float[]{0f, 0.6f, 1f},
                    new Color[]{new Color(0, 20, 15, 180), new Color(0, 20, 15, 80), new Color(0, 20, 15, 0)});
            sg.setPaint(rgp);
            sg.fill(new Ellipse2D.Float(-120, -120, 240, 240));
            sg.dispose();

            // Tier 1 (bottom tier)
            int bW = 186;
            int bH = 34;
            int bX = cx - bW / 2;
            int bY = topY + 16;
            // Extrusion body
            g2.setPaint(new LinearGradientPaint(bX, 0, bX + bW, 0,
                    new float[]{0f, 0.5f, 1f},
                    new Color[]{new Color(0x05, 0x2A, 0x20), new Color(0x09, 0x3F, 0x31), new Color(0x05, 0x2A, 0x20)}));
            g2.fillRect(bX, bY + bH / 2, bW, 14);
            // Lower ellipse
            g2.fillOval(bX, bY + 14, bW, bH);
            // Top ellipse of tier 1
            g2.setPaint(new LinearGradientPaint(bX, 0, bX + bW, 0,
                    new float[]{0f, 0.5f, 1f},
                    new Color[]{new Color(0x07, 0x35, 0x28), new Color(0x0D, 0x4D, 0x3C), new Color(0x07, 0x35, 0x28)}));
            g2.fillOval(bX, bY, bW, bH);

            // Tier 2 (middle tier)
            int mW = 154;
            int mH = 28;
            int mX = cx - mW / 2;
            int mY = topY + 8;
            g2.setPaint(new LinearGradientPaint(mX, 0, mX + mW, 0,
                    new float[]{0f, 0.5f, 1f},
                    new Color[]{new Color(0x07, 0x35, 0x28), new Color(0x0F, 0x56, 0x43), new Color(0x07, 0x35, 0x28)}));
            g2.fillRect(mX, mY + mH / 2, mW, 10);
            g2.fillOval(mX, mY + 10, mW, mH);
            g2.setPaint(new LinearGradientPaint(mX, 0, mX + mW, 0,
                    new float[]{0f, 0.5f, 1f},
                    new Color[]{new Color(0x09, 0x42, 0x33), new Color(0x13, 0x66, 0x4F), new Color(0x09, 0x42, 0x33)}));
            g2.fillOval(mX, mY, mW, mH);

            // Tier 3 (top platform with neon glow)
            int tW = 124;
            int tH = 24;
            int tX = cx - tW / 2;
            int tY = topY;
            g2.setPaint(new LinearGradientPaint(tX, 0, tX + tW, 0,
                    new float[]{0f, 0.5f, 1f},
                    new Color[]{new Color(0x0B, 0x4D, 0x3B), new Color(0x18, 0x7A, 0x5F), new Color(0x0B, 0x4D, 0x3B)}));
            g2.fillRect(tX, tY + tH / 2, tW, 7);
            g2.fillOval(tX, tY + 7, tW, tH);

            // Platform glowing surface
            Graphics2D pg = (Graphics2D) g2.create();
            pg.translate(cx, tY + tH / 2);
            pg.scale(1.0, (double) tH / tW);
            RadialGradientPaint topGlow = new RadialGradientPaint(
                    new Point2D.Float(0, 0), tW / 2f,
                    new float[]{0f, 0.65f, 1f},
                    new Color[]{new Color(0x34, 0xD3, 0x99), new Color(0x10, 0xB9, 0x81), new Color(0x04, 0x5D, 0x45)});
            pg.setPaint(topGlow);
            pg.fillOval(-tW / 2, -tW / 2, tW, tW);
            pg.dispose();

            // Specular rim on top platform
            g2.setColor(new Color(0x6E, 0xE7, 0xB7, 220));
            g2.setStroke(new BasicStroke(1.4f));
            g2.drawOval(tX, tY, tW - 1, tH - 1);

            // Upward light bloom from platform onto the shield
            Graphics2D bg2 = (Graphics2D) g2.create();
            bg2.translate(cx, topY);
            bg2.scale(1.0, 0.6);
            RadialGradientPaint upBeam = new RadialGradientPaint(
                    new Point2D.Float(0, 0), 80f,
                    new float[]{0f, 0.5f, 1f},
                    new Color[]{new Color(52, 211, 153, 140), new Color(16, 185, 129, 50), new Color(16, 185, 129, 0)});
            bg2.setPaint(upBeam);
            bg2.fill(new Ellipse2D.Float(-80, -80, 160, 160));
            bg2.dispose();
        }

        private void drawFloatingCard(Graphics2D g2, int px, int py, int cardW, int cardH,
                                       String icon, String label) {
            // Card background - semi-transparent glass effect
            Graphics2D cardG = (Graphics2D) g2.create();
            quality(cardG);
            cardG.setColor(new Color(6, 48, 38, 220));
            cardG.fillRoundRect(px, py, cardW, cardH, 12, 12);
            cardG.setColor(new Color(52, 211, 153, 60));
            cardG.setStroke(new BasicStroke(1f));
            cardG.drawRoundRect(px, py, cardW - 1, cardH - 1, 12, 12);

            // Checkmark circle badge at top-right
            int checkSize = 16;
            int checkX = px + cardW - checkSize - 6;
            int checkY = py + 6;
            cardG.setColor(new Color(0x10, 0xB9, 0x81));
            cardG.fillOval(checkX, checkY, checkSize, checkSize);
            // Draw check mark
            cardG.setColor(Color.WHITE);
            cardG.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            cardG.draw(new Line2D.Float(checkX + 4, checkY + 8, checkX + 7, checkY + 11));
            cardG.draw(new Line2D.Float(checkX + 7, checkY + 11, checkX + 12, checkY + 5));

            // Icon on the left
            SvgIcons.paint(cardG, icon, px + 10, py + (cardH - 18) / 2.0, 18,
                    new Color(0x34, 0xD3, 0x99));

            // Text
            String[] lines = label.split("\n");
            cardG.setFont(font(Font.BOLD, 10f));
            cardG.setColor(new Color(0xF0, 0xFD, 0xF4));
            FontMetrics fm = cardG.getFontMetrics();
            int textX = px + 34;
            int textY = py + (cardH - lines.length * fm.getHeight()) / 2 + fm.getAscent();
            for (String line : lines) {
                cardG.drawString(line, textX, textY);
                textY += fm.getHeight();
            }
            cardG.dispose();
        }
    }

}
