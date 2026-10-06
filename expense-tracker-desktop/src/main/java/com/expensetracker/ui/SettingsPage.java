package com.expensetracker.ui;

import com.expensetracker.config.AppConfig;
import com.expensetracker.model.Budget;
import com.expensetracker.model.Category;
import com.expensetracker.model.Expense;
import com.expensetracker.model.PaymentMethod;
import com.expensetracker.model.User;
import com.expensetracker.repository.BudgetRepository;
import com.expensetracker.repository.CategoryRepository;
import com.expensetracker.service.AuthService;
import com.expensetracker.service.BudgetService;
import com.expensetracker.service.ExpenseService;
import com.expensetracker.service.OcrService;
import com.expensetracker.service.ReportService;
import com.expensetracker.service.ServiceException;
import com.expensetracker.service.StorageService;
import com.expensetracker.service.Tess4JOcrService;
import com.expensetracker.settings.CurrencyOption;
import com.expensetracker.settings.DateStyle;
import com.expensetracker.settings.Formats;
import com.expensetracker.settings.SettingsStore;
import com.expensetracker.settings.UserSettings;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/** The Settings tab: preferences, account, receipts and OCR, budgets, data and privacy, and about. */
public final class SettingsPage extends JPanel {

    /** Everything the page needs from the application window. */
    public record Context(
            JFrame frame,
            AppConfig config,
            User user,
            AuthService authService,
            boolean demoMode,
            ExpenseService expenseService,
            CategoryRepository categoryRepository,
            BudgetRepository budgetRepository,
            StorageService storageService,
            OcrService ocrService,
            ReportService reportService,
            SettingsStore settingsStore,
            Runnable onSignOut,
            Runnable onAccountRemoved,
            Runnable onProfileChanged
    ) {
    }

    private static final Color INK = new Color(0x0F, 0x17, 0x2A);
    private static final Color LABEL = new Color(0x33, 0x41, 0x55);
    private static final Color MUTED = new Color(0x64, 0x74, 0x8B);
    private static final Color BORDER = new Color(0xE2, 0xE8, 0xF0);
    private static final Color TEAL = new Color(0x0F, 0x76, 0x6E);
    private static final Color AMBER = new Color(0xD9, 0x77, 0x06);
    private static final Color DANGER = new Color(0xB9, 0x1C, 0x1C);
    private static final int MAX_WIDTH = 920;

    private final Context ctx;
    private List<BudgetRow> budgetRows = new ArrayList<>();
    private JPanel budgetRowsPanel;
    private Section budgetsSection;
    private BudgetData budgetData;

    public SettingsPage(Context ctx) {
        this.ctx = ctx;
        setOpaque(false);
        setLayout(new BorderLayout());
        build();
    }

    private void build() {
        removeAll();
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        for (Section section : List.of(
                preferencesSection(), accountSection(), receiptsSection(),
                budgetsSection(), dataSection(), aboutSection())) {
            section.setAlignmentX(Component.LEFT_ALIGNMENT);
            column.add(section);
            column.add(Box.createVerticalStrut(18));
        }
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(column, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(wrapper);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll, BorderLayout.CENTER);
        revalidate();
        repaint();
        loadBudgets();
    }

    private UserSettings settings() {
        return ctx.settingsStore().get();
    }

    private void change(Section section, UnaryOperator<UserSettings> edit) {
        ctx.settingsStore().save(edit.apply(settings()));
        section.toast("Saved");
    }

