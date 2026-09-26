package com.expensetracker.service;

import com.expensetracker.model.Category;
import com.expensetracker.model.Expense;
import com.expensetracker.model.PaymentMethod;
import com.expensetracker.model.ReceiptStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReportServiceTest {
    private DefaultReportService reportService;
    private UUID userId;
    private Category foodCat;
    private Category travelCat;
    private List<Category> categories;
    private List<Expense> expenses;

    @BeforeEach
    void setUp() {
        reportService = new DefaultReportService();
        userId = UUID.randomUUID();

        foodCat = new Category(UUID.randomUUID(), "Food");
        travelCat = new Category(UUID.randomUUID(), "Travel");
        categories = List.of(foodCat, travelCat);

        expenses = List.of(
                new Expense(
                        UUID.randomUUID(), userId, foodCat.id(),
                        "Bakery", new BigDecimal("10.00"),
                        LocalDate.of(2026, 3, 5), PaymentMethod.CASH,
                        "Breakfast", null, ReceiptStatus.NO_RECEIPT,
                        Instant.now(), Instant.now()
                ),
                new Expense(
                        UUID.randomUUID(), userId, foodCat.id(),
                        "Supermarket", new BigDecimal("30.00"),
                        LocalDate.of(2026, 3, 10), PaymentMethod.CREDIT_CARD,
                        "Groceries", null, ReceiptStatus.AVAILABLE,
                        Instant.now(), Instant.now()
                ),
                new Expense(
                        UUID.randomUUID(), userId, travelCat.id(),
                        "Train Station", new BigDecimal("60.00"),
                        LocalDate.of(2026, 3, 15), PaymentMethod.CREDIT_CARD,
                        "Train ticket", null, ReceiptStatus.NO_RECEIPT,
                        Instant.now(), Instant.now()
                ),
                new Expense(
                        UUID.randomUUID(), userId, travelCat.id(),
                        "Airport Taxi", new BigDecimal("50.00"),
                        LocalDate.of(2026, 2, 20), PaymentMethod.UPI,
                        "Airport ride", null, ReceiptStatus.NO_RECEIPT,
                        Instant.now(), Instant.now()
                )
        );
    }

    @Test
    void testDetailedReportCalculationForDateRange() {
        LocalDate from = LocalDate.of(2026, 3, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);

        ReportService.DetailedReport report = reportService.generateDetailedReport(expenses, categories, from, to);

        assertEquals(3, report.count());
        assertEquals(new BigDecimal("100.00"), report.totalAmount());
        assertEquals(new BigDecimal("33.33"), report.averageAmount());
        assertNotNull(report.highestExpense());
        assertEquals(new BigDecimal("60.00"), report.highestExpense().amount());

        // Category breakdown
        assertEquals(2, report.categoryBreakdowns().size());
        ReportService.CategoryBreakdown topCat = report.categoryBreakdowns().get(0);
        assertEquals(travelCat.id(), topCat.categoryId());
        assertEquals(new BigDecimal("60.00"), topCat.totalAmount());
        assertEquals(60.0, topCat.percentage(), 0.1);

        ReportService.CategoryBreakdown secondCat = report.categoryBreakdowns().get(1);
        assertEquals(foodCat.id(), secondCat.categoryId());
        assertEquals(new BigDecimal("40.00"), secondCat.totalAmount());
        assertEquals(40.0, secondCat.percentage(), 0.1);

        // Payment breakdown
        assertEquals(2, report.paymentBreakdowns().size());
        ReportService.PaymentBreakdown cardBreakdown = report.paymentBreakdowns().stream()
                .filter(p -> p.paymentMethod() == PaymentMethod.CREDIT_CARD)
                .findFirst().orElseThrow();
        assertEquals(2, cardBreakdown.count());
        assertEquals(new BigDecimal("90.00"), cardBreakdown.totalAmount());
        assertEquals(90.0, cardBreakdown.percentage(), 0.1);
    }

    @Test
    void testDetailedReportInvalidDateRangeThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                reportService.generateDetailedReport(
                        expenses,
                        categories,
                        LocalDate.of(2026, 3, 15),
                        LocalDate.of(2026, 3, 1)
                )
        );
    }

    @Test
    void testExportToCsv(@TempDir Path tempDir) throws IOException {
        LocalDate from = LocalDate.of(2026, 3, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);
        ReportService.DetailedReport report = reportService.generateDetailedReport(expenses, categories, from, to);

        Path csvFile = tempDir.resolve("test-report.csv");
        reportService.exportToCsv(report, categories, csvFile);

        assertTrue(Files.exists(csvFile));
        List<String> lines = Files.readAllLines(csvFile);
        assertTrue(lines.size() > 5);
        assertTrue(lines.stream().anyMatch(l -> l.contains("Date,Merchant,Category,Amount,Payment Method,Receipt Status,Notes")));
        assertTrue(lines.stream().anyMatch(l -> l.contains("Supermarket")));
        assertTrue(lines.stream().anyMatch(l -> l.contains("Train Station")));
    }

    @Test
    void testExportToPdf(@TempDir Path tempDir) throws IOException {
        LocalDate from = LocalDate.of(2026, 3, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);
        ReportService.DetailedReport report = reportService.generateDetailedReport(expenses, categories, from, to);

        Path pdfFile = tempDir.resolve("test-report.pdf");
        reportService.exportToPdf(report, categories, pdfFile, "tester@example.com");

        assertTrue(Files.exists(pdfFile));
        assertTrue(Files.size(pdfFile) > 100);
    }
}
