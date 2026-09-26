package com.expensetracker.service;

import com.expensetracker.model.Category;
import com.expensetracker.model.Expense;
import com.expensetracker.model.PaymentMethod;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public final class DefaultReportService implements ReportService {

    @Override
    public ReportSummary generateReport(List<Expense> expenses, LocalDate from, LocalDate to) {
        Objects.requireNonNull(expenses, "Expenses are required.");
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("Report start date must not be after its end date.");
        }

        List<Expense> filteredExpenses = expenses.stream()
                .filter(expense -> from == null || !expense.expenseDate().isBefore(from))
                .filter(expense -> to == null || !expense.expenseDate().isAfter(to))
                .toList();
        BigDecimal total = filteredExpenses.stream()
                .map(Expense::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ReportSummary(from, to, filteredExpenses, total, filteredExpenses.size());
    }

    @Override
    public DetailedReport generateDetailedReport(
            List<Expense> expenses,
            List<Category> categories,
            LocalDate from,
            LocalDate to
    ) {
        ReportSummary summary = generateReport(expenses, from, to);
        List<Expense> filtered = summary.expenses();
        BigDecimal total = summary.total();
        int count = summary.count();

        BigDecimal average = count > 0
                ? total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Expense highest = filtered.stream()
                .max(Comparator.comparing(Expense::amount))
                .orElse(null);

        Map<UUID, String> categoryNames = new HashMap<>();
        if (categories != null) {
            for (Category c : categories) {
                categoryNames.put(c.id(), c.name());
            }
        }

        // Category breakdown
        Map<UUID, List<Expense>> byCategory = filtered.stream()
                .collect(Collectors.groupingBy(Expense::categoryId));

        List<CategoryBreakdown> categoryBreakdowns = new ArrayList<>();
        for (Map.Entry<UUID, List<Expense>> entry : byCategory.entrySet()) {
            UUID catId = entry.getKey();
            List<Expense> catExpenses = entry.getValue();
            BigDecimal catTotal = catExpenses.stream()
                    .map(Expense::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            double pct = total.signum() > 0
                    ? catTotal.multiply(BigDecimal.valueOf(100))
                    .divide(total, 2, RoundingMode.HALF_UP)
                    .doubleValue()
                    : 0.0;
            String name = categoryNames.getOrDefault(catId, "Uncategorized");
            categoryBreakdowns.add(new CategoryBreakdown(catId, name, catExpenses.size(), catTotal, pct));
        }
        categoryBreakdowns.sort(Comparator.comparing(CategoryBreakdown::totalAmount).reversed());

        // Payment method breakdown
        Map<PaymentMethod, List<Expense>> byPayment = filtered.stream()
                .collect(Collectors.groupingBy(Expense::paymentMethod));

        List<PaymentBreakdown> paymentBreakdowns = new ArrayList<>();
        for (Map.Entry<PaymentMethod, List<Expense>> entry : byPayment.entrySet()) {
            PaymentMethod method = entry.getKey();
            List<Expense> methodExpenses = entry.getValue();
            BigDecimal methodTotal = methodExpenses.stream()
                    .map(Expense::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            double pct = total.signum() > 0
                    ? methodTotal.multiply(BigDecimal.valueOf(100))
                    .divide(total, 2, RoundingMode.HALF_UP)
                    .doubleValue()
                    : 0.0;
            paymentBreakdowns.add(new PaymentBreakdown(method, methodExpenses.size(), methodTotal, pct));
        }
        paymentBreakdowns.sort(Comparator.comparing(PaymentBreakdown::totalAmount).reversed());

        return new DetailedReport(
                from,
                to,
                filtered,
                total,
                count,
                average,
                highest,
                categoryBreakdowns,
                paymentBreakdowns
        );
    }

    @Override
    public void exportToCsv(DetailedReport report, List<Category> categories, Path destination) throws IOException {
        Objects.requireNonNull(report, "Report is required");
        Objects.requireNonNull(destination, "Destination path is required");

        Map<UUID, String> categoryNames = new HashMap<>();
        if (categories != null) {
            for (Category c : categories) {
                categoryNames.put(c.id(), c.name());
            }
        }

        try (BufferedWriter writer = Files.newBufferedWriter(destination, StandardCharsets.UTF_8)) {
            // Write Summary Metadata
            writer.write("# Expense Tracker Report\n");
            writer.write("# Date Range: " + (report.from() != null ? report.from() : "All")
                    + " to " + (report.to() != null ? report.to() : "All") + "\n");
            writer.write("# Total Spending: " + report.totalAmount().toPlainString() + "\n");
            writer.write("# Total Transactions: " + report.count() + "\n");
            writer.write("# Average Transaction: " + report.averageAmount().toPlainString() + "\n\n");

            // Header
            writer.write("Date,Merchant,Category,Amount,Payment Method,Receipt Status,Notes\n");

            for (Expense expense : report.expenses()) {
                String catName = categoryNames.getOrDefault(expense.categoryId(), "Uncategorized");
                writer.write(String.format(
                        "%s,%s,%s,%s,%s,%s,%s\n",
                        escapeCsv(expense.expenseDate().toString()),
                        escapeCsv(expense.merchant()),
                        escapeCsv(catName),
                        expense.amount().toPlainString(),
                        escapeCsv(expense.paymentMethod().displayName()),
                        escapeCsv(expense.receiptStatus().displayName()),
                        escapeCsv(expense.description() != null ? expense.description() : "")
                ));
            }
        }
    }

    @Override
    public void exportToPdf(DetailedReport report, List<Category> categories, Path destination, String userEmail)
            throws IOException {
        Objects.requireNonNull(report, "Report is required");
        Objects.requireNonNull(destination, "Destination path is required");

        Map<UUID, String> categoryNames = new HashMap<>();
        if (categories != null) {
            for (Category c : categories) {
                categoryNames.put(c.id(), c.name());
            }
        }

        Document document = new Document(PageSize.A4, 36, 36, 40, 40);
        try {
            PdfWriter.getInstance(document, new FileOutputStream(destination.toFile()));
            document.open();

            // Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, new Color(0x06, 0x3B, 0x2E));
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 11, new Color(0x65, 0x73, 0x6D));
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(0x07, 0x5C, 0x45));
            Font cellBoldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);

            // Title
            Paragraph title = new Paragraph("Expense Tracker — Financial Report", titleFont);
            title.setAlignment(Element.ALIGN_LEFT);
            title.setSpacingAfter(4);
            document.add(title);

            String dateRange = (report.from() != null ? report.from().toString() : "Earliest")
                    + " to " + (report.to() != null ? report.to().toString() : "Latest");
            String meta = "User: " + (userEmail != null ? userEmail : "Account")
                    + " | Period: " + dateRange
                    + " | Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            Paragraph subtitle = new Paragraph(meta, subTitleFont);
            subtitle.setSpacingAfter(18);
            document.add(subtitle);

            // Overall Summary Table
            Paragraph summaryTitle = new Paragraph("Overall Summary", sectionFont);
            summaryTitle.setSpacingAfter(8);
            document.add(summaryTitle);

            PdfPTable summaryTable = new PdfPTable(4);
            summaryTable.setWidthPercentage(100);
            summaryTable.setSpacingAfter(18);
            addHeaderCell(summaryTable, "Total Expenses", cellBoldFont);
            addHeaderCell(summaryTable, "Transactions", cellBoldFont);
            addHeaderCell(summaryTable, "Average Transaction", cellBoldFont);
            addHeaderCell(summaryTable, "Highest Expense", cellBoldFont);

            summaryTable.addCell(createCell(report.totalAmount().setScale(2, RoundingMode.HALF_UP).toPlainString(), cellFont));
            summaryTable.addCell(createCell(String.valueOf(report.count()), cellFont));
            summaryTable.addCell(createCell(report.averageAmount().setScale(2, RoundingMode.HALF_UP).toPlainString(), cellFont));
            summaryTable.addCell(createCell(report.highestExpense() != null
                    ? report.highestExpense().amount().setScale(2, RoundingMode.HALF_UP).toPlainString()
                    : "—", cellFont));
            document.add(summaryTable);

            // Category Breakdown Table
            Paragraph catTitle = new Paragraph("Spending by Category", sectionFont);
            catTitle.setSpacingAfter(8);
            document.add(catTitle);

            PdfPTable catTable = new PdfPTable(4);
            catTable.setWidthPercentage(100);
            catTable.setSpacingAfter(18);
            addHeaderCell(catTable, "Category", cellBoldFont);
            addHeaderCell(catTable, "Transactions", cellBoldFont);
            addHeaderCell(catTable, "Total Amount", cellBoldFont);
            addHeaderCell(catTable, "Share (%)", cellBoldFont);

            for (CategoryBreakdown cb : report.categoryBreakdowns()) {
                catTable.addCell(createCell(cb.categoryName(), cellFont));
                catTable.addCell(createCell(String.valueOf(cb.count()), cellFont));
                catTable.addCell(createCell(cb.totalAmount().setScale(2, RoundingMode.HALF_UP).toPlainString(), cellFont));
                catTable.addCell(createCell(String.format("%.1f%%", cb.percentage()), cellFont));
            }
            if (report.categoryBreakdowns().isEmpty()) {
                PdfPCell emptyCell = new PdfPCell(new Phrase("No expenses recorded in this period", cellFont));
                emptyCell.setColspan(4);
                emptyCell.setPadding(6);
                catTable.addCell(emptyCell);
            }
            document.add(catTable);

            // Payment Method Breakdown Table
            Paragraph payTitle = new Paragraph("Spending by Payment Method", sectionFont);
            payTitle.setSpacingAfter(8);
            document.add(payTitle);

            PdfPTable payTable = new PdfPTable(4);
            payTable.setWidthPercentage(100);
            payTable.setSpacingAfter(18);
            addHeaderCell(payTable, "Payment Method", cellBoldFont);
            addHeaderCell(payTable, "Transactions", cellBoldFont);
            addHeaderCell(payTable, "Total Amount", cellBoldFont);
            addHeaderCell(payTable, "Share (%)", cellBoldFont);

            for (PaymentBreakdown pb : report.paymentBreakdowns()) {
                payTable.addCell(createCell(pb.paymentMethod().displayName(), cellFont));
                payTable.addCell(createCell(String.valueOf(pb.count()), cellFont));
                payTable.addCell(createCell(pb.totalAmount().setScale(2, RoundingMode.HALF_UP).toPlainString(), cellFont));
                payTable.addCell(createCell(String.format("%.1f%%", pb.percentage()), cellFont));
            }
            if (report.paymentBreakdowns().isEmpty()) {
                PdfPCell emptyCell = new PdfPCell(new Phrase("No payment transactions recorded", cellFont));
                emptyCell.setColspan(4);
                emptyCell.setPadding(6);
                payTable.addCell(emptyCell);
            }
            document.add(payTable);

            // Itemized Transactions (Up to 100 recent)
            Paragraph itemsTitle = new Paragraph("Itemized Expenses", sectionFont);
            itemsTitle.setSpacingAfter(8);
            document.add(itemsTitle);

            PdfPTable itemTable = new PdfPTable(5);
            itemTable.setWidthPercentage(100);
            itemTable.setWidths(new float[]{2.0f, 3.5f, 2.5f, 2.5f, 2.0f});
            addHeaderCell(itemTable, "Date", cellBoldFont);
            addHeaderCell(itemTable, "Merchant", cellBoldFont);
            addHeaderCell(itemTable, "Category", cellBoldFont);
            addHeaderCell(itemTable, "Payment", cellBoldFont);
            addHeaderCell(itemTable, "Amount", cellBoldFont);

            List<Expense> sorted = report.expenses().stream()
                    .sorted(Comparator.comparing(Expense::expenseDate).reversed())
                    .limit(100)
                    .toList();

            for (Expense exp : sorted) {
                String cat = categoryNames.getOrDefault(exp.categoryId(), "Uncategorized");
                itemTable.addCell(createCell(exp.expenseDate().toString(), cellFont));
                itemTable.addCell(createCell(exp.merchant(), cellFont));
                itemTable.addCell(createCell(cat, cellFont));
                itemTable.addCell(createCell(exp.paymentMethod().displayName(), cellFont));
                itemTable.addCell(createCell(exp.amount().setScale(2, RoundingMode.HALF_UP).toPlainString(), cellFont));
            }
            if (sorted.isEmpty()) {
                PdfPCell emptyCell = new PdfPCell(new Phrase("No expenses found for this date range.", cellFont));
                emptyCell.setColspan(5);
                emptyCell.setPadding(6);
                itemTable.addCell(emptyCell);
            }
            document.add(itemTable);

        } catch (Exception ex) {
            throw new IOException("Failed to generate PDF report: " + ex.getMessage(), ex);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
    }

    private static void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(0xF1, 0xF5, 0xF9));
        cell.setPadding(6);
        table.addCell(cell);
    }

    private static PdfPCell createCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        return cell;
    }

    private static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}