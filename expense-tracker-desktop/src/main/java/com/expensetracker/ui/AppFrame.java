package com.expensetracker.ui;

import com.expensetracker.config.AppConfig;
import com.expensetracker.model.Category;
import com.expensetracker.model.Expense;
import com.expensetracker.model.PaymentMethod;
import com.expensetracker.model.ReceiptStatus;
import com.expensetracker.model.User;
import com.expensetracker.repository.CategoryRepository;
import com.expensetracker.repository.SupabaseCategoryRepository;
import com.expensetracker.repository.SupabaseExpenseRepository;
import com.expensetracker.service.AuthService;
import com.expensetracker.service.DefaultReportService;
import com.expensetracker.service.ExpenseService;
import com.expensetracker.service.DemoAuthService;
import com.expensetracker.service.DemoDataService;
import com.expensetracker.service.DemoStorageService;
import com.expensetracker.service.OcrService;
import com.expensetracker.service.ReceiptFileValidator;
import com.expensetracker.service.ReportService;
import com.expensetracker.service.ServiceException;
import com.expensetracker.service.StorageService;
import com.expensetracker.service.SupabaseExpenseService;
import com.expensetracker.service.SupabaseStorageService;
import com.expensetracker.service.Tess4JOcrService;
import com.expensetracker.supabase.SupabaseClient;
import com.expensetracker.ui.components.MetricCard;
import javax.swing.SwingUtilities;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import com.expensetracker.ui.theme.ThemeColors;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AppFrame extends JFrame {
    private final AppConfig config;
    private final User user;
    private final AuthService authService;
    private final ExpenseService expenseService;
    private final ReportService reportService = new DefaultReportService();
    private final CategoryRepository categoryRepository;
    private final StorageService storageService;
    private final OcrService ocrService;
    private final JPanel contentPanel = new JPanel(new BorderLayout());
    private final JLabel pageTitle = new JLabel("Dashboard");

    private List<Expense> allExpenses = List.of();
    private List<Category> categories = List.of();
    private JTable expenseTable;
    private ExpenseTableModel expenseTableModel;
    private JTextField searchField;
    private JComboBox<Category> categoryFilter;
    private JComboBox<PaymentChoice> paymentFilter;
    private JComboBox<ReceiptChoice> receiptFilter;
    private JTextField fromDateField;
    private JTextField toDateField;
    private JLabel expenseStatus;
    private JButton editButton;
    private JButton deleteButton;
    private JButton refreshButton;
    private Runnable onLogoutCallback;
    private ReportService.DetailedReport currentReport;

    public void setOnLogoutCallback(Runnable callback) {
        this.onLogoutCallback = callback;
    }

    public AppFrame(User user, AuthService authService) {
        this(AppConfig.fromEnvironment(), user, authService);
    }

    public AppFrame(AppConfig config, User user, AuthService authService) {
        this(
                config,
                user,
                authService,
                buildExpenseService(config, user, authService),
                buildCategoryRepository(config, authService),
                buildStorageService(config, user, authService),
                new Tess4JOcrService(config.maxReceiptSizeBytes())
        );
    }

    public AppFrame(
            User user,
            AuthService authService,
            ExpenseService expenseService,
            CategoryRepository categoryRepository
    ) {
        this(
                AppConfig.fromEnvironment(),
                user,
                authService,
                expenseService,
                categoryRepository
        );
    }

    public AppFrame(
            AppConfig config,
            User user,
            AuthService authService,
            ExpenseService expenseService,
            CategoryRepository categoryRepository
    ) {
        this(
                config,
                user,
                authService,
                expenseService,
                categoryRepository,
                buildStorageService(config, user, authService),
                new Tess4JOcrService(config.maxReceiptSizeBytes())
        );
    }

    public AppFrame(
            AppConfig config,
            User user,
            AuthService authService,
            ExpenseService expenseService,
            CategoryRepository categoryRepository,
            StorageService storageService,
            OcrService ocrService
    ) {
        super("Expense Tracker");
        this.config = config;
        this.user = user;
        this.authService = authService;
        this.expenseService = expenseService;
        this.categoryRepository = categoryRepository;
        this.storageService = storageService;
        this.ocrService = ocrService;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1180, 760));
        setSize(1280, 820);
        setLocationRelativeTo(null);
        buildUi();
        showDashboard();
    }

    public static AppFrame createDemo(AppConfig config, Runnable onLogout) {
        User demoUser = new User(DemoDataService.DEMO_USER_ID, DemoDataService.DEMO_USER_EMAIL);
        AuthService demoAuth = new DemoAuthService();
        demoAuth.login(demoUser.email(), new char[0]);
        ExpenseService demoExpenseService = new SupabaseExpenseService(
                new DemoDataService.InMemoryExpenseRepository(),
                demoUser.id()
        );
        CategoryRepository demoCategoryRepo = new DemoDataService.InMemoryCategoryRepository();
        StorageService demoStorage = new DemoStorageService();
        OcrService demoOcr = new Tess4JOcrService(config.maxReceiptSizeBytes());

        AppFrame frame = new AppFrame(
                config,
                demoUser,
                demoAuth,
                demoExpenseService,
                demoCategoryRepo,
                demoStorage,
                demoOcr
        );
        frame.setOnLogoutCallback(onLogout);
        return frame;
    }

    private static ExpenseService buildExpenseService(
            AppConfig config,
            User user,
            AuthService authService
    ) {
        SupabaseClient client = new SupabaseClient(config);
        SupabaseExpenseRepository repository =
                new SupabaseExpenseRepository(client, authService::getAccessToken, user.id());
        return new SupabaseExpenseService(repository, user.id());
    }

    private static CategoryRepository buildCategoryRepository(AppConfig config, AuthService authService) {
        return new SupabaseCategoryRepository(new SupabaseClient(config), authService::getAccessToken);
    }

    private static StorageService buildStorageService(
            AppConfig config,
            User user,
            AuthService authService
    ) {
        return new SupabaseStorageService(
                new SupabaseClient(config),
                authService::getAccessToken,
                config.storageBucket(),
                user.id(),
                config.maxReceiptSizeBytes()
        );
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
        navigation.add(navButton("Settings", this::showSettings));
        sidebar.add(navigation, BorderLayout.CENTER);

        sidebar.add(navButton("Log out", this::handleLogout), BorderLayout.SOUTH);
        return sidebar;
    }

    private void handleLogout() {
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to log out?",
                "Log out",
                JOptionPane.YES_NO_OPTION
        );
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        authService.logout();
        dispose();
        if (onLogoutCallback != null) {
            onLogoutCallback.run();
        } else {
            new MainWindow(config, authService).setVisible(true);
        }
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

        MetricCard totalCard = new MetricCard("Total expenses", "All recorded expenses");
        MetricCard monthCard = new MetricCard("This month", "Current calendar month");
        MetricCard countCard = new MetricCard("Expense count", "Transactions recorded");
        MetricCard highestCard = new MetricCard("Highest expense", "Largest single record");
        JPanel metrics = new JPanel(new GridLayout(1, 4, 16, 0));
        metrics.setOpaque(false);
        metrics.add(totalCard);
        metrics.add(monthCard);
        metrics.add(countCard);
        metrics.add(highestCard);

        JPanel header = new JPanel(new BorderLayout(0, 18));
        header.setOpaque(false);
        header.add(intro, BorderLayout.NORTH);
        header.add(metrics, BorderLayout.CENTER);
        dashboard.add(header, BorderLayout.NORTH);

        JPanel recent = new JPanel(new BorderLayout(0, 12));
        recent.setOpaque(false);
        JPanel recentHeader = new JPanel(new BorderLayout(12, 0));
        recentHeader.setOpaque(false);
        JLabel recentTitle = new JLabel("Recent expenses");
        recentTitle.setFont(recentTitle.getFont().deriveFont(Font.BOLD, 18f));
        recentTitle.setForeground(new Color(15, 23, 42));
        JLabel dashboardStatus = new JLabel("Loading expenses...");
        dashboardStatus.setForeground(new Color(100, 116, 139));
        recentHeader.add(recentTitle, BorderLayout.WEST);
        recentHeader.add(dashboardStatus, BorderLayout.EAST);
        recent.add(recentHeader, BorderLayout.NORTH);
        DashboardTableModel tableModel = new DashboardTableModel();
        JTable table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setRowHeight(30);
        recent.add(new JScrollPane(table), BorderLayout.CENTER);
        dashboard.add(recent, BorderLayout.CENTER);
        replaceContent(dashboard);

        new SwingWorker<DashboardPageData, Void>() {
            @Override
            protected DashboardPageData doInBackground() {
                return new DashboardPageData(
                        categoryRepository.findAll(),
                        expenseService.getExpenses(user.id())
                );
            }

            @Override
            protected void done() {
                try {
                    DashboardPageData data = get();
                    categories = data.categories();
                    ReportService.DashboardSummary summary =
                            reportService.generateDashboard(data.expenses(), LocalDate.now());
                    totalCard.setValue(formatAmount(summary.total()));
                    monthCard.setValue(formatAmount(summary.currentMonthTotal()));
                    countCard.setValue(Integer.toString(summary.count()));
                    highestCard.setValue(summary.highestExpense() == null
                            ? "—"
                            : formatAmount(summary.highestExpense().amount()));
                    tableModel.setData(summary.recentExpenses(), data.categories());
                    dashboardStatus.setText(summary.count() == 0
                            ? "No expenses recorded yet."
                            : summary.count() + " "
                                    + (summary.count() == 1 ? "expense" : "expenses") + " loaded");
                } catch (Exception exception) {
                    Throwable cause = exception instanceof java.util.concurrent.ExecutionException
                            && exception.getCause() != null ? exception.getCause() : exception;
                    dashboardStatus.setText(readableError(cause, "Unable to load dashboard data."));
                }
            }
        }.execute();
    }

    private void showExpenses() {
        pageTitle.setText("Expenses");
        JPanel expenses = new JPanel(new BorderLayout(0, 16));
        expenses.setOpaque(false);

        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        JLabel title = new JLabel("Expense management");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headerActions.setOpaque(false);
        refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(event -> refreshExpenses(false));
        JButton addButton = primaryButton("+ Add expense");
        addButton.addActionListener(event -> openExpenseDialog(null));
        headerActions.add(refreshButton);
        headerActions.add(addButton);
        header.add(title, BorderLayout.WEST);
        header.add(headerActions, BorderLayout.EAST);
        expenses.add(header, BorderLayout.NORTH);

        JPanel filterPanel = new JPanel(new GridBagLayout());
        filterPanel.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(0, 0, 8, 10);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        searchField = new JTextField();
        searchField.setToolTipText("Search merchant or description");
        searchField.putClientProperty("JTextField.placeholderText", "Search merchant or description");
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 4;
        filterPanel.add(searchField, constraints);

        categoryFilter = new JComboBox<>();
        paymentFilter = new JComboBox<>();
        receiptFilter = new JComboBox<>();
        installFilterRenderers();
        categoryFilter.addItem(new Category(null, "All categories"));
        paymentFilter.addItem(new PaymentChoice(null, "All payment methods"));
        for (PaymentMethod paymentMethod : PaymentMethod.values()) {
            paymentFilter.addItem(new PaymentChoice(paymentMethod, paymentMethod.displayName()));
        }
        receiptFilter.addItem(new ReceiptChoice(null, "All receipt statuses"));
        for (ReceiptStatus receiptStatus : ReceiptStatus.values()) {
            receiptFilter.addItem(new ReceiptChoice(receiptStatus, receiptStatus.displayName()));
        }
        fromDateField = new JTextField();
        toDateField = new JTextField();
        fromDateField.putClientProperty("JTextField.placeholderText", "YYYY-MM-DD");
        toDateField.putClientProperty("JTextField.placeholderText", "YYYY-MM-DD");
        addFilterField(filterPanel, constraints, 0, 1, "Category", categoryFilter);
        addFilterField(filterPanel, constraints, 1, 1, "Payment method", paymentFilter);
        addFilterField(filterPanel, constraints, 2, 1, "Receipt status", receiptFilter);
        addFilterField(filterPanel, constraints, 3, 1, "From date", fromDateField);
        addFilterField(filterPanel, constraints, 4, 1, "To date", toDateField);

        JButton clearButton = new JButton("Clear filters");
        clearButton.addActionListener(event -> clearFilters());
        constraints.gridx = 5;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        constraints.weightx = 0;
        constraints.insets = new Insets(18, 0, 8, 0);
        filterPanel.add(clearButton, constraints);
        expenses.add(filterPanel, BorderLayout.CENTER);

        expenseTableModel = new ExpenseTableModel();
        expenseTable = new JTable(expenseTableModel);
        expenseTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        expenseTable.setFillsViewportHeight(true);
        expenseTable.setRowHeight(30);

        TableRowSorter<ExpenseTableModel> sorter = new TableRowSorter<>(expenseTableModel);
        sorter.setComparator(0, Comparator.comparing(o -> (LocalDate) o));
        sorter.setComparator(1, String.CASE_INSENSITIVE_ORDER);
        sorter.setComparator(2, String.CASE_INSENSITIVE_ORDER);
        sorter.setComparator(3, Comparator.comparing(o -> new BigDecimal(o.toString())));
        sorter.setComparator(4, String.CASE_INSENSITIVE_ORDER);
        sorter.setComparator(5, String.CASE_INSENSITIVE_ORDER);
        expenseTable.setRowSorter(sorter);

        expenseTable.getColumnModel().getColumn(0).setPreferredWidth(95);
        expenseTable.getColumnModel().getColumn(1).setPreferredWidth(190);
        expenseTable.getColumnModel().getColumn(2).setPreferredWidth(145);
        expenseTable.getColumnModel().getColumn(3).setPreferredWidth(105);
        expenseTable.getColumnModel().getColumn(6).setPreferredWidth(100);
        expenseTable.getSelectionModel().addListSelectionListener(event -> updateSelectionActions());
        expenseTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && expenseTable.getSelectedRow() >= 0) {
                    openExpenseDialog(selectedExpense());
                }
            }
        });

        JPanel tablePanel = new JPanel(new BorderLayout(0, 8));
        tablePanel.setOpaque(false);
        tablePanel.add(new JScrollPane(expenseTable), BorderLayout.CENTER);
        JPanel actionBar = new JPanel(new BorderLayout());
        actionBar.setOpaque(false);
        expenseStatus = new JLabel(" ");
        expenseStatus.setForeground(new Color(100, 116, 139));
        actionBar.add(expenseStatus, BorderLayout.WEST);
        JPanel rowActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rowActions.setOpaque(false);
        editButton = new JButton("Edit selected");
        deleteButton = new JButton("Delete selected");
        deleteButton.setForeground(new Color(185, 28, 28));
        editButton.addActionListener(event -> {
            Expense selected = selectedExpense();
            if (selected != null) {
                openExpenseDialog(selected);
            }
        });
        deleteButton.addActionListener(event -> deleteSelectedExpense());
        rowActions.add(editButton);
        rowActions.add(deleteButton);
        actionBar.add(rowActions, BorderLayout.EAST);
        tablePanel.add(actionBar, BorderLayout.SOUTH);
        expenses.add(tablePanel, BorderLayout.SOUTH);
        replaceContent(expenses);

        addFilterListeners();
        updateSelectionActions();
        refreshExpenses(false);
    }

    private void addFilterField(
            JPanel panel,
            GridBagConstraints template,
            int column,
            int row,
            String label,
            java.awt.Component field
    ) {
        GridBagConstraints constraints = (GridBagConstraints) template.clone();
        constraints.gridx = column;
        constraints.gridy = row;
        constraints.gridwidth = 1;
        constraints.weightx = field instanceof JTextField ? 0.7 : 1;
        JPanel wrapper = new JPanel(new BorderLayout(0, 3));
        wrapper.setOpaque(false);
        JLabel fieldLabel = new JLabel(label);
        fieldLabel.setForeground(new Color(71, 85, 105));
        wrapper.add(fieldLabel, BorderLayout.NORTH);
        wrapper.add(field, BorderLayout.CENTER);
        panel.add(wrapper, constraints);
    }

    private void installFilterRenderers() {
        categoryFilter.setRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                    javax.swing.JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus
            ) {
                return super.getListCellRendererComponent(
                        list,
                        value == null ? "All categories" : value,
                        index,
                        isSelected,
                        cellHasFocus
                );
            }
        });
        paymentFilter.setRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                    javax.swing.JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus
            ) {
                return super.getListCellRendererComponent(
                        list,
                        value == null ? "" : value,
                        index,
                        isSelected,
                        cellHasFocus
                );
            }
        });
        receiptFilter.setRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                    javax.swing.JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus
            ) {
                return super.getListCellRendererComponent(
                        list,
                        value == null ? "" : value,
                        index,
                        isSelected,
                        cellHasFocus
                );
            }
        });
    }

    private void addFilterListeners() {
        searchField.getDocument().addDocumentListener(new SimpleDocumentListener(this::applyFilters));
        fromDateField.getDocument().addDocumentListener(new SimpleDocumentListener(this::applyFilters));
        toDateField.getDocument().addDocumentListener(new SimpleDocumentListener(this::applyFilters));
        categoryFilter.addActionListener(event -> applyFilters());
        paymentFilter.addActionListener(event -> applyFilters());
        receiptFilter.addActionListener(event -> applyFilters());
    }

    private void refreshExpenses(boolean showSuccess) {
        if (expenseTableModel == null) {
            return;
        }
        setExpenseControlsEnabled(false);
        expenseStatus.setText("Loading expenses...");
        new SwingWorker<ExpensePageData, Void>() {
            @Override
            protected ExpensePageData doInBackground() {
                return new ExpensePageData(
                        categoryRepository.findAll(),
                        expenseService.getExpenses(user.id())
                );
            }

            @Override
            protected void done() {
                try {
                    ExpensePageData data = get();
                    categories = data.categories();
                    allExpenses = data.expenses();
                    updateCategoryFilter();
                    applyFilters();
                    if (showSuccess) {
                        expenseStatus.setText("Expenses refreshed.");
                    }
                } catch (Exception exception) {
                    Throwable cause = exception instanceof java.util.concurrent.ExecutionException
                            && exception.getCause() != null ? exception.getCause() : exception;
                    expenseStatus.setText("Unable to load expenses.");
                    showError(cause, "Unable to load expenses.");
                } finally {
                    setExpenseControlsEnabled(true);
                    updateSelectionActions();
                }
            }
        }.execute();
    }

    private void updateCategoryFilter() {
        Category selected = categoryFilter.getItemCount() > 0
                ? (Category) categoryFilter.getSelectedItem() : null;
        categoryFilter.removeAllItems();
        categoryFilter.addItem(new Category(null, "All categories"));
        for (Category category : categories) {
            categoryFilter.addItem(category);
        }
        if (selected != null) {
            for (int index = 0; index < categoryFilter.getItemCount(); index++) {
                Category item = categoryFilter.getItemAt(index);
                if (item.id() != null && item.id().equals(selected.id())) {
                    categoryFilter.setSelectedIndex(index);
                    break;
                }
            }
        }
    }

    private void applyFilters() {
        if (expenseTableModel == null) {
            return;
        }
        LocalDate from = parseFilterDate(fromDateField.getText(), "From date");
        LocalDate to = parseFilterDate(toDateField.getText(), "To date");
        if (!fromDateField.getText().isBlank() && from == null
                || !toDateField.getText().isBlank() && to == null) {
            expenseTableModel.setExpenses(List.of());
            return;
        }
        if (from != null && to != null && from.isAfter(to)) {
            expenseStatus.setText("From date must not be after To date.");
            expenseTableModel.setExpenses(List.of());
            return;
        }

        String query = searchField.getText().trim().toLowerCase(java.util.Locale.ROOT);
        Category selectedCategory = (Category) categoryFilter.getSelectedItem();
        PaymentChoice selectedPayment = (PaymentChoice) paymentFilter.getSelectedItem();
        ReceiptChoice selectedReceipt = (ReceiptChoice) receiptFilter.getSelectedItem();
        List<Expense> filtered = allExpenses.stream()
                .filter(expense -> query.isBlank()
                        || expense.merchant().toLowerCase(java.util.Locale.ROOT).contains(query)
                        || (expense.description() != null
                        && expense.description().toLowerCase(java.util.Locale.ROOT).contains(query)))
                .filter(expense -> selectedCategory == null
                        || selectedCategory.id() == null
                        || selectedCategory.id().equals(expense.categoryId()))
                .filter(expense -> selectedPayment == null
                        || selectedPayment.method() == null
                        || selectedPayment.method() == expense.paymentMethod())
                .filter(expense -> selectedReceipt == null
                        || selectedReceipt.status() == null
                        || selectedReceipt.status() == expense.receiptStatus())
                .filter(expense -> from == null || !expense.expenseDate().isBefore(from))
                .filter(expense -> to == null || !expense.expenseDate().isAfter(to))
                .sorted(Comparator.comparing(Expense::expenseDate).reversed())
                .toList();
        expenseTableModel.setExpenses(filtered);
        String resultLabel = filtered.size() + (filtered.size() == 1 ? " expense" : " expenses");
        expenseStatus.setText(resultLabel);
    }

    private LocalDate parseFilterDate(String value, String label) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException exception) {
            expenseStatus.setText(label + " must use YYYY-MM-DD.");
            return null;
        }
    }

    private void clearFilters() {
        searchField.setText("");
        categoryFilter.setSelectedIndex(0);
        paymentFilter.setSelectedIndex(0);
        receiptFilter.setSelectedIndex(0);
        fromDateField.setText("");
        toDateField.setText("");
        applyFilters();
    }

    private Expense selectedExpense() {
        if (expenseTable == null || expenseTable.getSelectedRow() < 0) {
            return null;
        }
        int modelRow = expenseTable.convertRowIndexToModel(expenseTable.getSelectedRow());
        return expenseTableModel.expenseAt(modelRow);
    }

    private void updateSelectionActions() {
        boolean selected = selectedExpense() != null;
        if (editButton != null) {
            editButton.setEnabled(selected);
        }
        if (deleteButton != null) {
            deleteButton.setEnabled(selected);
        }
    }

    private void deleteSelectedExpense() {
        Expense expense = selectedExpense();
        if (expense == null) {
            return;
        }
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete this expense?\n" + expense.merchant(),
                "Delete expense?",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        setExpenseControlsEnabled(false);
        expenseStatus.setText("Deleting expense...");
        new SwingWorker<Void, Void>() {
            private ExpenseDeleteWorkflow.DeleteResult deleteResult;

            @Override
            protected Void doInBackground() {
                deleteResult = ExpenseDeleteWorkflow.delete(expenseService, allExpenses, expense.id());
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    if (deleteResult.deleted()) {
                        refreshExpenses(true);
                        JOptionPane.showMessageDialog(
                                AppFrame.this,
                                "Expense deleted successfully.",
                                "Expense deleted",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    } else {
                        // Keep the failed row in the UI model and surface the service error.
                        allExpenses = deleteResult.expenses();
                        setExpenseControlsEnabled(true);
                        applyFilters();
                        showError(
                                deleteResult.error(),
                                "The expense could not be deleted. It is still visible."
                        );
                    }
                } catch (Exception exception) {
                    Throwable cause = exception instanceof java.util.concurrent.ExecutionException
                            && exception.getCause() != null ? exception.getCause() : exception;
                    // Unexpected worker failures also leave allExpenses untouched.
                    setExpenseControlsEnabled(true);
                    applyFilters();
                    showError(cause, "The expense could not be deleted. It is still visible.");
                }
            }
        }.execute();
    }

    private void openExpenseDialog(Expense existing) {
        JDialog dialog = new JDialog(
                this,
                existing == null ? "Add expense" : "Edit expense",
                Dialog.ModalityType.APPLICATION_MODAL
        );
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setSize(600, 700);
        dialog.setLocationRelativeTo(this);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 14, 24));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridwidth = 2;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(4, 0, 4, 0);

        JTextField merchantField = new JTextField(existing == null ? "" : existing.merchant());
        JTextField amountField = new JTextField(existing == null ? "" : existing.amount().toPlainString());
        JTextField dateField = new JTextField(
                existing == null ? LocalDate.now().toString() : existing.expenseDate().toString()
        );
        JComboBox<Category> categoryBox = new JComboBox<>();
        categoryBox.addItem(new Category(null, "Select category"));
        categories.forEach(categoryBox::addItem);
        JComboBox<PaymentMethod> paymentBox = new JComboBox<>();
        paymentBox.addItem(null);
        for (PaymentMethod paymentMethod : PaymentMethod.values()) {
            paymentBox.addItem(paymentMethod);
        }
        paymentBox.setRenderer(new PlaceholderRenderer("Select payment method"));
        JTextArea descriptionArea = new JTextArea(existing == null ? "" : safe(existing.description()), 4, 20);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        ReceiptSelection receiptSelection = new ReceiptSelection(
                existing == null ? null : existing.receiptPath(),
                existing == null ? ReceiptStatus.NO_RECEIPT : existing.receiptStatus()
        );
        JLabel receiptLabel = new JLabel();
        JButton attachReceiptButton = new JButton("Attach receipt");
        JButton viewReceiptButton = new JButton("View");
        JButton removeReceiptButton = new JButton("Remove");
        JLabel ocrStatusLabel = new JLabel("OCR will suggest merchant, amount, and date only.");
        ocrStatusLabel.setForeground(new Color(100, 116, 139));
        updateReceiptControls(receiptSelection, receiptLabel, viewReceiptButton, removeReceiptButton);

        if (existing != null) {
            selectCategory(categoryBox, existing.categoryId());
            paymentBox.setSelectedItem(existing.paymentMethod());
        }

        addFormField(form, constraints, 0, "Merchant / expense title", merchantField);
        addFormField(form, constraints, 2, "Amount", amountField);
        addFormField(form, constraints, 4, "Expense date (YYYY-MM-DD)", dateField);
        addFormField(form, constraints, 6, "Category", categoryBox);
        addFormField(form, constraints, 8, "Payment method", paymentBox);
        addFormField(form, constraints, 10, "Description", new JScrollPane(descriptionArea));

        JPanel receiptPanel = new JPanel(new BorderLayout(8, 6));
        receiptPanel.setOpaque(false);
        JPanel receiptActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        receiptActions.setOpaque(false);
        receiptActions.add(attachReceiptButton);
        receiptActions.add(viewReceiptButton);
        receiptActions.add(removeReceiptButton);
        receiptPanel.add(receiptLabel, BorderLayout.NORTH);
        receiptPanel.add(receiptActions, BorderLayout.CENTER);
        receiptPanel.add(ocrStatusLabel, BorderLayout.SOUTH);
        constraints.gridy = 12;
        constraints.insets = new Insets(10, 0, 10, 0);
        form.add(receiptPanel, constraints);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        JButton cancelButton = new JButton("Cancel");
        JButton saveButton = primaryButton(existing == null ? "Save expense" : "Save changes");
        cancelButton.addActionListener(event -> dialog.dispose());
        attachReceiptButton.addActionListener(event -> chooseReceipt(
                dialog,
                receiptSelection,
                receiptLabel,
                viewReceiptButton,
                removeReceiptButton,
                ocrStatusLabel,
                merchantField,
                amountField,
                dateField,
                attachReceiptButton
        ));
        viewReceiptButton.addActionListener(event -> viewReceipt(receiptSelection.receiptPath));
        removeReceiptButton.addActionListener(event -> {
            if (receiptSelection.localFile != null || receiptSelection.receiptPath != null) {
                receiptSelection.localFile = null;
                receiptSelection.removeExisting = receiptSelection.receiptPath != null;
                receiptSelection.receiptPath = null;
                receiptSelection.status = ReceiptStatus.NO_RECEIPT;
                updateReceiptControls(
                        receiptSelection,
                        receiptLabel,
                        viewReceiptButton,
                        removeReceiptButton
                );
                ocrStatusLabel.setText("Receipt will be removed when you save.");
            }
        });
        saveButton.addActionListener(event -> {
            Expense formExpense;
            try {
                formExpense = buildExpenseFromForm(
                        existing,
                        merchantField.getText(),
                        amountField.getText(),
                        dateField.getText(),
                        (Category) categoryBox.getSelectedItem(),
                        (PaymentMethod) paymentBox.getSelectedItem(),
                        descriptionArea.getText()
                );
            } catch (IllegalArgumentException exception) {
                JOptionPane.showMessageDialog(
                        dialog,
                        exception.getMessage(),
                        "Check expense details",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            saveButton.setEnabled(false);
            cancelButton.setEnabled(false);
            saveButton.setText("Saving...");
            new SwingWorker<ReceiptWorkflow.Result, Void>() {
                @Override
                protected ReceiptWorkflow.Result doInBackground() {
                    Expense savedExpense = existing == null
                            ? expenseService.createExpense(formExpense)
                            : expenseService.updateExpense(formExpense);
                    return finishReceiptWorkflow(savedExpense, existing, receiptSelection);
                }

                @Override
                protected void done() {
                    try {
                        ReceiptWorkflow.Result receiptResult = get();
                        dialog.dispose();
                        refreshExpenses(false);
                        JOptionPane.showMessageDialog(
                                AppFrame.this,
                                receiptResult.message(),
                                receiptResult.warning() ? "Expense saved with a receipt warning" : "Expense saved",
                                receiptResult.warning()
                                        ? JOptionPane.WARNING_MESSAGE
                                        : JOptionPane.INFORMATION_MESSAGE
                        );
                    } catch (Exception exception) {
                        Throwable cause = exception instanceof java.util.concurrent.ExecutionException
                                && exception.getCause() != null ? exception.getCause() : exception;
                        saveButton.setEnabled(true);
                        cancelButton.setEnabled(true);
                        saveButton.setText(existing == null ? "Save expense" : "Save changes");
                        showError(cause, "The expense could not be saved.");
                    }
                }
            }.execute();
        });
        actions.add(cancelButton);
        actions.add(saveButton);

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(actions, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void chooseReceipt(
            JDialog dialog,
            ReceiptSelection selection,
            JLabel receiptLabel,
            JButton viewButton,
            JButton removeButton,
            JLabel ocrStatusLabel,
            JTextField merchantField,
            JTextField amountField,
            JTextField dateField,
            JButton attachButton
    ) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose a receipt image");
        chooser.setFileFilter(new FileNameExtensionFilter(
                "Receipt images (JPG, JPEG, PNG)",
                "jpg",
                "jpeg",
                "png"
        ));
        if (chooser.showOpenDialog(dialog) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path file = chooser.getSelectedFile().toPath();
        try {
            ReceiptFileValidator.validate(file, config.maxReceiptSizeBytes());
        } catch (ServiceException exception) {
            JOptionPane.showMessageDialog(
                    dialog,
                    exception.getMessage(),
                    "Receipt not attached",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        selection.localFile = file;
        selection.removeExisting = false;
        updateReceiptControls(selection, receiptLabel, viewButton, removeButton);
        ocrStatusLabel.setText("Reading receipt for editable suggestions...");
        attachButton.setEnabled(false);
        new SwingWorker<com.expensetracker.model.OcrResult, Void>() {
            @Override
            protected com.expensetracker.model.OcrResult doInBackground() {
                return ocrService.processReceipt(file);
            }

            @Override
            protected void done() {
                attachButton.setEnabled(true);
                try {
                    com.expensetracker.model.OcrResult result = get();
                    int suggestions = 0;
                    if (result.merchant() != null && !result.merchant().isBlank()) {
                        merchantField.setText(result.merchant());
                        suggestions++;
                    }
                    if (result.amount() != null && result.amount().signum() > 0) {
                        amountField.setText(result.amount().toPlainString());
                        suggestions++;
                    }
                    if (result.expenseDate() != null) {
                        dateField.setText(result.expenseDate().toString());
                        suggestions++;
                    }
                    ocrStatusLabel.setText(suggestions == 0
                            ? "OCR found no suggestions. Enter the details manually."
                            : "OCR suggested " + suggestions
                            + " field" + (suggestions == 1 ? "" : "s")
                            + ". Review before saving.");
                } catch (Exception exception) {
                    Throwable cause = exception instanceof java.util.concurrent.ExecutionException
                            && exception.getCause() != null ? exception.getCause() : exception;
                    ocrStatusLabel.setText("OCR failed. You can enter the details manually.");
                    JOptionPane.showMessageDialog(
                            dialog,
                            cause.getMessage() == null
                                    ? "OCR failed. You can enter the receipt details manually."
                                    : cause.getMessage(),
                            "Receipt attached without OCR",
                            JOptionPane.WARNING_MESSAGE
                    );
                }
            }
        }.execute();
    }

    private void updateReceiptControls(
            ReceiptSelection selection,
            JLabel receiptLabel,
            JButton viewButton,
            JButton removeButton
    ) {
        if (selection.localFile != null) {
            receiptLabel.setText("Selected: " + selection.localFile.getFileName()
                    + " (upload on save)");
            viewButton.setVisible(false);
            removeButton.setVisible(true);
        } else if (selection.receiptPath != null && !selection.receiptPath.isBlank()) {
            receiptLabel.setText("Receipt status: " + selection.status.displayName());
            viewButton.setVisible(true);
            removeButton.setVisible(true);
        } else {
            receiptLabel.setText("No receipt attached.");
            viewButton.setVisible(false);
            removeButton.setVisible(false);
        }
        receiptLabel.setForeground(new Color(100, 116, 139));
    }

    private void viewReceipt(String receiptPath) {
        if (receiptPath == null || receiptPath.isBlank()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Attach and save a receipt before viewing it.",
                    "No receipt",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }
        new SwingWorker<byte[], Void>() {
            @Override
            protected byte[] doInBackground() {
                return storageService.downloadReceipt(receiptPath);
            }

            @Override
            protected void done() {
                try {
                    showReceiptPreview(get(), receiptPath);
                } catch (Exception exception) {
                    Throwable cause = exception instanceof java.util.concurrent.ExecutionException
                            && exception.getCause() != null ? exception.getCause() : exception;
                    showError(cause, "The receipt preview could not be loaded.");
                }
            }
        }.execute();
    }

    private void showReceiptPreview(byte[] bytes, String receiptPath) {
        ReceiptViewerDialog.showReceipt(this, "Receipt Viewer", bytes, receiptPath);
    }

    private ReceiptWorkflow.Result finishReceiptWorkflow(
            Expense savedExpense,
            Expense existing,
            ReceiptSelection selection
    ) {
        return ReceiptWorkflow.finish(
                expenseService,
                storageService,
                savedExpense,
                existing,
                selection.localFile,
                selection.removeExisting
        );
    }

    private Expense buildExpenseFromForm(
            Expense existing,
            String merchant,
            String amountValue,
            String dateValue,
            Category category,
            PaymentMethod paymentMethod,
            String description
    ) {
        if (merchant == null || merchant.isBlank()) {
            throw new IllegalArgumentException("Merchant is required.");
        }
        BigDecimal amount;
        try {
            amount = new BigDecimal(amountValue.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Amount must be a valid number.");
        }
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
        LocalDate date;
        try {
            date = LocalDate.parse(dateValue.trim());
        } catch (DateTimeParseException | NullPointerException exception) {
            throw new IllegalArgumentException("Date must use YYYY-MM-DD.");
        }
        if (category == null || category.id() == null) {
            throw new IllegalArgumentException("Select a category.");
        }
        if (paymentMethod == null) {
            throw new IllegalArgumentException("Select a payment method.");
        }
        try {
            return new Expense(
                    existing == null ? null : existing.id(),
                    user.id(),
                    category.id(),
                    merchant.trim(),
                    amount,
                    date,
                    paymentMethod,
                    description == null || description.isBlank() ? null : description.trim(),
                    existing == null ? null : existing.receiptPath(),
                    existing == null ? ReceiptStatus.NO_RECEIPT : existing.receiptStatus(),
                    existing == null ? null : existing.createdAt(),
                    Instant.now()
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(exception.getMessage(), exception);
        }
    }

    private void addFormField(
            JPanel form,
            GridBagConstraints template,
            int row,
            String label,
            java.awt.Component field
    ) {
        GridBagConstraints labelConstraints = (GridBagConstraints) template.clone();
        labelConstraints.gridy = row;
        labelConstraints.insets = new Insets(5, 0, 1, 0);
        JLabel fieldLabel = new JLabel(label);
        fieldLabel.setForeground(new Color(51, 65, 85));
        form.add(fieldLabel, labelConstraints);

        GridBagConstraints fieldConstraints = (GridBagConstraints) template.clone();
        fieldConstraints.gridy = row + 1;
        fieldConstraints.insets = new Insets(1, 0, 5, 0);
        form.add(field, fieldConstraints);
    }

    private void selectCategory(JComboBox<Category> box, UUID categoryId) {
        for (int index = 0; index < box.getItemCount(); index++) {
            Category category = box.getItemAt(index);
            if (category.id() != null && category.id().equals(categoryId)) {
                box.setSelectedIndex(index);
                return;
            }
        }
    }

    private void setExpenseControlsEnabled(boolean enabled) {
        if (refreshButton != null) {
            refreshButton.setEnabled(enabled);
        }
        if (editButton != null && !enabled) {
            editButton.setEnabled(false);
        }
        if (deleteButton != null && !enabled) {
            deleteButton.setEnabled(false);
        }
    }

    private void showError(Throwable cause, String fallback) {
        String message = cause instanceof ServiceException && cause.getMessage() != null
                ? cause.getMessage()
                : fallback;
        JOptionPane.showMessageDialog(this, message, "Expense Tracker", JOptionPane.ERROR_MESSAGE);
    }

    private void showReports() {
        pageTitle.setText("Reports & Analytics");
        JPanel reports = new JPanel(new BorderLayout(0, 16));
        reports.setOpaque(false);

        JPanel topControls = new JPanel(new BorderLayout(12, 8));
        topControls.setOpaque(false);

        JPanel dateInputs = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        dateInputs.setOpaque(false);

        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);

        JTextField fromField = new JTextField(monthStart.toString(), 9);
        JTextField toField = new JTextField(today.toString(), 9);
        JButton generateBtn = primaryButton("Generate Report");

        JButton presetMonth = new JButton("This Month");
        JButton preset30 = new JButton("Last 30 Days");
        JButton presetYear = new JButton("This Year");
        JButton presetAll = new JButton("All Time");

        dateInputs.add(new JLabel("From:"));
        dateInputs.add(fromField);
        dateInputs.add(new JLabel("To:"));
        dateInputs.add(toField);
        dateInputs.add(generateBtn);
        dateInputs.add(presetMonth);
        dateInputs.add(preset30);
        dateInputs.add(presetYear);
        dateInputs.add(presetAll);

        JPanel exportActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        exportActions.setOpaque(false);
        JButton exportCsvBtn = new JButton("Export CSV");
        JButton exportPdfBtn = new JButton("Export PDF");
        JButton printBtn = new JButton("Print Summary");
        exportActions.add(exportCsvBtn);
        exportActions.add(exportPdfBtn);
        exportActions.add(printBtn);

        topControls.add(dateInputs, BorderLayout.WEST);
        topControls.add(exportActions, BorderLayout.EAST);
        reports.add(topControls, BorderLayout.NORTH);

        JPanel reportContent = new JPanel();
        reportContent.setLayout(new BoxLayout(reportContent, BoxLayout.Y_AXIS));
        reportContent.setOpaque(false);

        MetricCard repTotal = new MetricCard("Total Spending", "In selected period");
        MetricCard repCount = new MetricCard("Transactions", "Number of records");
        MetricCard repAvg = new MetricCard("Average Expense", "Mean per transaction");
        MetricCard repMax = new MetricCard("Highest Expense", "Largest single purchase");

        JPanel metricsRow = new JPanel(new GridLayout(1, 4, 16, 0));
        metricsRow.setOpaque(false);
        metricsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 105));
        metricsRow.add(repTotal);
        metricsRow.add(repCount);
        metricsRow.add(repAvg);
        metricsRow.add(repMax);
        reportContent.add(metricsRow);
        reportContent.add(Box.createVerticalStrut(16));

        JPanel chartsRow = new JPanel(new GridLayout(1, 2, 16, 0));
        chartsRow.setOpaque(false);
        chartsRow.setPreferredSize(new Dimension(800, 270));
        chartsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 270));

        JPanel catChartPanel = new JPanel(new BorderLayout());
        catChartPanel.setBackground(Color.WHITE);
        catChartPanel.setBorder(BorderFactory.createLineBorder(new Color(0xE2, 0xE8, 0xF0)));

        JPanel payChartPanel = new JPanel(new BorderLayout());
        payChartPanel.setBackground(Color.WHITE);
        payChartPanel.setBorder(BorderFactory.createLineBorder(new Color(0xE2, 0xE8, 0xF0)));

        chartsRow.add(catChartPanel);
        chartsRow.add(payChartPanel);
        reportContent.add(chartsRow);
        reportContent.add(Box.createVerticalStrut(16));

        JPanel tablesRow = new JPanel(new GridLayout(1, 2, 16, 0));
        tablesRow.setOpaque(false);
        tablesRow.setPreferredSize(new Dimension(800, 220));
        tablesRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 240));

        CategoryReportTableModel catTableModel = new CategoryReportTableModel();
        JTable catTable = new JTable(catTableModel);
        catTable.setRowHeight(26);
        JPanel catTableBox = new JPanel(new BorderLayout(0, 6));
        catTableBox.setBackground(Color.WHITE);
        catTableBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE2, 0xE8, 0xF0)),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        JLabel catHeading = new JLabel("Category Breakdown");
        catHeading.setFont(ThemeColors.FONT_SUBHEADING);
        catTableBox.add(catHeading, BorderLayout.NORTH);
        catTableBox.add(new JScrollPane(catTable), BorderLayout.CENTER);

        PaymentReportTableModel payTableModel = new PaymentReportTableModel();
        JTable payTable = new JTable(payTableModel);
        payTable.setRowHeight(26);
        JPanel payTableBox = new JPanel(new BorderLayout(0, 6));
        payTableBox.setBackground(Color.WHITE);
        payTableBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE2, 0xE8, 0xF0)),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        JLabel payHeading = new JLabel("Payment Method Breakdown");
        payHeading.setFont(ThemeColors.FONT_SUBHEADING);
        payTableBox.add(payHeading, BorderLayout.NORTH);
        payTableBox.add(new JScrollPane(payTable), BorderLayout.CENTER);

        tablesRow.add(catTableBox);
        tablesRow.add(payTableBox);
        reportContent.add(tablesRow);

        JScrollPane scroll = new JScrollPane(reportContent);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        reports.add(scroll, BorderLayout.CENTER);

        Runnable runReport = () -> {
            LocalDate f = parseFilterDate(fromField.getText(), "From date");
            LocalDate t = parseFilterDate(toField.getText(), "To date");
            if (!fromField.getText().isBlank() && f == null || !toField.getText().isBlank() && t == null) {
                return;
            }
            if (f != null && t != null && f.isAfter(t)) {
                JOptionPane.showMessageDialog(AppFrame.this, "From date must not be after To date.", "Invalid Date Range", JOptionPane.WARNING_MESSAGE);
                return;
            }

            generateBtn.setEnabled(false);
            new SwingWorker<ReportService.DetailedReport, Void>() {
                @Override
                protected ReportService.DetailedReport doInBackground() {
                    return reportService.generateDetailedReport(allExpenses, categories, f, t);
                }

                @Override
                protected void done() {
                    generateBtn.setEnabled(true);
                    try {
                        currentReport = get();
                        repTotal.setValue(formatAmount(currentReport.totalAmount()));
                        repCount.setValue(String.valueOf(currentReport.count()));
                        repAvg.setValue(formatAmount(currentReport.averageAmount()));
                        repMax.setValue(currentReport.highestExpense() != null
                                ? formatAmount(currentReport.highestExpense().amount()) : "—");

                        catTableModel.setData(currentReport.categoryBreakdowns());
                        payTableModel.setData(currentReport.paymentBreakdowns());

                        // Render Pie Chart
                        DefaultPieDataset pieDataset = new DefaultPieDataset();
                        for (ReportService.CategoryBreakdown cb : currentReport.categoryBreakdowns()) {
                            pieDataset.setValue(cb.categoryName(), cb.totalAmount());
                        }
                        JFreeChart pieChart = ChartFactory.createPieChart("Category Breakdown", pieDataset, true, true, false);
                        catChartPanel.removeAll();
                        catChartPanel.add(new ChartPanel(pieChart), BorderLayout.CENTER);
                        catChartPanel.revalidate();
                        catChartPanel.repaint();

                        // Render Bar Chart
                        DefaultCategoryDataset barDataset = new DefaultCategoryDataset();
                        for (ReportService.PaymentBreakdown pb : currentReport.paymentBreakdowns()) {
                            barDataset.addValue(pb.totalAmount(), "Amount", pb.paymentMethod().displayName());
                        }
                        JFreeChart barChart = ChartFactory.createBarChart("Payment Methods", "Method", "Amount ($)", barDataset, PlotOrientation.VERTICAL, false, true, false);
                        payChartPanel.removeAll();
                        payChartPanel.add(new ChartPanel(barChart), BorderLayout.CENTER);
                        payChartPanel.revalidate();
                        payChartPanel.repaint();

                    } catch (Exception ex) {
                        showError(ex, "Failed to calculate report.");
                    }
                }
            }.execute();
        };

        generateBtn.addActionListener(e -> runReport.run());
        presetMonth.addActionListener(e -> {
            fromField.setText(today.withDayOfMonth(1).toString());
            toField.setText(today.toString());
            runReport.run();
        });
        preset30.addActionListener(e -> {
            fromField.setText(today.minusDays(30).toString());
            toField.setText(today.toString());
            runReport.run();
        });
        presetYear.addActionListener(e -> {
            fromField.setText(today.withDayOfYear(1).toString());
            toField.setText(today.toString());
            runReport.run();
        });
        presetAll.addActionListener(e -> {
            fromField.setText("");
            toField.setText("");
            runReport.run();
        });

        exportCsvBtn.addActionListener(e -> {
            if (currentReport == null) {
                JOptionPane.showMessageDialog(AppFrame.this, "Please generate a report first.", "Export CSV", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new java.io.File("expense-report-" + LocalDate.now() + ".csv"));
            chooser.setFileFilter(new FileNameExtensionFilter("CSV files (*.csv)", "csv"));
            if (chooser.showSaveDialog(AppFrame.this) == JFileChooser.APPROVE_OPTION) {
                java.nio.file.Path dest = chooser.getSelectedFile().toPath();
                new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        reportService.exportToCsv(currentReport, categories, dest);
                        return null;
                    }

                    @Override
                    protected void done() {
                        try {
                            get();
                            JOptionPane.showMessageDialog(AppFrame.this, "Report exported successfully to:\n" + dest, "Export CSV", JOptionPane.INFORMATION_MESSAGE);
                        } catch (Exception ex) {
                            showError(ex, "Failed to export CSV.");
                        }
                    }
                }.execute();
            }
        });

        exportPdfBtn.addActionListener(e -> {
            if (currentReport == null) {
                JOptionPane.showMessageDialog(AppFrame.this, "Please generate a report first.", "Export PDF", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new java.io.File("expense-report-" + LocalDate.now() + ".pdf"));
            chooser.setFileFilter(new FileNameExtensionFilter("PDF files (*.pdf)", "pdf"));
            if (chooser.showSaveDialog(AppFrame.this) == JFileChooser.APPROVE_OPTION) {
                java.nio.file.Path dest = chooser.getSelectedFile().toPath();
                new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        reportService.exportToPdf(currentReport, categories, dest, user.email());
                        return null;
                    }

                    @Override
                    protected void done() {
                        try {
                            get();
                            JOptionPane.showMessageDialog(AppFrame.this, "PDF report created successfully at:\n" + dest, "Export PDF", JOptionPane.INFORMATION_MESSAGE);
                        } catch (Exception ex) {
                            showError(ex, "Failed to generate PDF report.");
                        }
                    }
                }.execute();
            }
        });

        printBtn.addActionListener(e -> {
            try {
                boolean complete = catTable.print(JTable.PrintMode.FIT_WIDTH, new java.text.MessageFormat("Expense Tracker — Category Summary"), new java.text.MessageFormat("Page - {0}"));
                if (complete) {
                    JOptionPane.showMessageDialog(AppFrame.this, "Printing completed.", "Print", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ex) {
                showError(ex, "Printing error occurred.");
            }
        });

        replaceContent(reports);
        SwingUtilities.invokeLater(runReport);
    }

    private void showSettings() {
        pageTitle.setText("Settings & Configuration");
        JPanel settings = new JPanel(new BorderLayout());
        settings.setOpaque(false);

        JPanel formCard = new JPanel(new GridBagLayout());
        formCard.setBackground(Color.WHITE);
        formCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE2, 0xE8, 0xF0)),
                BorderFactory.createEmptyBorder(30, 40, 30, 40)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 0, 8, 20);
        gbc.weightx = 1.0;

        addSettingsRow(formCard, gbc, 0, "Account Email", user.email());
        addSettingsRow(formCard, gbc, 1, "User ID", user.id().toString());
        addSettingsRow(formCard, gbc, 2, "Database Provider", config.isSupabaseConfigured() ? "Supabase PostgreSQL" : "Local In-Memory Demo Storage");
        addSettingsRow(formCard, gbc, 3, "Storage Bucket", config.storageBucket());
        addSettingsRow(formCard, gbc, 4, "Max Receipt File Size", (config.maxReceiptSizeBytes() / (1024 * 1024)) + " MB");
        addSettingsRow(formCard, gbc, 5, "OCR Engine", "Tess4J (Tesseract 5.13 Wrapper)");
        addSettingsRow(formCard, gbc, 6, "Theme Engine", "FlatLaf Light Theme");
        addSettingsRow(formCard, gbc, 7, "Application Version", "0.1.0-SNAPSHOT (Java 22, Swing)");

        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT));
        wrapper.setOpaque(false);
        wrapper.add(formCard);
        settings.add(wrapper, BorderLayout.NORTH);
        replaceContent(settings);
    }

    private void addSettingsRow(JPanel panel, GridBagConstraints gbc, int row, String label, String value) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.3;
        JLabel l = new JLabel(label);
        l.setFont(ThemeColors.FONT_SUBHEADING);
        l.setForeground(ThemeColors.PRIMARY_TEXT);
        panel.add(l, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        JTextField v = new JTextField(value);
        v.setEditable(false);
        v.setBackground(new Color(0xF8, 0xFA, 0xFC));
        panel.add(v, gbc);
    }

    private static final class CategoryReportTableModel extends AbstractTableModel {
        private final String[] cols = {"Category", "Transactions", "Total Amount", "Share (%)"};
        private List<ReportService.CategoryBreakdown> rows = List.of();

        void setData(List<ReportService.CategoryBreakdown> data) {
            rows = new ArrayList<>(data);
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() { return rows.size(); }
        @Override
        public int getColumnCount() { return cols.length; }
        @Override
        public String getColumnName(int c) { return cols[c]; }
        @Override
        public Object getValueAt(int r, int c) {
            ReportService.CategoryBreakdown b = rows.get(r);
            return switch (c) {
                case 0 -> b.categoryName();
                case 1 -> b.count();
                case 2 -> "$" + b.totalAmount().toPlainString();
                case 3 -> String.format("%.1f%%", b.percentage());
                default -> "";
            };
        }
    }

    private static final class PaymentReportTableModel extends AbstractTableModel {
        private final String[] cols = {"Payment Method", "Transactions", "Total Amount", "Share (%)"};
        private List<ReportService.PaymentBreakdown> rows = List.of();

        void setData(List<ReportService.PaymentBreakdown> data) {
            rows = new ArrayList<>(data);
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() { return rows.size(); }
        @Override
        public int getColumnCount() { return cols.length; }
        @Override
        public String getColumnName(int c) { return cols[c]; }
        @Override
        public Object getValueAt(int r, int c) {
            ReportService.PaymentBreakdown b = rows.get(r);
            return switch (c) {
                case 0 -> b.paymentMethod().displayName();
                case 1 -> b.count();
                case 2 -> "$" + b.totalAmount().toPlainString();
                case 3 -> String.format("%.1f%%", b.percentage());
                default -> "";
            };
        }
    }

    private JButton primaryButton(String label) {
        JButton button = new JButton(label);
        button.setBackground(new Color(15, 118, 110));
        button.setForeground(Color.WHITE);
        return button;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String formatAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String readableError(Throwable cause, String fallback) {
        return cause instanceof ServiceException && cause.getMessage() != null
                ? cause.getMessage()
                : fallback;
    }

    private void replaceContent(JPanel panel) {
        contentPanel.removeAll();
        contentPanel.add(panel, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private record ExpensePageData(List<Category> categories, List<Expense> expenses) {
    }

    private record DashboardPageData(List<Category> categories, List<Expense> expenses) {
    }

    private record PaymentChoice(PaymentMethod method, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    private record ReceiptChoice(ReceiptStatus status, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    private static final class ReceiptSelection {
        private String receiptPath;
        private ReceiptStatus status;
        private Path localFile;
        private boolean removeExisting;

        private ReceiptSelection(String receiptPath, ReceiptStatus status) {
            this.receiptPath = receiptPath;
            this.status = status == null ? ReceiptStatus.NO_RECEIPT : status;
        }
    }

    private static final class PlaceholderRenderer extends DefaultListCellRenderer {
        private final String placeholder;

        private PlaceholderRenderer(String placeholder) {
            this.placeholder = placeholder;
        }

        @Override
        public java.awt.Component getListCellRendererComponent(
                javax.swing.JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus
        ) {
            return super.getListCellRendererComponent(
                    list,
                    value == null ? placeholder : value,
                    index,
                    isSelected,
                    cellHasFocus
            );
        }
    }

    private final class ExpenseTableModel extends AbstractTableModel {
        private final String[] columns = {
                "Date", "Merchant", "Category", "Amount", "Payment", "Receipt", "Actions"
        };
        private final Map<UUID, String> categoryNames = new HashMap<>();
        private List<Expense> rows = List.of();

        void setExpenses(List<Expense> expenses) {
            rows = new ArrayList<>(expenses);
            categoryNames.clear();
            for (Category category : categories) {
                categoryNames.put(category.id(), category.name());
            }
            fireTableDataChanged();
            updateSelectionActions();
        }

        Expense expenseAt(int row) {
            return row >= 0 && row < rows.size() ? rows.get(row) : null;
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Expense expense = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> expense.expenseDate();
                case 1 -> expense.merchant();
                case 2 -> categoryNames.getOrDefault(expense.categoryId(), "Unknown category");
                case 3 -> expense.amount().toPlainString();
                case 4 -> expense.paymentMethod().displayName();
                case 5 -> expense.receiptStatus().displayName();
                case 6 -> "Edit / Delete";
                default -> "";
            };
        }
    }

    private final class DashboardTableModel extends AbstractTableModel {
        private final String[] columns = {"Date", "Merchant", "Category", "Amount", "Receipt"};
        private final Map<UUID, String> categoryNames = new HashMap<>();
        private List<Expense> rows = List.of();

        void setData(List<Expense> expenses, List<Category> categories) {
            rows = new ArrayList<>(expenses);
            categoryNames.clear();
            for (Category category : categories) {
                categoryNames.put(category.id(), category.name());
            }
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Expense expense = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> expense.expenseDate();
                case 1 -> expense.merchant();
                case 2 -> categoryNames.getOrDefault(expense.categoryId(), "Unknown category");
                case 3 -> formatAmount(expense.amount());
                case 4 -> expense.receiptStatus().displayName();
                default -> "";
            };
        }
    }

    @FunctionalInterface
    private interface DocumentChange {
        void changed();
    }

    private static final class SimpleDocumentListener implements DocumentListener {
        private final DocumentChange change;

        private SimpleDocumentListener(DocumentChange change) {
            this.change = change;
        }

        @Override
        public void insertUpdate(DocumentEvent event) {
            change.changed();
        }

        @Override
        public void removeUpdate(DocumentEvent event) {
            change.changed();
        }

        @Override
        public void changedUpdate(DocumentEvent event) {
            change.changed();
        }
    }
}