    // ============================================================
    // Preferences
    // ============================================================
    private Section preferencesSection() {
        Section s = new Section("Preferences", "How amounts and dates are shown, and what new expenses start with.", false);
        UserSettings current = settings();

        JComboBox<CurrencyOption> currency = new JComboBox<>(CurrencyOption.values());
        currency.setSelectedItem(current.currency());
        currency.addActionListener(e -> change(s, st -> st.withCurrency((CurrencyOption) currency.getSelectedItem())));
        s.row("Currency", currency, "Changes how amounts are displayed. Stored amounts are not converted.");

        JComboBox<DateStyle> dates = new JComboBox<>(DateStyle.values());
        dates.setSelectedItem(current.dateStyle());
        dates.addActionListener(e -> change(s, st -> st.withDateStyle((DateStyle) dates.getSelectedItem())));
        s.row("Date format", dates, "Used in tables and when typing dates. YYYY-MM-DD is always accepted.");

        JComboBox<Choice<PaymentMethod>> payment = new JComboBox<>();
        payment.addItem(new Choice<>(null, "No default"));
        for (PaymentMethod method : PaymentMethod.values()) {
            payment.addItem(new Choice<>(method, method.displayName()));
        }
        selectChoice(payment, current.defaultPayment());
        payment.addActionListener(e -> change(s, st -> st.withDefaultPayment(selected(payment))));
        s.row("Default payment method", payment, "Preselected when you add an expense.");

        JComboBox<Choice<String>> category = new JComboBox<>();
        category.addItem(new Choice<>(null, "No default"));
        if (current.defaultCategory() != null) {
            category.addItem(new Choice<>(current.defaultCategory(), current.defaultCategory()));
            category.setSelectedIndex(1);
        }
        boolean[] populating = {true};
        category.addActionListener(e -> {
            if (!populating[0]) {
                change(s, st -> st.withDefaultCategory(selected(category)));
            }
        });
        s.row("Default category", category, "Preselected when you add an expense.");
        async(ctx.categoryRepository()::findAll, categories -> {
            String saved = settings().defaultCategory();
            category.removeAllItems();
            category.addItem(new Choice<>(null, "No default"));
            for (Category c : categories) {
                category.addItem(new Choice<>(c.name(), c.name()));
            }
            selectChoice(category, saved);
            populating[0] = false;
        }, failure -> populating[0] = false);
        return s;
    }

    // ============================================================
    // Account
    // ============================================================
    private Section accountSection() {
        Section s = new Section("Account", ctx.demoMode()
                ? "You are using offline demo mode. Account changes are not available."
                : "Your sign-in details.", false);

        JTextField email = new JTextField(ctx.user().email(), 26);
        email.setEditable(false);
        s.row("Email", email, "Your sign-in email cannot be changed here.");

        JTextField name = new JTextField(ctx.authService().getDisplayName(), 20);
        JButton saveName = secondaryButton("Save name");
        JPanel nameRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        nameRow.setOpaque(false);
        nameRow.add(name);
        nameRow.add(saveName);
        name.setEnabled(!ctx.demoMode());
        saveName.setEnabled(!ctx.demoMode());
        saveName.addActionListener(e -> {
            saveName.setEnabled(false);
            async(() -> {
                ctx.authService().updateDisplayName(name.getText());
                return null;
            }, ok -> {
                saveName.setEnabled(true);
                s.toast("Name saved");
                ctx.onProfileChanged().run();
            }, failure -> {
                saveName.setEnabled(true);
                showError(failure, "The name could not be saved.");
            });
        });
        s.row("Display name", nameRow, "Shown in the top bar.");

        JButton password = secondaryButton("Change password…");
        password.setEnabled(!ctx.demoMode());
        password.addActionListener(e -> changePasswordDialog());
        JButton signOut = secondaryButton("Sign out");
        signOut.addActionListener(e -> ctx.onSignOut().run());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(password);
        buttons.add(signOut);
        s.row("Security", buttons, null);
        return s;
    }

    private void changePasswordDialog() {
        JPasswordField first = new JPasswordField(18);
        JPasswordField second = new JPasswordField(18);
        Object[] form = {"New password (at least 6 characters)", first, "Confirm new password", second};
        int choice = JOptionPane.showConfirmDialog(ctx.frame(), form, "Change password",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }
        char[] one = first.getPassword();
        char[] two = second.getPassword();
        try {
            if (one.length < 6) {
                warn("Password too short", "Use at least 6 characters.");
                return;
            }
            if (!Arrays.equals(one, two)) {
                warn("Passwords do not match", "Type the same password in both fields.");
                return;
            }
        } finally {
            Arrays.fill(two, '\0');
        }
        async(() -> {
            ctx.authService().changePassword(one);
            return null;
        }, ok -> JOptionPane.showMessageDialog(ctx.frame(), "Your password has been changed.",
                "Password changed", JOptionPane.INFORMATION_MESSAGE),
                failure -> showError(failure, "The password could not be changed."));
    }

