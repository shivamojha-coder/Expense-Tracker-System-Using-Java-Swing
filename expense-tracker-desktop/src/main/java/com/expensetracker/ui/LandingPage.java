package com.expensetracker.ui;

import com.expensetracker.ui.theme.ThemeColors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.net.URL;

/**
 * Premium Java Swing + FlatLaf Landing Page for Expense Tracker Desktop.
 * Designed with a consistent teal/green SaaS palette, real Swing components,
 * custom Java2D vector illustrations, responsive layout, and zero horizontal clipping.
 */
public final class LandingPage extends JPanel {

    // --- Premium Brand Color System ---
    private static final Color C_BG          = new Color(0xF7, 0xF9, 0xF8);
    private static final Color C_WHITE       = Color.WHITE;
    private static final Color C_DARK_TEAL   = new Color(0x06, 0x3B, 0x2E);
    private static final Color C_DEEP_GREEN  = new Color(0x07, 0x5C, 0x45);
    private static final Color C_ACCENT      = new Color(0x18, 0xC7, 0x8A);
    private static final Color C_ACCENT_HOV  = new Color(0x15, 0xB0, 0x7A);
    private static final Color C_PRI_TEXT    = new Color(0x17, 0x21, 0x1D);
    private static final Color C_SEC_TEXT    = new Color(0x5A, 0x6B, 0x64);
    private static final Color C_MUTED_TEXT  = new Color(0x8A, 0x9B, 0x94);
    private static final Color C_BORDER      = new Color(0xDD, 0xE5, 0xE1);
    private static final Color C_CARD_BG     = Color.WHITE;
    private static final Color C_SURFACE     = new Color(0xFA, 0xFB, 0xFA);
    private static final Color C_BADGE_BG    = new Color(0xE8, 0xF8, 0xF2);
    private static final Color C_BADGE_BD    = new Color(0xA7, 0xE9, 0xCD);
    private static final Color C_ICON_BG     = new Color(0xEE, 0xF9, 0xF4);
    private static final Color C_ICON_BD     = new Color(0xD2, 0xF0, 0xE3);
    private static final Color C_FOOTER_BG   = new Color(0x04, 0x2A, 0x21);

    private static Font fontB(float size) { return new Font("Segoe UI", Font.BOLD, (int) size); }
    private static Font fontP(float size) { return new Font("Segoe UI", Font.PLAIN, (int) size); }
    private static Font fontM(float size) { return new Font("Segoe UI", Font.BOLD, (int) size); }

    private final Runnable onSignUp;
    private final Runnable onLogin;

    private JScrollPane scrollPane;
    private JPanel featuresSection;
    private JPanel howItWorksSection;
    private JPanel securitySection;

    public LandingPage(Runnable onSignUp, Runnable onLogin) {
        this.onSignUp = onSignUp;
        this.onLogin = onLogin;

        setLayout(new BorderLayout());
        setBackground(C_BG);

        // Content panel implements Scrollable to strictly eliminate horizontal scrolling
        ScrollableContentPanel content = new ScrollableContentPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(C_BG);

        // Build all page sections
        content.add(buildNavbar());
        content.add(buildHero());
        content.add(buildFeatures());
        content.add(buildHowItWorks());
        content.add(buildSecurity());
        content.add(buildFinalCta());
        content.add(buildFooter());

        scrollPane = new JScrollPane(content);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(24);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        add(scrollPane, BorderLayout.CENTER);
    }

