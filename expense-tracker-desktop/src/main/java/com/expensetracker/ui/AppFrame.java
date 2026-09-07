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
import com.expensetracker.service.ExpenseService;
import com.expensetracker.service.ServiceException;
import com.expensetracker.service.SupabaseExpenseService;
import com.expensetracker.supabase.SupabaseClient;
import com.expensetracker.ui.components.MetricCard;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
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
    private final CategoryRepository categoryRepository;
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

    public AppFrame(User user, AuthService authService) {
        this(AppConfig.fromEnvironment(), user, authService);
    }

    public AppFrame(AppConfig config, User user, AuthService authService) {
        this(
                config,
                user,
                authService,
                buildExpenseService(config, user, authService),
                buildCategoryRepository(config, authService)
        );
    }

    public AppFrame(
            User user,
            AuthService authService,
            ExpenseService expenseService,
            CategoryRepository categoryRepository
    ) {
        this(AppConfig.fromEnvironment(), user, authService, expenseService, categoryRepository);
    }

    private AppFrame(
            AppConfig config,
            User user,
            AuthService authService,
            ExpenseService expenseService,
            CategoryRepository categoryRepository
    ) {
        super("Expense Tracker");
        this.config = config;
        this.user = user;
        this.authService = authService;
        this.expenseService = expenseService;
        this.categoryRepository = categoryRepository;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1180, 760));
        setSize(1280, 820);
        setLocationRelativeTo(null);
        buildUi();
        showDashboard();
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
        new LoginFrame(config, authService).setVisible(true);
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
            @Override
            protected Void doInBackground() {
                expenseService.deleteExpense(expense.id());
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    refreshExpenses(true);
                    JOptionPane.showMessageDialog(
                            AppFrame.this,
                            "Expense deleted successfully.",
                            "Expense deleted",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                } catch (Exception exception) {
                    Throwable cause = exception instanceof java.util.concurrent.ExecutionException
                            && exception.getCause() != null ? exception.getCause() : exception;
                    // allExpenses and the table are intentionally untouched on failure.
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
        dialog.setSize(520, 570);
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

        JLabel receiptHint = new JLabel(
                existing == null
                        ? "Receipt attachment will be available in the receipt workflow."
                        : "Receipt status: " + existing.receiptStatus().displayName()
        );
        receiptHint.setForeground(new Color(100, 116, 139));
        constraints.gridy = 12;
        constraints.insets = new Insets(10, 0, 10, 0);
        form.add(receiptHint, constraints);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        JButton cancelButton = new JButton("Cancel");
        JButton saveButton = primaryButton(existing == null ? "Save expense" : "Save changes");
        cancelButton.addActionListener(event -> dialog.dispose());
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
            new SwingWorker<Expense, Void>() {
                @Override
                protected Expense doInBackground() {
                    return existing == null
                            ? expenseService.createExpense(formExpense)
                            : expenseService.updateExpense(formExpense);
                }

                @Override
                protected void done() {
                    try {
                        get();
                        dialog.dispose();
                        refreshExpenses(false);
                        JOptionPane.showMessageDialog(
                                AppFrame.this,
                                existing == null
                                        ? "Expense added successfully."
                                        : "Expense updated successfully.",
                                "Expense saved",
                                JOptionPane.INFORMATION_MESSAGE
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
        pageTitle.setText("Reports");
        JPanel reports = new JPanel(new BorderLayout(0, 18));
        reports.setOpaque(false);
        JLabel title = new JLabel("Expense reports");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        reports.add(title, BorderLayout.NORTH);
        JPanel body = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
        body.setOpaque(false);
        body.add(new JLabel("From date"));
        body.add(new JTextField("YYYY-MM-DD", 12));
        body.add(new JLabel("To date"));
        body.add(new JTextField("YYYY-MM-DD", 12));
        body.add(new JComboBox<>(new String[]{"All categories"}));
        body.add(new JButton("Generate report"));
        reports.add(body, BorderLayout.CENTER);
        replaceContent(reports);
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

    private void replaceContent(JPanel panel) {
        contentPanel.removeAll();
        contentPanel.add(panel, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private record ExpensePageData(List<Category> categories, List<Expense> expenses) {
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