    // ============================================================
    // Receipts & OCR
    // ============================================================
    private Section receiptsSection() {
        Section s = new Section("Receipts & OCR", "How receipt images are read when you attach them to an expense.", false);
        UserSettings current = settings();

        Map<String, String> installed = Tess4JOcrService.installedLanguages();
        JComboBox<Choice<String>> language = new JComboBox<>();
        installed.forEach((code, label) -> language.addItem(new Choice<>(code, label + " (" + code + ")")));
        if (!installed.containsKey(current.ocrLanguage())) {
            language.addItem(new Choice<>(current.ocrLanguage(), current.ocrLanguage() + " (not installed)"));
        }
        selectChoice(language, current.ocrLanguage());
        language.addActionListener(e -> {
            String code = selected(language);
            ctx.ocrService().useLanguage(code);
            change(s, st -> st.withOcrLanguage(code));
        });
        s.row("Recognition language", language,
                "Only languages with a .traineddata file in your tessdata folder are listed.");

        JCheckBox auto = new JCheckBox("Read receipts automatically when attached", current.autoScan());
        auto.setOpaque(false);
        auto.addActionListener(e -> change(s, st -> st.withAutoScan(auto.isSelected())));
        s.row("Auto-scan", auto, "When off, receipts are attached without suggesting merchant, amount or date.");

        s.row("Max receipt size", readOnly((ctx.config().maxReceiptSizeBytes() / (1024 * 1024)) + " MB"), null);
        return s;
    }

    // ============================================================
    // Budgets & alerts
    // ============================================================
    private Section budgetsSection() {
        budgetsSection = new Section("Budgets & alerts",
                "Monthly spending limits. You get a warning when an expense takes a budget past your alert level.", false);

        JSpinner threshold = new JSpinner(new SpinnerNumberModel(settings().alertThreshold(), 50, 100, 5));
        threshold.addChangeListener(e -> {
            change(budgetsSection, st -> st.withAlertThreshold((Integer) threshold.getValue()));
            refreshBudgetBars();
        });
        JPanel thresholdRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        thresholdRow.setOpaque(false);
        thresholdRow.add(threshold);
        thresholdRow.add(new JLabel("% of a budget"));
        budgetsSection.row("Warn me at", thresholdRow, "Bars turn amber at this level and red once a budget is exceeded.");

        budgetRowsPanel = new JPanel();
        budgetRowsPanel.setOpaque(false);
        budgetRowsPanel.setLayout(new BoxLayout(budgetRowsPanel, BoxLayout.Y_AXIS));
        budgetRowsPanel.add(muted("Loading budgets…"));
        budgetsSection.fullWidth(budgetRowsPanel);
        return budgetsSection;
    }

    private record BudgetData(List<Category> categories, List<Budget> budgets, List<Expense> expenses) {
    }

    private final class BudgetRow {
        final UUID categoryId;
        final String name;
        final Budget existing;
        final JTextField field = new JTextField(9);
        final JProgressBar bar = new JProgressBar(0, 100);

        BudgetRow(UUID categoryId, String name, Budget existing) {
            this.categoryId = categoryId;
            this.name = name;
            this.existing = existing;
            field.setText(existing == null ? "" : existing.monthlyLimit().stripTrailingZeros().toPlainString());
            bar.setStringPainted(true);
            bar.setPreferredSize(new Dimension(300, 22));
        }
    }