    // ==========================================
    // 1. Navigation Bar
    // ==========================================
    private JPanel buildNavbar() {
        JPanel bar = new JPanel(new GridBagLayout());
        bar.setBackground(C_WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, C_BORDER),
                BorderFactory.createEmptyBorder(0, 48, 0, 48)
        ));
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 84));
        bar.setPreferredSize(new Dimension(1200, 84));

        // Brand Logo + Title
        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        brand.setOpaque(false);
        JLabel logoIcon = new JLabel(new javax.swing.ImageIcon(
                loadImage("/images/app-logo.png").getScaledInstance(64, 48, Image.SCALE_SMOOTH)));
        logoIcon.setPreferredSize(new Dimension(68, 52));
        JLabel name = new JLabel("Expense Tracker");
        name.setFont(fontB(18));
        name.setForeground(C_DARK_TEAL);
        brand.add(logoIcon);
        brand.add(name);
        // Middle Navigation Links
        JPanel links = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        links.setOpaque(false);
        links.add(navLink("Features", () -> scrollTo(featuresSection)));
        links.add(navLink("How It Works", () -> scrollTo(howItWorksSection)));
        links.add(navLink("Security", () -> scrollTo(securitySection)));
        // Actions: Log In + Sign Up
        JPanel acts = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        acts.setOpaque(false);
        acts.add(textBtn("Log In", onLogin));
        acts.add(pillBtn("Sign Up", onSignUp, true));
        // Equal flexible outer columns keep the navigation links centered in the full window,
        // independent of the different widths of the brand and action groups.
        GridBagConstraints nav = new GridBagConstraints();
        nav.gridy = 0;
        nav.fill = GridBagConstraints.NONE;
        nav.weightx = 1.0;
        nav.anchor = GridBagConstraints.WEST;
        bar.add(brand, nav);

        nav.gridx = 1;
        nav.weightx = 0.0;
        nav.anchor = GridBagConstraints.CENTER;
        bar.add(links, nav);

        nav.gridx = 2;
        nav.weightx = 1.0;
        nav.anchor = GridBagConstraints.EAST;
        bar.add(acts, nav);

        return bar;
    }

    // ==========================================
    // 2. Hero Section
    // ==========================================
    private JPanel buildHero() {
        JPanel heroWrap = new JPanel(new BorderLayout());
        heroWrap.setOpaque(false);
        heroWrap.setBorder(new EmptyBorder(40, 48, 48, 48));

        JPanel container = new JPanel(new GridBagLayout());
        container.setOpaque(false);

        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.BOTH;
        g.weighty = 1.0;

        // Left Column: Value Proposition & CTAs
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        // Top Pill Badge
        JPanel badgeWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        badgeWrap.setOpaque(false);
        JLabel badge = new JLabel("  ✦  A SMARTER WAY TO MANAGE YOUR MONEY  ");
        badge.setOpaque(true);
        badge.setBackground(C_BADGE_BG);
        badge.setForeground(C_DEEP_GREEN);
        badge.setFont(fontB(11));
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BADGE_BD, 1, true),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        badgeWrap.add(badge);
        badgeWrap.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Dual-tone Hero Title
        JLabel h1 = new JLabel("Take Control");
        h1.setFont(fontB(48));
        h1.setForeground(C_DARK_TEAL);
        h1.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel h2 = new JLabel("of Every Expense");
        h2.setFont(fontB(48));
        h2.setForeground(C_ACCENT);
        h2.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Subtitle Description
        JLabel desc = new JLabel("<html><body style='width:420px; line-height:1.55; color:#5A6B64; font-size:14px;'>"
                + "Track spending in real time, extract receipt data with optical character recognition (OCR), "
                + "manage budgets, and export financial reports — all in one private, offline-capable desktop suite."
                + "</body></html>");
        desc.setFont(fontP(14));
        desc.setForeground(C_SEC_TEXT);
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);

        // CTA Buttons Row
        JPanel cta = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        cta.setOpaque(false);
        cta.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton su = pillBtn("Sign Up  →", onSignUp, true);
        su.setPreferredSize(new Dimension(156, 46));
        su.setFont(fontB(14));

        JButton li = outlineBtn("Log In", onLogin);
        li.setPreferredSize(new Dimension(120, 46));
        li.setFont(fontB(14));

        JButton exp = new JButton("Explore Features ↓");
        exp.setFont(fontP(13));
        exp.setForeground(C_SEC_TEXT);
        exp.setContentAreaFilled(false);
        exp.setBorderPainted(false);
        exp.setFocusPainted(false);
        exp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        exp.addActionListener(e -> scrollTo(featuresSection));
        exp.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { exp.setForeground(C_DEEP_GREEN); }
            public void mouseExited(MouseEvent e) { exp.setForeground(C_SEC_TEXT); }
        });

        cta.add(su);
        cta.add(li);
        cta.add(exp);

        // Trust Pills Row
        JPanel trust = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 0));
        trust.setOpaque(false);
        trust.setAlignmentX(Component.LEFT_ALIGNMENT);
        trust.add(trustItem("100% Private & Secure"));
        trust.add(trustItem("Smart Receipt OCR"));
        trust.add(trustItem("PDF & CSV Export"));

        left.add(badgeWrap);
        left.add(Box.createVerticalStrut(18));
        left.add(h1);
        left.add(h2);
        left.add(Box.createVerticalStrut(14));
        left.add(desc);
        left.add(Box.createVerticalStrut(26));
        left.add(cta);
        left.add(Box.createVerticalStrut(22));
        left.add(trust);

        // GridBag columns: Left 52%, Right 48%
        g.gridx = 0;
        g.weightx = 0.52;
        g.insets = new Insets(0, 0, 0, 36);
        container.add(left, g);

        g.gridx = 1;
        g.weightx = 0.48;
        g.insets = new Insets(0, 0, 0, 0);
        container.add(buildHeroLaptopPreview(), g);

        heroWrap.add(container, BorderLayout.CENTER);
        return heroWrap;
    }

    private JPanel trustItem(String text) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        p.setOpaque(false);
        VectorIcon check = new VectorIcon("check", 14, C_ACCENT);
        JLabel lbl = new JLabel(text);
        lbl.setFont(fontP(12));
        lbl.setForeground(C_SEC_TEXT);
        p.add(check);
        p.add(lbl);
        return p;
    }

    // ==========================================
    // 3. Local Laptop Dashboard Preview
    // ==========================================
    private JPanel buildHeroLaptopPreview() {
        JPanel preview = new JPanel(new BorderLayout());
        preview.setOpaque(false);
        preview.setBorder(new EmptyBorder(4, 0, 4, 0));
        preview.add(new ResponsiveImagePanel(loadImage("/images/hero-laptop-preview.png")), BorderLayout.CENTER);
        return preview;
    }

    private static Image loadImage(String resourcePath) {
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

    // ==========================================
    // 3. Enlarged & Improved Dashboard Preview Card
    // ==========================================
    private JPanel buildDashboardPreview() {
        ShadowContainer shadow = new ShadowContainer();
        shadow.setLayout(new BorderLayout());
        shadow.setOpaque(false);
        shadow.setBorder(new EmptyBorder(4, 4, 12, 4));

        RoundPanel card = new RoundPanel(16, C_WHITE);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                BorderFactory.createEmptyBorder(18, 20, 18, 20)
        ));
        card.setPreferredSize(new Dimension(510, 480));
        card.setMinimumSize(new Dimension(380, 420));

        // Window Top Chrome: Dots + App Title + Synced Badge
        JPanel chrome = new JPanel(new BorderLayout());
        chrome.setOpaque(false);
        chrome.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xEE, 0xF2, 0xF0)),
                BorderFactory.createEmptyBorder(0, 0, 12, 0)
        ));

        JPanel dots = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        dots.setOpaque(false);
        dots.add(miniDot(new Color(0xFF, 0x5F, 0x56))); // red
        dots.add(miniDot(new Color(0xFF, 0xBD, 0x2E))); // yellow
        dots.add(miniDot(new Color(0x27, 0xC9, 0x3F))); // green

        JLabel title = new JLabel("Financial Dashboard");
        title.setFont(fontB(13));
        title.setForeground(C_PRI_TEXT);

        JPanel status = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        status.setOpaque(false);
        JLabel liveDot = new JLabel("●");
        liveDot.setFont(fontB(10));
        liveDot.setForeground(C_ACCENT);
        JLabel liveText = new JLabel("Live Preview");
        liveText.setFont(fontB(11));
        liveText.setForeground(C_DEEP_GREEN);
        status.add(liveDot);
        status.add(liveText);

        JPanel leftChrome = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftChrome.setOpaque(false);
        leftChrome.add(dots);
        leftChrome.add(title);

        chrome.add(leftChrome, BorderLayout.WEST);
        chrome.add(status, BorderLayout.EAST);
        card.add(chrome, BorderLayout.NORTH);

        // Body: 3 Metric Cards + MiniChart + Category Breakdown + Transactions
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(14, 0, 0, 0));

        // 3 Key Metrics Row
        JPanel statsRow = new JPanel(new GridLayout(1, 3, 10, 0));
        statsRow.setOpaque(false);
        statsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));
        statsRow.add(previewStatCard("TOTAL SPENT", "$3,842.50", "+4.2%", C_DARK_TEAL));
        statsRow.add(previewStatCard("THIS MONTH", "$1,248.80", "Budget: $2k", C_DEEP_GREEN));
        statsRow.add(previewStatCard("RECEIPTS", "28 Scanned", "100% OCR", C_ACCENT));
        body.add(statsRow);
        body.add(Box.createVerticalStrut(12));

        // Monthly Spending Trend Chart
        RoundPanel chartBox = new RoundPanel(10, C_SURFACE);
        chartBox.setLayout(new BorderLayout(0, 6));
        chartBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));
        chartBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        JPanel chartHdr = new JPanel(new BorderLayout());
        chartHdr.setOpaque(false);
        JLabel chartTitle = new JLabel("Monthly Spending Trend");
        chartTitle.setFont(fontB(11));
        chartTitle.setForeground(C_PRI_TEXT);

        JLabel chartSub = new JLabel("Last 7 Months  •  Target On Track");
        chartSub.setFont(fontP(10));
        chartSub.setForeground(C_DEEP_GREEN);
        chartHdr.add(chartTitle, BorderLayout.WEST);
        chartHdr.add(chartSub, BorderLayout.EAST);
        chartBox.add(chartHdr, BorderLayout.NORTH);

        MiniBarChart barChart = new MiniBarChart();
        barChart.setPreferredSize(new Dimension(0, 66));
        chartBox.add(barChart, BorderLayout.CENTER);
        body.add(chartBox);
        body.add(Box.createVerticalStrut(12));

        // Top Categories Chips
        JPanel catSection = new JPanel();
        catSection.setLayout(new BoxLayout(catSection, BoxLayout.Y_AXIS));
        catSection.setOpaque(false);
        catSection.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JPanel catRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        catRow.setOpaque(false);
        catRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        catRow.add(catChip("Food & Dining", "42%", new Color(0xF5, 0x9E, 0x0B)));
        catRow.add(catChip("Tech & Office", "28%", new Color(0x63, 0x66, 0xF1)));
        catRow.add(catChip("Travel", "18%", new Color(0x18, 0xC7, 0x8A)));
        catRow.add(catChip("Utilities", "12%", new Color(0x64, 0x74, 0x8B)));
        catSection.add(catRow);
        body.add(catSection);
        body.add(Box.createVerticalStrut(10));

        // Recent Transactions Header + 3 Real Swing Rows
        JPanel txnHeader = new JPanel(new BorderLayout());
        txnHeader.setOpaque(false);
        txnHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        JLabel txnTitle = new JLabel("Recent Transactions");
        txnTitle.setFont(fontB(11));
        txnTitle.setForeground(C_SEC_TEXT);
        JLabel txnSub = new JLabel("Instant OCR Synced");
        txnSub.setFont(fontP(10));
        txnSub.setForeground(C_MUTED_TEXT);
        txnHeader.add(txnTitle, BorderLayout.WEST);
        txnHeader.add(txnSub, BorderLayout.EAST);
        body.add(txnHeader);
        body.add(Box.createVerticalStrut(6));

        body.add(txnRow("Starbucks Reserve", "Food & Dining", "-$14.80", "OCR Scanned", new Color(0xF5, 0x9E, 0x0B)));
        body.add(Box.createVerticalStrut(5));
        body.add(txnRow("Apple Store NYC", "Hardware & Tech", "-$1,299.00", "OCR Scanned", new Color(0x63, 0x66, 0xF1)));
        body.add(Box.createVerticalStrut(5));
        body.add(txnRow("Metro Transit Pass", "Travel & Transit", "-$85.00", "Manual Entry", new Color(0x18, 0xC7, 0x8A)));

        card.add(body, BorderLayout.CENTER);
        shadow.add(card, BorderLayout.CENTER);
        return shadow;
    }

    private JPanel miniDot(Color color) {
        JPanel dot = new JPanel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillOval(0, 0, 9, 9);
                g2.dispose();
            }
        };
        dot.setOpaque(false);
        dot.setPreferredSize(new Dimension(9, 9));
        return dot;
    }

    private JPanel previewStatCard(String label, String value, String badge, Color valueColor) {
        RoundPanel p = new RoundPanel(8, C_SURFACE);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        JLabel l = new JLabel(label);
        l.setFont(fontB(9));
        l.setForeground(C_MUTED_TEXT);

        JLabel v = new JLabel(value);
        v.setFont(fontB(13));
        v.setForeground(valueColor);

        JLabel b = new JLabel(badge);
        b.setFont(fontM(9));
        b.setForeground(C_DEEP_GREEN);

        p.add(l);
        p.add(Box.createVerticalStrut(2));
        p.add(v);
        p.add(Box.createVerticalStrut(1));
        p.add(b);
        return p;
    }

    private JPanel catChip(String name, String pct, Color color) {
        JPanel chip = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        chip.setBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 20));
        chip.setBorder(BorderFactory.createLineBorder(new Color(color.getRed(), color.getGreen(), color.getBlue(), 65), 1, true));

        JPanel dot = new JPanel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillOval(0, 0, 6, 6);
                g2.dispose();
            }
        };
        dot.setOpaque(false);
        dot.setPreferredSize(new Dimension(6, 6));

        JLabel lbl = new JLabel(name + " " + pct);
        lbl.setFont(fontB(9));
        lbl.setForeground(C_PRI_TEXT);

        chip.add(dot);
        chip.add(lbl);
        return chip;
    }

    private JPanel txnRow(String merchant, String category, String amount, String tag, Color categoryDot) {
        RoundPanel row = new RoundPanel(8, C_WHITE);
        row.setLayout(new BorderLayout());
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);

        JPanel dot = new JPanel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(categoryDot);
                g2.fillOval(0, 0, 8, 8);
                g2.dispose();
            }
        };
        dot.setOpaque(false);
        dot.setPreferredSize(new Dimension(8, 8));

        JLabel m = new JLabel(merchant);
        m.setFont(fontB(11));
        m.setForeground(C_PRI_TEXT);

        JLabel c = new JLabel("• " + category);
        c.setFont(fontP(10));
        c.setForeground(C_SEC_TEXT);

        left.add(dot);
        left.add(m);
        left.add(c);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        JLabel a = new JLabel(amount);
        a.setFont(fontB(11));
        a.setForeground(C_DARK_TEAL);

        JLabel t = new JLabel(tag);
        t.setFont(fontP(9));
        t.setForeground(C_DEEP_GREEN);
        t.setOpaque(true);
        t.setBackground(C_BADGE_BG);
        t.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BADGE_BD, 1, true),
                BorderFactory.createEmptyBorder(1, 6, 1, 6)
        ));

        right.add(a);
        right.add(t);

        row.add(left, BorderLayout.WEST);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    // ==========================================
    // 4. Feature Cards: Clean 3-Column / 2-Row Grid
    // ==========================================
    private JPanel buildFeatures() {
        featuresSection = new JPanel(new BorderLayout());
        featuresSection.setBackground(C_WHITE);
        featuresSection.setBorder(new EmptyBorder(64, 48, 64, 48));

        // Section Title & Subheading
        JPanel hdr = new JPanel();
        hdr.setLayout(new BoxLayout(hdr, BoxLayout.Y_AXIS));
        hdr.setOpaque(false);

        JLabel badge = new JLabel("CAPABILITIES");
        badge.setFont(fontB(11));
        badge.setForeground(C_DEEP_GREEN);
        badge.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel t1 = new JLabel("Everything You Need to Master Your Money", SwingConstants.CENTER);
        t1.setFont(fontB(32));
        t1.setForeground(C_DARK_TEAL);
        t1.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("From intelligent optical receipt scanning to executive PDF reporting, all in one place.", SwingConstants.CENTER);
        sub.setFont(fontP(15));
        sub.setForeground(C_SEC_TEXT);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        hdr.add(badge);
        hdr.add(Box.createVerticalStrut(6));
        hdr.add(t1);
        hdr.add(Box.createVerticalStrut(8));
        hdr.add(sub);
        featuresSection.add(hdr, BorderLayout.NORTH);

        // 3-Column x 2-Row Grid
        JPanel grid = new JPanel(new GridLayout(2, 3, 20, 20));
        grid.setOpaque(false);
        grid.setBorder(new EmptyBorder(36, 0, 0, 0));

        grid.add(buildFeatureCard("ocr", "Receipt OCR Scanning",
                "Scan or drop receipts and invoices. Built-in Tess4J automatically extracts merchant names, dates, and amounts into editable fields."));
        grid.add(buildFeatureCard("insights", "Smart Visual Insights",
                "Understand where every dollar goes with interactive JFreeChart breakdowns across spending categories, payment methods, and timelines."));
        grid.add(buildFeatureCard("reports", "Executive PDF & CSV Reports",
                "Generate comprehensive, structured financial summaries. One-click export to PDF or CSV ready for accountants and audits."));
        grid.add(buildFeatureCard("search", "Deep Search & Filters",
                "Locate any transaction in milliseconds. Filter by category, payment method, date intervals, receipt attachment, and custom notes."));
        grid.add(buildFeatureCard("export", "Instant Data Mobility",
                "Keep complete ownership of your records. Export your entire transaction archive or filtered sets whenever you need them."));
        grid.add(buildFeatureCard("security", "Row-Level Security & Privacy",
                "Backed by Supabase Auth with strict Row-Level Security policies. Your transaction data is isolated, encrypted, and accessible only to you."));

        featuresSection.add(grid, BorderLayout.CENTER);
        return featuresSection;
    }

    private JPanel buildFeatureCard(String iconType, String title, String desc) {
        RoundPanel card = new RoundPanel(14, C_SURFACE);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                BorderFactory.createEmptyBorder(22, 22, 22, 22)
        ));

        // Icon Box with Vector Graphic
        JPanel iconBox = new JPanel(new GridBagLayout()) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_ICON_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(C_ICON_BD);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        iconBox.setOpaque(false);
        iconBox.setPreferredSize(new Dimension(46, 46));
        iconBox.setMaximumSize(new Dimension(46, 46));
        iconBox.add(new VectorIcon(iconType, 22, C_DEEP_GREEN));

        JPanel iconRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        iconRow.setOpaque(false);
        iconRow.add(iconBox);
        iconRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel tl = new JLabel(title);
        tl.setFont(fontB(16));
        tl.setForeground(C_DARK_TEAL);
        tl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel dl = new JLabel("<html><body style='line-height:1.45; color:#5A6B64; font-size:12px;'>" + desc + "</body></html>");
        dl.setFont(fontP(13));
        dl.setForeground(C_SEC_TEXT);
        dl.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(iconRow);
        card.add(Box.createVerticalStrut(14));
        card.add(tl);
        card.add(Box.createVerticalStrut(8));
        card.add(dl);

        // Hover Effect
        card.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                card.setBackground(new Color(0xF2, 0xFA, 0xF5));
                card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(C_ACCENT, 1),
                        BorderFactory.createEmptyBorder(22, 22, 22, 22)
                ));
            }
            public void mouseExited(MouseEvent e) {
                card.setBackground(C_SURFACE);
                card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(C_BORDER, 1),
                        BorderFactory.createEmptyBorder(22, 22, 22, 22)
                ));
            }
        });

        return card;
    }

    // ==========================================
    // 5. "How It Works" Section
    // ==========================================
    private JPanel buildHowItWorks() {
        howItWorksSection = new JPanel(new BorderLayout());
        howItWorksSection.setBackground(C_BG);
        howItWorksSection.setBorder(new EmptyBorder(64, 48, 64, 48));

        JPanel hdr = new JPanel();
        hdr.setLayout(new BoxLayout(hdr, BoxLayout.Y_AXIS));
        hdr.setOpaque(false);

        JLabel badge = new JLabel("SIMPLE WORKFLOW");
        badge.setFont(fontB(11));
        badge.setForeground(C_DEEP_GREEN);
        badge.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel t = new JLabel("How Expense Tracker Works in 3 Simple Steps", SwingConstants.CENTER);
        t.setFont(fontB(32));
        t.setForeground(C_DARK_TEAL);
        t.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel s = new JLabel("Effortlessly transition from scattered paper receipts to comprehensive financial control.", SwingConstants.CENTER);
        s.setFont(fontP(15));
        s.setForeground(C_SEC_TEXT);
        s.setAlignmentX(Component.CENTER_ALIGNMENT);

        hdr.add(badge);
        hdr.add(Box.createVerticalStrut(6));
        hdr.add(t);
        hdr.add(Box.createVerticalStrut(8));
        hdr.add(s);
        howItWorksSection.add(hdr, BorderLayout.NORTH);

        StepsContainer stepsContainer = new StepsContainer();
        stepsContainer.setOpaque(false);
        stepsContainer.setBorder(new EmptyBorder(38, 0, 0, 0));
        howItWorksSection.add(stepsContainer, BorderLayout.CENTER);

        return howItWorksSection;
    }

    private class StepsContainer extends JPanel {
        private final FloatingStepCard first = stepCard("01", "ocr", "Record & Scan",
                "Add expenses manually in seconds or attach physical receipts for instant OCR extraction.");
        private final FloatingStepCard second = stepCard("02", "search", "Categorize & Track",
                "Organize transactions by category, payment method, timestamp, and notes.");
        private final FloatingStepCard third = stepCard("03", "insights", "Understand & Export",
                "Analyze spending with charts, maintain budgets, and generate PDF summaries.");

        StepsContainer() {
            setLayout(null); // Only this visual canvas positions its three floating cards.
            setPreferredSize(new Dimension(980, 540));
            setMinimumSize(new Dimension(0, 540));
            add(first);
            add(second);
            add(third);
        }

        @Override
        public void doLayout() {
            int w = getWidth();
            if (w < 680) {
                int cardW = Math.max(220, Math.min(360, w - 28));
                int x = Math.max(14, (w - cardW) / 2);
                first.setBounds(x, 10, cardW, 156);
                second.setBounds(x, 184, cardW, 156);
                third.setBounds(x, 358, cardW, 156);
                return;
            }

            int cardW = Math.max(240, Math.min(366, (int) (w * 0.205)));
            int cardH = 182;
            first.setBounds(Math.max(18, (int) (w * 0.075)), 18, cardW, cardH);
            second.setBounds(Math.min(w - cardW - 18, (int) (w * 0.55)), 123, cardW, cardH);
            third.setBounds(Math.min(w - cardW - 18, (int) (w * 0.27)), 332, cardW, cardH);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Very quiet editorial background details.
            g2.setColor(new Color(0x18, 0xC7, 0x8A, 12));
            for (int x = 28; x < getWidth(); x += 42) {
                for (int y = 14; y < getHeight(); y += 42) g2.fillOval(x, y, 2, 2);
            }
            g2.setStroke(new BasicStroke(1f));
            g2.setColor(new Color(0x06, 0x3B, 0x2E, 13));
            for (int x = 24; x < getWidth(); x += 95) g2.drawLine(x, 0, x, getHeight() - 10);
            for (int y = 32; y < getHeight(); y += 88) g2.drawLine(0, y, getWidth() - 24, y);
            g2.setColor(new Color(0x06, 0x3B, 0x2E, 10));
            g2.drawOval(getWidth() - 170, 12, 132, 132);
            g2.drawOval(20, getHeight() - 126, 96, 96);
            g2.drawLine(26, getHeight() - 26, getWidth() - 26, getHeight() - 26);

            Rectangle a = first.getBounds();
            Rectangle b = second.getBounds();
            Rectangle c = third.getBounds();
            g2.setColor(new Color(0x18, 0xC7, 0x8A, 130));
            g2.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{4, 7}, 0));
            Path2D journey = new Path2D.Float();
            if (getWidth() < 680) {
                int cx = getWidth() / 2;
                journey.moveTo(cx, a.y + a.height);
                journey.curveTo(cx + 42, a.y + a.height + 24, cx - 42, b.y - 24, cx, b.y);
                journey.moveTo(cx, b.y + b.height);
                journey.curveTo(cx - 42, b.y + b.height + 24, cx + 42, c.y - 24, cx, c.y);
            } else {
                journey.moveTo(a.x + a.width, a.y + a.height / 2);
                journey.curveTo(a.x + a.width + 85, a.y + a.height / 2 - 28,
                        b.x - 72, b.y + b.height / 2 - 18, b.x, b.y + b.height / 2);
                journey.moveTo(b.x + (b.width * 2) / 5, b.y + b.height);
                journey.curveTo(b.x + (b.width * 2) / 5 - 8, b.y + b.height + 48,
                        c.x + (c.width * 4) / 5 + 34, c.y - 38, c.x + (c.width * 4) / 5, c.y);
            }
            g2.draw(journey);
            g2.dispose();
        }
    }

    private FloatingStepCard stepCard(String stepNum, String iconType, String title, String desc) {
        return new FloatingStepCard(stepNum, iconType, title, desc);
    }

    private final class FloatingStepCard extends JPanel {
        FloatingStepCard(String stepNum, String iconType, String title, String desc) {
            setOpaque(false);
            setLayout(new BorderLayout(0, 12));
            setBorder(new EmptyBorder(18, 20, 17, 20));

            JPanel topRow = new JPanel(new BorderLayout());
            topRow.setOpaque(false);
            JLabel number = new JLabel(stepNum);
            number.setFont(fontB(10));
            number.setForeground(C_DEEP_GREEN);
            number.setOpaque(true);
            number.setBackground(C_BADGE_BG);
            number.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(C_BADGE_BD, 1, true),
                    BorderFactory.createEmptyBorder(4, 9, 4, 9)));
            topRow.add(number, BorderLayout.WEST);
            topRow.add(new VectorIcon(iconType, 21, C_DEEP_GREEN), BorderLayout.EAST);

            JPanel copy = new JPanel(new BorderLayout(0, 6));
            copy.setOpaque(false);
            JLabel heading = new JLabel(title);
            heading.setFont(fontB(17));
            heading.setForeground(C_DARK_TEAL);
            JTextArea description = new JTextArea(desc);
            description.setFont(fontP(12));
            description.setForeground(C_SEC_TEXT);
            description.setLineWrap(true);
            description.setWrapStyleWord(true);
            description.setEditable(false);
            description.setFocusable(false);
            description.setOpaque(false);
            description.setBorder(BorderFactory.createEmptyBorder());
            copy.add(heading, BorderLayout.NORTH);
            copy.add(description, BorderLayout.CENTER);

            add(topRow, BorderLayout.NORTH);
            add(copy, BorderLayout.CENTER);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            for (int i = 5; i >= 1; i--) {
                g2.setColor(new Color(0, 0, 0, 3 + i));
                g2.fillRoundRect(3 + i, 5 + i, getWidth() - 6 - (i * 2), getHeight() - 8, 18, 18);
            }
            g2.setColor(C_WHITE);
            g2.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 5, 16, 16);
            g2.setColor(C_BORDER);
            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 5, 16, 16);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ==========================================
    // 6. Enterprise Security Banner Section
    // ==========================================
    private JPanel buildSecurity() {
        securitySection = new JPanel(new BorderLayout());
        securitySection.setBackground(C_WHITE);
        securitySection.setBorder(new EmptyBorder(56, 48, 56, 48));

        RoundPanel banner = new RoundPanel(18, C_DARK_TEAL);
        banner.setLayout(new GridBagLayout());
        banner.setBorder(new EmptyBorder(40, 44, 40, 44));

        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.BOTH;
        g.weighty = 1.0;

        // Left Side: Prominent Vector Shield Illustration
        JPanel shieldWrap = new JPanel(new GridBagLayout());
        shieldWrap.setOpaque(false);
        VectorIcon largeShield = new VectorIcon("security_large", 80, C_ACCENT);
        shieldWrap.add(largeShield);

        g.gridx = 0;
        g.gridy = 0;
        g.weightx = 0.18;
        g.insets = new Insets(0, 0, 0, 28);
        banner.add(shieldWrap, g);

        // Right Side: Content + Trust Bullet Points
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        JLabel badge = new JLabel("ENTERPRISE-GRADE PRIVACY & PROTECTION");
        badge.setFont(fontB(11));
        badge.setForeground(C_ACCENT);
        badge.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel st = new JLabel("Your Financial Data Stays 100% Yours");
        st.setFont(fontB(24));
        st.setForeground(C_WHITE);
        st.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sd = new JLabel("<html><body style='color:#D1E7DD; line-height:1.55; font-size:13px;'>"
                + "Every record, receipt scan, and category is safeguarded with Supabase Row-Level Security (RLS). "
                + "Only your authenticated account can read or mutate your transactions. "
                + "Passwords, keys, and session tokens are securely handled and never exposed."
                + "</body></html>");
        sd.setFont(fontP(13));
        sd.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel checks = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 0));
        checks.setOpaque(false);
        checks.setAlignmentX(Component.LEFT_ALIGNMENT);
        checks.add(secCheckItem("Row-Level Security (RLS) Isolation"));
        checks.add(secCheckItem("Zero-Telemetry Guarantee"));
        checks.add(secCheckItem("Local Offline Demo Mode"));

        content.add(badge);
        content.add(Box.createVerticalStrut(8));
        content.add(st);
        content.add(Box.createVerticalStrut(10));
        content.add(sd);
        content.add(Box.createVerticalStrut(20));
        content.add(checks);

        g.gridx = 1;
        g.weightx = 0.82;
        g.insets = new Insets(0, 0, 0, 0);
        banner.add(content, g);

        securitySection.add(banner, BorderLayout.CENTER);
        return securitySection;
    }

    private JPanel secCheckItem(String text) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        p.setOpaque(false);
        VectorIcon check = new VectorIcon("check", 14, C_ACCENT);
        JLabel lbl = new JLabel(text);
        lbl.setFont(fontB(12));
        lbl.setForeground(new Color(0xE2, 0xF5, 0xED));
        p.add(check);
        p.add(lbl);
        return p;
    }

    // ==========================================
    // 7. Final Call to Action (CTA) Section
    // ==========================================
    private JPanel buildFinalCta() {
        JPanel sec = new JPanel(new BorderLayout());
        sec.setBackground(C_BG);
        sec.setBorder(new EmptyBorder(64, 48, 64, 48));

        RoundPanel card = new RoundPanel(16, C_WHITE);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                BorderFactory.createEmptyBorder(48, 36, 48, 36)
        ));

        JLabel t1 = new JLabel("Ready to Take Control of Your Financial Life?", SwingConstants.CENTER);
        t1.setFont(fontB(30));
        t1.setForeground(C_DARK_TEAL);
        t1.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel t2 = new JLabel("Start tracking spending, scanning receipts with OCR, and saving smarter today.", SwingConstants.CENTER);
        t2.setFont(fontP(15));
        t2.setForeground(C_SEC_TEXT);
        t2.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton su = pillBtn("Sign Up  →", onSignUp, true);
        su.setPreferredSize(new Dimension(160, 48));
        su.setFont(fontB(14));

        JButton li = outlineBtn("Log In", onLogin);
        li.setPreferredSize(new Dimension(126, 48));
        li.setFont(fontB(14));

        btnRow.add(su);
        btnRow.add(li);

        JLabel subNote = new JLabel("No credit card required  •  Offline Demo Mode available instantly", SwingConstants.CENTER);
        subNote.setFont(fontP(12));
        subNote.setForeground(C_MUTED_TEXT);
        subNote.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(t1);
        card.add(Box.createVerticalStrut(10));
        card.add(t2);
        card.add(Box.createVerticalStrut(28));
        card.add(btnRow);
        card.add(Box.createVerticalStrut(18));
        card.add(subNote);

        sec.add(card, BorderLayout.CENTER);
        return sec;
    }

    // ==========================================
    // 8. Footer Section
    // ==========================================
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(0, 16));
        footer.setBackground(C_FOOTER_BG);
        footer.setBorder(new EmptyBorder(38, 48, 30, 48));

        // Top Row: Brand & Navigation Jump Links
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        brand.setOpaque(false);
        JLabel footerLogo = new JLabel(new javax.swing.ImageIcon(
                loadImage("/images/app-logo.png").getScaledInstance(64, 48, Image.SCALE_SMOOTH)));
        footerLogo.setPreferredSize(new Dimension(68, 52));
        JLabel name = new JLabel("Expense Tracker Desktop");
        name.setFont(fontB(16));
        name.setForeground(C_WHITE);
        brand.add(footerLogo);
        brand.add(name);

        JPanel links = new JPanel(new FlowLayout(FlowLayout.RIGHT, 24, 0));
        links.setOpaque(false);
        links.add(footerLink("Features", () -> scrollTo(featuresSection)));
        links.add(footerLink("How It Works", () -> scrollTo(howItWorksSection)));
        links.add(footerLink("Security", () -> scrollTo(securitySection)));
        links.add(footerLink("Back to Top ↑", () -> scrollToTop()));

        top.add(brand, BorderLayout.WEST);
        top.add(links, BorderLayout.EAST);
        footer.add(top, BorderLayout.NORTH);

        // Divider
        JPanel divider = new JPanel();
        divider.setBackground(new Color(0x0A, 0x4E, 0x3D));
        divider.setPreferredSize(new Dimension(1, 1));
        divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        footer.add(divider, BorderLayout.CENTER);

        // Bottom Row: Copyright & Technology Badge
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);

        JLabel copyright = new JLabel("© 2026 Expense Tracker Desktop • Production Ready");
        copyright.setFont(fontP(12));
        copyright.setForeground(new Color(0x8A, 0xA6, 0x9B));

        JLabel tech = new JLabel("Java 22  •  Swing  •  FlatLaf  •  Supabase  •  Tess4J OCR  •  JFreeChart");
        tech.setFont(fontP(12));
        tech.setForeground(new Color(0x8A, 0xA6, 0x9B));

        bottom.add(copyright, BorderLayout.WEST);
        bottom.add(tech, BorderLayout.EAST);
        footer.add(bottom, BorderLayout.SOUTH);

        return footer;
    }

    private JLabel footerLink(String text, Runnable action) {
        JLabel l = new JLabel(text);
        l.setFont(fontP(13));
        l.setForeground(new Color(0xD1, 0xE7, 0xDD));
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        l.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { action.run(); }
            public void mouseEntered(MouseEvent e) { l.setForeground(C_ACCENT); }
            public void mouseExited(MouseEvent e) { l.setForeground(new Color(0xD1, 0xE7, 0xDD)); }
        });
        return l;
    }

    // ==========================================
    // UI Helpers: Buttons, Scrolling, Custom Panels
    // ==========================================
    private JButton pillBtn(String text, Runnable action, boolean primary) {
        JButton btn = new JButton(text) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(fontB(13));
        btn.setForeground(C_WHITE);
        btn.setBackground(primary ? C_DEEP_GREEN : C_DARK_TEAL);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(130, 40));
        btn.addActionListener(e -> action.run());

        Color base = primary ? C_DEEP_GREEN : C_DARK_TEAL;
        Color hov = primary ? C_DARK_TEAL : C_DEEP_GREEN;

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(hov); btn.repaint(); }
            public void mouseExited(MouseEvent e) { btn.setBackground(base); btn.repaint(); }
        });
        return btn;
    }

    private JButton outlineBtn(String text, Runnable action) {
        JButton btn = new JButton(text) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g2.setColor(C_DEEP_GREEN);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 22, 22);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(fontB(13));
        btn.setForeground(C_DARK_TEAL);
        btn.setBackground(C_WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(110, 40));
        btn.addActionListener(e -> action.run());
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(C_BADGE_BG); btn.repaint(); }
            public void mouseExited(MouseEvent e) { btn.setBackground(C_WHITE); btn.repaint(); }
        });
        return btn;
    }

    private JButton textBtn(String text, Runnable action) {
        JButton btn = new JButton(text);
        btn.setFont(fontB(13));
        btn.setForeground(C_DARK_TEAL);
        btn.setBackground(C_WHITE);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        btn.addActionListener(e -> action.run());
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setForeground(C_ACCENT_HOV); }
            public void mouseExited(MouseEvent e) { btn.setForeground(C_DARK_TEAL); }
        });
        return btn;
    }

    private JButton navLink(String label, Runnable action) {
        JButton btn = new JButton(label);
        btn.setFont(fontP(14));
        btn.setForeground(C_PRI_TEXT);
        btn.setBackground(C_WHITE);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> action.run());
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setForeground(C_DEEP_GREEN); }
            public void mouseExited(MouseEvent e) { btn.setForeground(C_PRI_TEXT); }
        });
        return btn;
    }

    private void scrollTo(JComponent c) {
        if (c != null) {
            c.scrollRectToVisible(new Rectangle(0, 0, c.getWidth(), c.getHeight()));
        }
    }

    private void scrollToTop() {
        if (scrollPane != null && scrollPane.getVerticalScrollBar() != null) {
            scrollPane.getVerticalScrollBar().setValue(0);
        }
    }

    // ==========================================
    // Inner Classes: ScrollablePanel, RoundPanel, MiniBarChart, VectorIcon, ShadowContainer
    // ==========================================

    /**
     * Scrollable content panel that strictly tracks viewport width.
     * Prevents any horizontal scrollbar from appearing under any screen resolution.
     */
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
            return true; // Crucial: Locks width to viewport width!
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    /** Draws a classpath image centered and proportionally at desktop-friendly hero dimensions. */
    private static final class ResponsiveImagePanel extends JPanel {
        private static final int MAX_IMAGE_WIDTH = 650;
        private final Image image;

        ResponsiveImagePanel(Image image) {
            this.image = image;
            setOpaque(false);
            setPreferredSize(new Dimension(620, 414));
            setMinimumSize(new Dimension(360, 240));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            int availableWidth = Math.max(1, getWidth() - 8);
            int availableHeight = Math.max(1, getHeight() - 8);
            double scale = Math.min(Math.min(availableWidth, MAX_IMAGE_WIDTH) / (double) image.getWidth(this),
                    availableHeight / (double) image.getHeight(this));
            int width = Math.max(1, (int) Math.round(image.getWidth(this) * scale));
            int height = Math.max(1, (int) Math.round(image.getHeight(this) * scale));
            int x = (getWidth() - width) / 2;
            int y = (getHeight() - height) / 2;

            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.drawImage(image, x, y, width, height, this);
            g2.dispose();
        }
    }

    /**
     * Smooth rounded-corner panel with anti-aliasing.
     */
    private static final class RoundPanel extends JPanel {
        private final int radius;
        private Color bg;

        RoundPanel(int radius, Color bg) {
            this.radius = radius;
            this.bg = bg;
            setOpaque(false);
        }

        @Override
        public void setBackground(Color c) {
            this.bg = c;
            super.setBackground(c);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg != null ? bg : Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Subtle multi-layer drop shadow container.
     */
    private static final class ShadowContainer extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            for (int i = 0; i < 5; i++) {
                float alpha = 0.022f - (i * 0.0035f);
                if (alpha <= 0) break;
                g2.setColor(new Color(0f, 0f, 0f, alpha));
                g2.fillRoundRect(i * 2 + 4, i * 2 + 6, getWidth() - (i * 4) - 4, getHeight() - (i * 4) - 6, 20, 20);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Sleek mini bar chart rendered via Java2D with gradient bars, rounded caps, and month labels.
     */
    private static final class MiniBarChart extends JPanel {
        private static final int[] VALUES = {40, 62, 52, 75, 68, 86, 96};
        private static final String[] MONTHS = {"Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep"};
        private static final Color BAR_MUTED = new Color(0x94, 0xD2, 0xB8);
        private static final Color BAR_ACTIVE = new Color(0x18, 0xC7, 0x8A);

        MiniBarChart() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int n = VALUES.length;
            int gap = 8;
            int barWidth = Math.max((w - 24 - (gap * (n - 1))) / n, 10);
            int startX = (w - ((barWidth * n) + (gap * (n - 1)))) / 2;
            int chartHeight = h - 20;

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            FontMetrics fm = g2.getFontMetrics();

            // Background baseline
            g2.setColor(new Color(0xEB, 0xF2, 0xEE));
            g2.drawLine(startX - 6, chartHeight + 2, startX + (barWidth * n) + (gap * (n - 1)) + 6, chartHeight + 2);

            for (int i = 0; i < n; i++) {
                int bh = (int) ((VALUES[i] / 100.0) * (chartHeight - 6));
                int x = startX + (i * (barWidth + gap));
                int y = chartHeight - bh + 2;

                if (i == n - 1) {
                    g2.setColor(BAR_ACTIVE);
                } else {
                    float factor = (float) i / (n - 1) * 0.65f;
                    int r = (int) (BAR_MUTED.getRed() + (BAR_ACTIVE.getRed() - BAR_MUTED.getRed()) * factor);
                    int gr = (int) (BAR_MUTED.getGreen() + (BAR_ACTIVE.getGreen() - BAR_MUTED.getGreen()) * factor);
                    int b = (int) (BAR_MUTED.getBlue() + (BAR_ACTIVE.getBlue() - BAR_MUTED.getBlue()) * factor);
                    g2.setColor(new Color(r, gr, b));
                }

                g2.fillRoundRect(x, y, barWidth, bh, 6, 6);

                // Month Label
                g2.setColor(i == n - 1 ? C_DARK_TEAL : C_SEC_TEXT);
                if (i == n - 1) g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                String label = MONTHS[i];
                int labelX = x + (barWidth - fm.stringWidth(label)) / 2;
                g2.drawString(label, labelX, h - 2);
            }
            g2.dispose();
        }
    }

    /**
     * Vector icon component drawn with crisp Java2D lines and curves.
     * Supports Receipt OCR, Insights, Reports, Search, Export, Security, Check, and Brand.
     */
    private static final class VectorIcon extends JComponent {
        private final String type;
        private final int size;
        private final Color color;

        VectorIcon(String type, int size, Color color) {
            this.type = type;
            this.size = size;
            this.color = color;
            setPreferredSize(new Dimension(size, size));
            setMinimumSize(new Dimension(size, size));
            setMaximumSize(new Dimension(size, size));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            int s = size;
            g2.setColor(color);

            switch (type) {
                case "brand": {
                    // Modern stylized brand mark: outer rounded square + inner accent node
                    g2.setStroke(new BasicStroke(1.8f));
                    g2.drawRoundRect(2, 2, s - 5, s - 5, 6, 6);
                    g2.setColor(C_ACCENT);
                    g2.fillOval(s / 2 - 3, s / 2 - 3, 6, 6);
                    break;
                }
                case "check": {
                    // Checkmark
                    g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(2, s / 2, s / 2 - 1, s - 3);
                    g2.drawLine(s / 2 - 1, s - 3, s - 2, 2);
                    break;
                }
                case "ocr": {
                    // Receipt sheet outline + scan line + detail lines
                    g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawRoundRect(4, 2, s - 8, s - 4, 3, 3);
                    // Receipt detail lines
                    g2.drawLine(7, 7, s - 7, 7);
                    g2.drawLine(7, 11, s - 7, 11);
                    g2.drawLine(7, 15, s - 11, 15);
                    // Optical scanning beam across center
                    g2.setColor(C_ACCENT);
                    g2.setStroke(new BasicStroke(1.8f));
                    g2.drawLine(1, s / 2 + 1, s - 2, s / 2 + 1);
                    break;
                }
                case "insights": {
                    // Upward trending bar chart + polyline
                    g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    int bw = Math.max((s - 12) / 3, 3);
                    // Bar 1
                    g2.drawRoundRect(4, s - 9, bw, 6, 2, 2);
                    // Bar 2
                    g2.drawRoundRect(4 + bw + 2, s - 14, bw, 11, 2, 2);
                    // Bar 3
                    g2.setColor(C_ACCENT);
                    g2.drawRoundRect(4 + (bw + 2) * 2, s - 19, bw, 16, 2, 2);
                    // Trending arrow line
                    g2.setColor(color);
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawLine(4, s - 10, 4 + bw + 2, s - 15);
                    g2.drawLine(4 + bw + 2, s - 15, 4 + (bw + 2) * 2, s - 20);
                    break;
                }
                case "reports": {
                    // Document with folded corner + data grid
                    g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    Path2D p = new Path2D.Float();
                    p.moveTo(4, 2);
                    p.lineTo(s - 8, 2);
                    p.lineTo(s - 4, 6);
                    p.lineTo(s - 4, s - 3);
                    p.lineTo(4, s - 3);
                    p.closePath();
                    g2.draw(p);
                    // Fold flap
                    g2.drawLine(s - 8, 2, s - 8, 6);
                    g2.drawLine(s - 8, 6, s - 4, 6);
                    // Table lines inside
                    g2.drawLine(7, 10, s - 7, 10);
                    g2.drawLine(7, 14, s - 7, 14);
                    g2.drawLine(s / 2, 10, s / 2, s - 6);
                    break;
                }
                case "search": {
                    // Magnifying glass lens + handle + slider tick
                    g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    int lensD = s * 7 / 12;
                    g2.drawOval(3, 3, lensD, lensD);
                    g2.drawLine(3 + lensD - 2, 3 + lensD - 2, s - 3, s - 3);
                    // Filter slider tick
                    g2.setStroke(new BasicStroke(1.4f));
                    g2.drawLine(3, s - 3, 8, s - 3);
                    break;
                }
                case "export": {
                    // Tray box + arrow pointing straight up and out
                    g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    // Tray base
                    g2.drawLine(3, s / 2, 3, s - 4);
                    g2.drawLine(3, s - 4, s - 4, s - 4);
                    g2.drawLine(s - 4, s - 4, s - 4, s / 2);
                    // Upward arrow
                    g2.setColor(C_ACCENT);
                    g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    int cx = s / 2;
                    g2.drawLine(cx, s - 8, cx, 3);
                    g2.drawLine(cx - 4, 7, cx, 3);
                    g2.drawLine(cx + 4, 7, cx, 3);
                    break;
                }
                case "security": {
                    // Security shield with center checkmark
                    g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    Path2D shield = new Path2D.Float();
                    shield.moveTo(3, 4);
                    shield.lineTo(s / 2, 2);
                    shield.lineTo(s - 4, 4);
                    shield.curveTo(s - 4, s * 0.65f, s / 2, s - 2, s / 2, s - 2);
                    shield.curveTo(s / 2, s - 2, 3, s * 0.65f, 3, 4);
                    shield.closePath();
                    g2.draw(shield);
                    // Center checkmark
                    g2.setColor(C_ACCENT);
                    g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(s / 2 - 3, s / 2, s / 2 - 1, s / 2 + 3);
                    g2.drawLine(s / 2 - 1, s / 2 + 3, s / 2 + 4, s / 2 - 3);
                    break;
                }
                case "security_large": {
                    // Large Security Banner Shield with Layered Emblem & Keyhole
                    g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                    // Outer shield glow / silhouette
                    Path2D outer = new Path2D.Float();
                    outer.moveTo(8, 10);
                    outer.lineTo(s / 2, 4);
                    outer.lineTo(s - 9, 10);
                    outer.curveTo(s - 9, s * 0.68f, s / 2, s - 4, s / 2, s - 4);
                    outer.curveTo(s / 2, s - 4, 8, s * 0.68f, 8, 10);
                    outer.closePath();

                    g2.setColor(new Color(C_ACCENT.getRed(), C_ACCENT.getGreen(), C_ACCENT.getBlue(), 35));
                    g2.fill(outer);

                    g2.setColor(C_ACCENT);
                    g2.draw(outer);

                    // Inner Lock / Keyhole emblem
                    int kx = s / 2;
                    int ky = s / 2;
                    // Padlock shackle
                    g2.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawArc(kx - 10, ky - 18, 20, 20, 0, 180);
                    // Padlock body
                    g2.fillRoundRect(kx - 14, ky - 8, 28, 22, 6, 6);
                    // Keyhole in dark teal
                    g2.setColor(C_DARK_TEAL);
                    g2.fillOval(kx - 3, ky - 3, 6, 6);
                    g2.fillRect(kx - 2, ky, 4, 7);
                    break;
                }
                default: {
                    g2.fillOval(2, 2, s - 4, s - 4);
                    break;
                }
            }
            g2.dispose();
        }
    }
}
