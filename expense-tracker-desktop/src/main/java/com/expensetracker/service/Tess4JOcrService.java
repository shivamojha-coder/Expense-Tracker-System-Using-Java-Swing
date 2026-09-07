package com.expensetracker.service;

import com.expensetracker.model.OcrResult;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OCR is deliberately limited to editable suggestions. Category and payment
 * method are never inferred from receipt text.
 */
public final class Tess4JOcrService implements OcrService {
    private static final Pattern LABELED_AMOUNT = Pattern.compile(
            "(?im)\\b(?:grand\\s+total|total\\s+due|amount\\s+due|balance\\s+due|total)"
                    + "\\s*[:#-]?\\s*(?:[$€£]\\s*)?([0-9][0-9,]*(?:\\.\\d{1,2})?)"
    );
    private static final Pattern AMOUNT = Pattern.compile(
            "(?<![\\d/])(?:[$€£]\\s*)?([0-9]{1,6}(?:,[0-9]{3})*(?:\\.\\d{1,2})?)(?![\\d/])"
    );
    private static final Pattern ISO_DATE = Pattern.compile(
            "\\b(20\\d{2})[-/.](\\d{1,2})[-/.](\\d{1,2})\\b"
    );
    private static final Pattern SLASH_DATE = Pattern.compile(
            "\\b(\\d{1,2})[/-](\\d{1,2})[/-](20\\d{2})\\b"
    );
    private static final Pattern MONTH_DATE = Pattern.compile(
            "(?i)\\b(Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|"
                    + "Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|"
                    + "Dec(?:ember)?)\\s+(\\d{1,2})(?:st|nd|rd|th)?[,]?\\s+(20\\d{2})\\b"
    );

    private final String tessDataPath;
    private final long maxReceiptSizeBytes;

    public Tess4JOcrService() {
        this(System.getenv().getOrDefault("TESSDATA_PREFIX", "").trim(), 10 * 1024 * 1024);
    }

    public Tess4JOcrService(String tessDataPath) {
        this(tessDataPath, 10 * 1024 * 1024);
    }

    public Tess4JOcrService(long maxReceiptSizeBytes) {
        this(System.getenv().getOrDefault("TESSDATA_PREFIX", "").trim(), maxReceiptSizeBytes);
    }

    public Tess4JOcrService(String tessDataPath, long maxReceiptSizeBytes) {
        this.tessDataPath = tessDataPath == null ? "" : tessDataPath.trim();
        this.maxReceiptSizeBytes = maxReceiptSizeBytes;
    }

    @Override
    public OcrResult processReceipt(Path receipt) {
        ReceiptFileValidator.validate(receipt, maxReceiptSizeBytes);
        try {
            Tesseract tesseract = new Tesseract();
            if (!tessDataPath.isBlank()) {
                tesseract.setDatapath(tessDataPath);
            }
            String rawText = tesseract.doOCR(receipt.toFile());
            return parseText(rawText);
        } catch (TesseractException exception) {
            throw new ServiceException(
                    "OCR could not read this receipt. You can still enter its details manually.",
                    exception
            );
        } catch (RuntimeException exception) {
            throw new ServiceException(
                    "OCR is unavailable for this receipt. You can still enter its details manually.",
                    exception
            );
        }
    }

    static OcrResult parseText(String rawText) {
        String text = rawText == null ? "" : rawText;
        return new OcrResult(
                findMerchant(text),
                findAmount(text),
                findDate(text),
                text
        );
    }

    private static String findMerchant(String text) {
        for (String line : text.split("\\R")) {
            String candidate = line.replaceAll("\\s+", " ").trim();
            String lower = candidate.toLowerCase(Locale.ROOT);
            if (candidate.length() < 2 || candidate.length() > 80
                    || candidate.replaceAll("[^A-Za-z]", "").length() < 2
                    || lower.matches(".*\\b(?:receipt|invoice|subtotal|tax|total|date|cash|visa|mastercard)\\b.*")
                    || candidate.matches(".*\\d{3,}.*")) {
                continue;
            }
            return candidate;
        }
        return null;
    }

    private static BigDecimal findAmount(String text) {
        Matcher labeled = LABELED_AMOUNT.matcher(text);
        BigDecimal lastLabeled = null;
        while (labeled.find()) {
            lastLabeled = decimal(labeled.group(1));
        }
        if (lastLabeled != null && lastLabeled.signum() > 0) {
            return lastLabeled;
        }

        Matcher amounts = AMOUNT.matcher(text);
        BigDecimal largest = null;
        while (amounts.find()) {
            BigDecimal value = decimal(amounts.group(1));
            if (value != null && value.signum() > 0
                    && (largest == null || value.compareTo(largest) > 0)) {
                largest = value;
            }
        }
        return largest;
    }

    private static LocalDate findDate(String text) {
        Matcher iso = ISO_DATE.matcher(text);
        if (iso.find()) {
            return safeDate(iso.group(1) + "-" + iso.group(2) + "-" + iso.group(3),
                    DateTimeFormatter.ISO_LOCAL_DATE);
        }
        Matcher slash = SLASH_DATE.matcher(text);
        if (slash.find()) {
            return safeDate(
                    slash.group(1) + "/" + slash.group(2) + "/" + slash.group(3),
                    DateTimeFormatter.ofPattern("M/d/yyyy")
            );
        }
        Matcher month = MONTH_DATE.matcher(text);
        if (month.find()) {
            String value = month.group(1) + " " + month.group(2) + " " + month.group(3);
            for (String pattern : List.of("MMM d yyyy", "MMMM d yyyy")) {
                try {
                    return LocalDate.parse(value, DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH));
                } catch (DateTimeParseException ignored) {
                    // Try the full or abbreviated month spelling.
                }
            }
        }
        return null;
    }

    private static BigDecimal decimal(String value) {
        try {
            return new BigDecimal(value.replace(",", ""));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static LocalDate safeDate(String value, DateTimeFormatter formatter) {
        try {
            return LocalDate.parse(value, formatter);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }
}