    private void loadBudgets() {
        async(() -> new BudgetData(
                ctx.categoryRepository().findAll(),
                ctx.budgetRepository().findAll(),
                ctx.expenseService().getExpenses(ctx.user().id())
        ), data -> {
            budgetData = data;
            showBudgetRows();
        }, failure -> {
            budgetData = null;
            budgetRowsPanel.removeAll();
            JLabel message = new JLabel("<html><body style='width:560px'>"
                    + escape(messageOf(failure, "Budgets could not be loaded.")) + "</body></html>");
            message.setForeground(DANGER);
            message.setAlignmentX(Component.LEFT_ALIGNMENT);
            JButton retry = secondaryButton("Try again");
            retry.setAlignmentX(Component.LEFT_ALIGNMENT);
            retry.addActionListener(e -> {
                budgetRowsPanel.removeAll();
                budgetRowsPanel.add(muted("Loading budgets…"));
                budgetRowsPanel.revalidate();
                loadBudgets();
            });
            budgetRowsPanel.add(message);
            budgetRowsPanel.add(Box.createVerticalStrut(8));
            budgetRowsPanel.add(retry);
            budgetRowsPanel.revalidate();
            budgetRowsPanel.repaint();
        });
    }

    private void showBudgetRows() {
        budgetRowsPanel.removeAll();
        budgetRows = new ArrayList<>();
        Budget overall = budgetData.budgets().stream().filter(b -> b.categoryId() == null).findFirst().orElse(null);
        budgetRows.add(new BudgetRow(null, "Overall monthly budget", overall));
        for (Category category : budgetData.categories()) {
            Budget existing = budgetData.budgets().stream()
                    .filter(b -> Objects.equals(b.categoryId(), category.id())).findFirst().orElse(null);
            budgetRows.add(new BudgetRow(category.id(), category.name(), existing));
        }

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        String symbol = new Formats(settings()).currencySymbol();
        int rowIndex = 0;
        for (BudgetRow row : budgetRows) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridy = rowIndex++;
            c.insets = new Insets(5, 0, 5, 12);
            c.anchor = GridBagConstraints.WEST;
            c.gridx = 0;
            c.weightx = 0;
            JLabel name = new JLabel(row.name);
            name.setForeground(LABEL);
            name.setFont(row.categoryId == null ? name.getFont().deriveFont(Font.BOLD) : name.getFont());
            name.setPreferredSize(new Dimension(190, 24));
            grid.add(name, c);
            c.gridx = 1;
            JPanel limit = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            limit.setOpaque(false);
            limit.add(new JLabel(symbol));
            limit.add(row.field);
            grid.add(limit, c);
            c.gridx = 2;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            grid.add(row.bar, c);
        }
        budgetRowsPanel.add(grid);
        budgetRowsPanel.add(Box.createVerticalStrut(10));

