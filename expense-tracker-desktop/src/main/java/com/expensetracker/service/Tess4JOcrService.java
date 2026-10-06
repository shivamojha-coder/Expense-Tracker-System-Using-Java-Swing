package com.expensetracker.service;

import com.expensetracker.model.OcrResult;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OCR is deliberately limited to editable suggestions. Category and payment
 * method are never inferred from receipt text.
 */
public final class Tess4JOcrService implements OcrService {
    private static final String CURRENCY = "(?:(?:[$€£₹]|Rs\\.?|INR)\\s*)?";
    private static final Pattern LABELED_AMOUNT = Pattern.compile(
            "(?im)\\b(?:grand\\s+total|total\\s+due|amount\\s+due|balance\\s+due|total)"
                    + "\\s*[:#-]?\\s*" + CURRENCY + "([0-9][0-9,]*(?:\\.\\d{1,2})?)"
    );
    private static final Pattern AMOUNT = Pattern.compile(
            "(?<![\\d/])" + CURRENCY + "([0-9]{1,6}(?:,[0-9]{3})*(?:\\.\\d{1,2})?)(?![\\d/])"
    );
    private static final Pattern ISO_DATE = Pattern.compile(
            "\\b(20\\d{2})[-/.](\\d{1,2})[-/.](\\d{1,2})\\b"
    );
    private static final Pattern SLASH_DATE = Pattern.compile(
            "\\b(\\d{1,2})[/-](\\d{1,2})[/-](20\\d{2})\\b"
    );
    private static final String MONTH_NAME =
            "(Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|"
                    + "Jul(?:y)?|Aug(?:ust)?|Sep(?:t(?:ember)?)?|Oct(?:ober)?|Nov(?:ember)?|"
                    + "Dec(?:ember)?)";
    private static final Pattern MONTH_DATE = Pattern.compile(
            "(?i)\\b" + MONTH_NAME + "\\.?\\s+(\\d{1,2})(?:st|nd|rd|th)?[,]?\\s+(20\\d{2})\\b"
    );
    private static final Pattern DAY_MONTH_DATE = Pattern.compile(
            "(?i)\\b(\\d{1,2})(?:st|nd|rd|th)?\\s+" + MONTH_NAME + "\\.?,?\\s+(20\\d{2})\\b"
    );

    private static final Map<String, String> LANGUAGE_NAMES = Map.ofEntries(
            Map.entry("eng", "English"), Map.entry("hin", "Hindi"), Map.entry("ben", "Bengali"),
            Map.entry("tam", "Tamil"), Map.entry("tel", "Telugu"), Map.entry("mar", "Marathi"),
            Map.entry("guj", "Gujarati"), Map.entry("kan", "Kannada"), Map.entry("mal", "Malayalam"),
            Map.entry("pan", "Punjabi"), Map.entry("urd", "Urdu"), Map.entry("deu", "German"),
            Map.entry("fra", "French"), Map.entry("spa", "Spanish"), Map.entry("ita", "Italian"),
            Map.entry("por", "Portuguese"), Map.entry("nld", "Dutch"), Map.entry("ara", "Arabic"),
            Map.entry("chi_sim", "Chinese (Simplified)"), Map.entry("jpn", "Japanese")
    );

    private final String tessDataPath;
    private final long maxReceiptSizeBytes;
    private volatile String language = "eng";

    public Tess4JOcrService(long maxReceiptSizeBytes) {
        this(System.getenv().getOrDefault("TESSDATA_PREFIX", "").trim(), maxReceiptSizeBytes);
    }

    public Tess4JOcrService(String tessDataPath, long maxReceiptSizeBytes) {
        this.tessDataPath = tessDataPath == null ? "" : tessDataPath.trim();
        this.maxReceiptSizeBytes = maxReceiptSizeBytes;
    }

    @Override
    public void useLanguage(String languageCode) {
        if (languageCode != null && !languageCode.isBlank()) {
            this.language = languageCode.trim();
        }
    }

    /**
     * Languages whose {@code .traineddata} file is present in the tessdata folder
     * (code to display name). English is always offered because it is the default.
     */
    public static Map<String, String> installedLanguages() {
        Map<String, String> found = new TreeMap<>();
        found.put("eng", "English");
        String prefix = System.getenv().getOrDefault("TESSDATA_PREFIX", "").trim();
        List<Path> folders = new ArrayList<>();
        if (!prefix.isBlank()) {
            folders.add(Path.of(prefix));
            folders.add(Path.of(prefix, "tessdata"));
        }
        folders.add(Path.of("tessdata"));
        for (Path folder : folders) {
            if (!Files.isDirectory(folder)) {
                continue;
            }
            try (DirectoryStream<Path> files = Files.newDirectoryStream(folder, "*.traineddata")) {
                for (Path file : files) {
                    String name = file.getFileName().toString();
                    String code = name.substring(0, name.length() - ".traineddata".length());
                    if (!code.equals("osd")) {
                        found.put(code, LANGUAGE_NAMES.getOrDefault(code, code));
                    }
                }
            } catch (IOException ignored) {
                // An unreadable folder simply contributes no extra languages.
            }
        }
        return found;
    }

    @Override
    public OcrResult processReceipt(Path receipt) {
        ReceiptFileValidator.validate(receipt, maxReceiptSizeBytes);
        try {
            Tesseract tesseract = new Tesseract();
            if (!tessDataPath.isBlank()) {
                tesseract.setDatapath(tessDataPath);
            }
            tesseract.setLanguage(language);
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
                    || lower.matches(".*\\b(?:receipt|invoice|subtotal|tax|total|date|cash|visa|mastercard|items?|order|qty|price|thank\\s+you)\\b.*")
                    || candidate.matches("^\\d{2,}\\s+.*")
                    || candidate.matches(".*\\d{5,}.*")) {
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
            // A first part above 12 can only be the day. Ambiguous dates (both parts 12 or less)
            // are read month-first; the expense form lets the user correct them.
            boolean dayFirst = Integer.parseInt(slash.group(1)) > 12;
            String month = dayFirst ? slash.group(2) : slash.group(1);
            String day = dayFirst ? slash.group(1) : slash.group(2);
            return safeDate(month + "/" + day + "/" + slash.group(3), strict("M/d/uuuu"));
        }
        Matcher monthFirst = MONTH_DATE.matcher(text);
        if (monthFirst.find()) {
            return namedMonthDate(monthFirst.group(2), monthFirst.group(1), monthFirst.group(3));
        }
        Matcher dayFirst = DAY_MONTH_DATE.matcher(text);
        if (dayFirst.find()) {
            return namedMonthDate(dayFirst.group(1), dayFirst.group(2), dayFirst.group(3));
        }
        return null;
    }

    private static LocalDate namedMonthDate(String day, String monthName, String year) {
        String abbreviation = monthName.substring(0, 3);
        return safeDate(day + " " + abbreviation + " " + year, strict("d MMM uuuu"));
    }

    /** Strict parsing so an impossible date such as 31 Feb is rejected rather than moved to the 28th. */
    private static DateTimeFormatter strict(String pattern) {
        return DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH).withResolverStyle(ResolverStyle.STRICT);
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