package com.expensetracker.ui;

import com.expensetracker.model.User;
import com.expensetracker.service.AuthService;
import com.expensetracker.ui.components.MetricCard;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;

public final class AppFrame extends JFrame {
    private final User user;
    private final AuthService authService;
    private final JPanel contentPanel = new JPanel(new BorderLayout());
    private final JLabel pageTitle = new JLabel("Dashboard");

    public AppFrame(User user, AuthService authService) {
        super("Expense Tracker");
        this.user = user;
        this.authService = authService;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1180, 760));
        setSize(1280, 820);
        setLocationRelativeTo(null);
        buildUi();
        showDashboard();
    }

    private void buildUi() {
        setLayout(new BorderLayout());
        add(buildSidebar(), BorderLayout.WEST);
        add(buildTopBar(), BorderLayout.NORTH);
        contentPanel.setBackground(new Color(248, 250, 252));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(26, 30, 30, 30));
        add(contentPanel, BorderLayout.CENTER);
    }

    private JPanel buildTopBar() {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(Color.WHITE);
        topBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(18, 26, 18, 26)
        ));
        pageTitle.setFont(pageTitle.getFont().deriveFont(Font.BOLD, 22f));
        pageTitle.setForeground(new Color(15, 23, 42));
        topBar.add(pageTitle, BorderLayout.WEST);
        JLabel userLabel = new JLabel("Personal workspace  •  " + user.email());
        userLabel.setForeground(new Color(100, 116, 139));
        topBar.add(userLabel, BorderLayout.EAST);
        return topBar;
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(new Color(15, 23, 42));
        sidebar.setPreferredSize(new Dimension(230, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(28, 18, 22, 18));

        JLabel brand = new JLabel("expense tracker");
        brand.setForeground(Color.WHITE);
        brand.setFont(brand.getFont().deriveFont(Font.BOLD, 20f));
        sidebar.add(brand, BorderLayout.NORTH);

        JPanel navigation = new JPanel(new GridLayout(0, 1, 0, 8));
        navigation.setOpaque(false);
        navigation.setBorder(BorderFactory.createEmptyBorder(38, 0, 0, 0));
        navigation.add(navButton("Dashboard", this::showDashboard));
        navigation.add(navButton("Expenses", this::showExpenses));
        navigation.add(navButton("Reports", this::showReports));
        sidebar.add(navigation, BorderLayout.CENTER);

        JButton logout = navButton("Log out", this::handleLogout);
        sidebar.add(logout, BorderLayout.SOUTH);
        return sidebar;
    }

    private void handleLogout() {
        int choice = javax.swing.JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to log out?",
                "Log out",
                javax.swing.JOptionPane.YES_NO_OPTION
        );
        if (choice != javax.swing.JOptionPane.YES_OPTION) {
            return;
        }
        authService.logout();
        dispose();
        new LoginFrame(com.expensetracker.config.AppConfig.fromEnvironment(), authService).setVisible(true);
    }

    private JButton navButton(String label, Runnable action) {
        JButton button = new JButton(label);
        button.setHorizontalAlignment(JButton.LEFT);
        button.setForeground(new Color(203, 213, 225));
        button.setBackground(new Color(30, 41, 59));
        button.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        button.addActionListener(event -> action.run());
        return button;
    }

    private void showDashboard() {
        pageTitle.setText("Dashboard");
        JPanel dashboard = new JPanel(new BorderLayout(0, 26));
        dashboard.setOpaque(false);

        JLabel intro = new JLabel("Welcome back");
        intro.setFont(intro.getFont().deriveFont(Font.BOLD, 26f));
        intro.setForeground(new Color(15, 23, 42));

        JPanel metrics = new JPanel(new GridLayout(1, 4, 16, 0));
        metrics.setOpaque(false);
        metrics.add(new MetricCard("Total expenses", "All recorded expenses"));
        metrics.add(new MetricCard("This month", "Current calendar month"));
        metrics.add(new MetricCard("Expense count", "Transactions recorded"));
        metrics.add(new MetricCard("Highest expense", "Largest single record"));

        JPanel header = new JPanel(new BorderLayout(0, 18));
        header.setOpaque(false);
        header.add(intro, BorderLayout.NORTH);
        header.add(metrics, BorderLayout.CENTER);
        dashboard.add(header, BorderLayout.NORTH);

        JPanel recent = new JPanel(new BorderLayout(0, 12));
        recent.setOpaque(false);
        JLabel recentTitle = new JLabel("Recent expenses");
        recentTitle.setFont(recentTitle.getFont().deriveFont(Font.BOLD, 18f));
        recentTitle.setForeground(new Color(15, 23, 42));
        recent.add(recentTitle, BorderLayout.NORTH);
        JTable table = new JTable(new Object[][]{}, new String[]{"Date", "Merchant", "Category", "Amount", "Receipt"});
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        recent.add(new JScrollPane(table), BorderLayout.CENTER);
        dashboard.add(recent, BorderLayout.CENTER);
        replaceContent(dashboard);
    }

    private void showExpenses() {
        pageTitle.setText("Expenses");
        JPanel expenses = new JPanel(new BorderLayout(0, 18));
        expenses.setOpaque(false);
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Expense management");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        JButton addButton = new JButton("+ Add expense");
        addButton.setBackground(new Color(15, 118, 110));
        addButton.setForeground(Color.WHITE);
        addButton.addActionListener(event -> javax.swing.JOptionPane.showMessageDialog(
                this,
                "The expense form will be connected to ExpenseService in the next increment.",
                "Add expense",
                javax.swing.JOptionPane.INFORMATION_MESSAGE
        ));
        header.add(title, BorderLayout.WEST);
        header.add(addButton, BorderLayout.EAST);
        expenses.add(header, BorderLayout.NORTH);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filters.setOpaque(false);
        filters.add(new javax.swing.JTextField("Search merchant or description", 22));
        filters.add(new javax.swing.JComboBox<>(new String[]{"All categories"}));
        filters.add(new javax.swing.JComboBox<>(new String[]{"All payment methods"}));
        filters.add(new javax.swing.JComboBox<>(new String[]{"All receipt statuses"}));
        filters.add(new JButton("Clear filters"));
        expenses.add(filters, BorderLayout.CENTER);
        replaceContent(expenses);
    }

    private void showReports() {
        pageTitle.setText("Reports");
        JPanel reports = new JPanel(new BorderLayout(0, 18));
        reports.setOpaque(false);
        JLabel title = new JLabel("Expense reports");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        reports.add(title, BorderLayout.NORTH);
        JPanel body = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
        body.setOpaque(false);
        body.add(new JLabel("From date"));
        body.add(new javax.swing.JTextField("YYYY-MM-DD", 12));
        body.add(new JLabel("To date"));
        body.add(new javax.swing.JTextField("YYYY-MM-DD", 12));
        body.add(new javax.swing.JComboBox<>(new String[]{"All categories"}));
        body.add(new JButton("Generate report"));
        reports.add(body, BorderLayout.CENTER);
        replaceContent(reports);
    }

    private void replaceContent(JPanel panel) {
        contentPanel.removeAll();
        contentPanel.add(panel, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }
}