        JButton save = primaryButton("Save budgets");
        save.setAlignmentX(Component.LEFT_ALIGNMENT);
        save.addActionListener(e -> saveBudgets(save));
        JLabel hint = muted("Leave a limit empty for no budget. Bars show spending in " + YearMonth.now().getMonth()
                .getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH) + ".");
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        budgetRowsPanel.add(save);
        budgetRowsPanel.add(Box.createVerticalStrut(6));
        budgetRowsPanel.add(hint);
        refreshBudgetBars();
        budgetRowsPanel.revalidate();
        budgetRowsPanel.repaint();
    }

    private void refreshBudgetBars() {
        if (budgetData == null || budgetRows.isEmpty()) {
            return;
        }
        Formats formats = new Formats(settings());
        List<Budget> saved = budgetData.budgets();
        List<BudgetService.Status> statuses = BudgetService.evaluate(
                saved, budgetData.expenses(), budgetData.categories(), YearMonth.now(), settings().alertThreshold());
        for (BudgetRow row : budgetRows) {
            BudgetService.Status status = statuses.stream()
                    .filter(st -> Objects.equals(st.categoryId(), row.categoryId)).findFirst().orElse(null);
            if (status == null) {
                row.bar.setValue(0);
                row.bar.setString("No budget set");
                row.bar.setForeground(BORDER);
            } else {
                row.bar.setValue((int) Math.min(100, Math.round(status.percent())));
                row.bar.setString(formats.money(status.spent()) + " of " + formats.money(status.limit())
                        + " (" + Math.round(status.percent()) + "%)");
                row.bar.setForeground(colorFor(status.level()));
            }
        }
    }

    static Color colorFor(BudgetService.Level level) {
        return switch (level) {
            case OK -> TEAL;
            case WARNING -> AMBER;
            case EXCEEDED -> new Color(0xDC, 0x26, 0x26);
        };
    }

    private void saveBudgets(JButton saveButton) {
        record Change(BudgetRow row, BigDecimal value) {
        }
        List<Change> changes = new ArrayList<>();
        for (BudgetRow row : budgetRows) {
            String text = row.field.getText().trim().replace(",", "");
            BigDecimal value = null;
            if (!text.isEmpty()) {
                try {
                    value = new BigDecimal(text);
                } catch (NumberFormatException exception) {
                    warn("Check the budget", "Enter a valid amount for \"" + row.name + "\".");
                    return;
                }
                if (value.signum() <= 0) {
                    warn("Check the budget", "The limit for \"" + row.name + "\" must be greater than zero.");
                    return;
                }
                value = value.setScale(2, java.math.RoundingMode.HALF_UP);
            }
            boolean unchanged = value == null
                    ? row.existing == null
                    : row.existing != null && row.existing.monthlyLimit().compareTo(value) == 0;
            if (!unchanged) {
                changes.add(new Change(row, value));
            }
        }
        if (changes.isEmpty()) {
            budgetsSection.toast("No changes");
            return;
        }
        saveButton.setEnabled(false);
        async(() -> {
            for (Change change : changes) {
                if (change.value() == null) {
                    ctx.budgetRepository().delete(change.row().existing.id());
                } else {
                    ctx.budgetRepository().save(change.row().categoryId, change.value());
                }
            }
            return null;
        }, ok -> {
            budgetsSection.toast("Budgets saved");
            loadBudgets();
        }, failure -> {
            saveButton.setEnabled(true);
            showError(failure, "The budgets could not be saved.");
        });
    }

    // ============================================================
    // Data & privacy
    // ============================================================
    private Section dataSection() {
        Section s = new Section("Data & privacy", "Take your data with you, reset this app, or leave.", false);

        JButton csv = secondaryButton("Export CSV");
        JButton pdf = secondaryButton("Export PDF");
        csv.addActionListener(e -> exportAll(false));
        pdf.addActionListener(e -> exportAll(true));
        JPanel exports = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        exports.setOpaque(false);
        exports.add(csv);
        exports.add(pdf);
        s.row("Export all my expenses", exports, "Includes every expense, not just the current report period.");

        JButton reset = secondaryButton("Reset preferences");
        reset.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(ctx.frame(),
                    "Reset currency, date format, defaults, OCR options and alert level to their defaults?\n"
                            + "Your expenses and budgets are not affected.",
                    "Reset preferences", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (choice == JOptionPane.OK_OPTION) {
                ctx.settingsStore().reset();
                ctx.ocrService().useLanguage(settings().ocrLanguage());
                build();
            }
        });
        s.row("Preferences", reset, "Restores the defaults on this computer.");

        JButton delete = new JButton("Delete account…");
        delete.setForeground(DANGER);
        delete.setEnabled(!ctx.demoMode());
        delete.addActionListener(e -> confirmDeleteAccount(delete));
        s.row("Delete account", delete, ctx.demoMode()
                ? "Not available in offline demo mode."
                : "Permanently removes your login, expenses, budgets and receipt files.");
        return s;
    }

    private void exportAll(boolean pdf) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("all-expenses-" + LocalDate.now() + (pdf ? ".pdf" : ".csv")));
        chooser.setFileFilter(pdf
                ? new FileNameExtensionFilter("PDF files (*.pdf)", "pdf")
                : new FileNameExtensionFilter("CSV files (*.csv)", "csv"));
        if (chooser.showSaveDialog(ctx.frame()) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path destination = chooser.getSelectedFile().toPath();
        async(() -> {
            List<Expense> expenses = ctx.expenseService().getExpenses(ctx.user().id());
            List<Category> categories = ctx.categoryRepository().findAll();
            ReportService.DetailedReport report = ctx.reportService().generateDetailedReport(expenses, categories, null, null);
            if (pdf) {
                ctx.reportService().exportToPdf(report, categories, destination, ctx.user().email());
            } else {
                ctx.reportService().exportToCsv(report, categories, destination);
            }
            return expenses.size();
        }, count -> JOptionPane.showMessageDialog(ctx.frame(),
                count + (count == 1 ? " expense" : " expenses") + " exported to:\n" + destination,
                "Export complete", JOptionPane.INFORMATION_MESSAGE),
                failure -> showError(failure, "The export failed."));
    }

    private void confirmDeleteAccount(JButton button) {
        JTextField typed = new JTextField(12);
        Object[] message = {
                "This permanently deletes your login, all expenses, budgets and receipt files.\nThis cannot be undone.",
                "Type DELETE to confirm:", typed};
        int choice = JOptionPane.showConfirmDialog(ctx.frame(), message, "Delete account",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }
        if (!"DELETE".equals(typed.getText().trim())) {
            warn("Account not deleted", "You must type DELETE exactly to confirm.");
            return;
        }
        button.setEnabled(false);
        async(() -> {
            ctx.authService().deleteAccount(false);
            int failedReceipts = 0;
            for (Expense expense : ctx.expenseService().getExpenses(ctx.user().id())) {
                String path = expense.receiptPath();
                if (path != null && !path.isBlank()) {
                    try {
                        ctx.storageService().deleteReceipt(path);
                    } catch (ServiceException exception) {
                        failedReceipts++;
                    }
                }
            }
            ctx.authService().deleteAccount(true);
            return failedReceipts;
        }, failedReceipts -> {
            ctx.settingsStore().clear();
            ctx.authService().logout();
            JOptionPane.showMessageDialog(ctx.frame(),
                    "Your account and data have been deleted."
                            + (failedReceipts > 0 ? "\n" + failedReceipts + " receipt file(s) could not be removed from storage." : ""),
                    "Account deleted", JOptionPane.INFORMATION_MESSAGE);
            ctx.onAccountRemoved().run();
        }, failure -> {
            button.setEnabled(true);
            showError(failure, "The account could not be deleted.");
        });
    }

    // ============================================================
    // About
    // ============================================================
    private Section aboutSection() {
        Section s = new Section("About", "Technical details about this installation.", false);
        s.row("Version", readOnly("0.1.0-SNAPSHOT"), null);
        s.row("Database", readOnly(ctx.config().isSupabaseConfigured() && !ctx.demoMode()
                ? "Supabase PostgreSQL" : "Local in-memory demo storage"), null);
        s.row("Receipt storage", readOnly(ctx.demoMode() ? "Local demo storage" : ctx.config().storageBucket()), null);
        s.row("OCR engine", readOnly("Tess4J (Tesseract)"), null);
        return s;
    }

    // ============================================================
    // Helpers
    // ============================================================
    private <T> void async(Callable<T> work, Consumer<T> success, Consumer<Throwable> failure) {
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return work.call();
            }

            @Override
            protected void done() {
                try {
                    success.accept(get());
                } catch (ExecutionException exception) {
                    failure.accept(exception.getCause() == null ? exception : exception.getCause());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    failure.accept(exception);
                }
            }
        }.execute();
    }

    private void showError(Throwable cause, String fallback) {
        JOptionPane.showMessageDialog(ctx.frame(), messageOf(cause, fallback), "Settings", JOptionPane.ERROR_MESSAGE);
    }

    private void warn(String title, String message) {
        JOptionPane.showMessageDialog(ctx.frame(), message, title, JOptionPane.WARNING_MESSAGE);
    }

    static String messageOf(Throwable cause, String fallback) {
        return cause instanceof ServiceException && cause.getMessage() != null ? cause.getMessage() : fallback;
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static JLabel muted(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(MUTED);
        label.setFont(label.getFont().deriveFont(12f));
        return label;
    }

    private static JTextField readOnly(String text) {
        JTextField field = new JTextField(text, 26);
        field.setEditable(false);
        field.setBackground(new Color(0xF8, 0xFA, 0xFC));
        return field;
    }

    private static JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(TEAL);
        button.setForeground(Color.WHITE);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private static JButton secondaryButton(String text) {
        JButton button = new JButton(text);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private static <T> T selected(JComboBox<Choice<T>> box) {
        return box.getItemAt(box.getSelectedIndex()).value();
    }

    private static <T> void selectChoice(JComboBox<Choice<T>> box, T value) {
        for (int i = 0; i < box.getItemCount(); i++) {
            if (Objects.equals(box.getItemAt(i).value(), value)) {
                box.setSelectedIndex(i);
                return;
            }
        }
        box.setSelectedIndex(0);
    }

    /** A combo-box entry with a label and an optional (nullable) value. */
    private record Choice<T>(T value, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    /** A titled white card with label/control rows and a small "Saved" confirmation. */
    private static final class Section extends JPanel {
        private final JPanel grid = new JPanel(new GridBagLayout());
        private final JLabel toast = new JLabel(" ");
        private Timer toastTimer;
        private int row;

        Section(String title, String subtitle, boolean danger) {
            setLayout(new BorderLayout(0, 14));
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(danger ? new Color(0xFE, 0xCA, 0xCA) : BORDER),
                    BorderFactory.createEmptyBorder(20, 26, 22, 26)));

            JPanel head = new JPanel(new BorderLayout(12, 0));
            head.setOpaque(false);
            JPanel titles = new JPanel();
            titles.setOpaque(false);
            titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
            JLabel heading = new JLabel(title);
            heading.setFont(heading.getFont().deriveFont(Font.BOLD, 17f));
            heading.setForeground(INK);
            JLabel sub = muted(subtitle);
            heading.setAlignmentX(Component.LEFT_ALIGNMENT);
            sub.setAlignmentX(Component.LEFT_ALIGNMENT);
            titles.add(heading);
            titles.add(Box.createVerticalStrut(3));
            titles.add(sub);
            toast.setForeground(new Color(0x15, 0x80, 0x3D));
            toast.setFont(toast.getFont().deriveFont(Font.BOLD, 12f));
            head.add(titles, BorderLayout.CENTER);
            head.add(toast, BorderLayout.EAST);

            grid.setOpaque(false);
            add(head, BorderLayout.NORTH);
            add(grid, BorderLayout.CENTER);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(MAX_WIDTH, getPreferredSize().height);
        }

        void row(String label, JComponent control, String hint) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridy = row++;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.insets = new Insets(8, 0, 8, 18);
            c.gridx = 0;
            JLabel name = new JLabel(label);
            name.setForeground(LABEL);
            name.setFont(name.getFont().deriveFont(Font.BOLD));
            name.setPreferredSize(new Dimension(190, 28));
            grid.add(name, c);

            JPanel cell = new JPanel();
            cell.setOpaque(false);
            cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
            JPanel holder = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            holder.setOpaque(false);
            holder.add(control);
            holder.setAlignmentX(Component.LEFT_ALIGNMENT);
            cell.add(holder);
            if (hint != null) {
                JLabel help = muted(hint);
                help.setAlignmentX(Component.LEFT_ALIGNMENT);
                cell.add(Box.createVerticalStrut(4));
                cell.add(help);
            }
            c.gridx = 1;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(8, 0, 8, 0);
            grid.add(cell, c);
        }

        void fullWidth(JComponent content) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridy = row++;
            c.gridx = 0;
            c.gridwidth = 2;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(6, 0, 0, 0);
            grid.add(content, c);
        }

        void toast(String text) {
            toast.setText(text);
            if (toastTimer != null) {
                toastTimer.stop();
            }
            toastTimer = new Timer(2200, e -> toast.setText(" "));
            toastTimer.setRepeats(false);
            toastTimer.start();
        }
    }
}
