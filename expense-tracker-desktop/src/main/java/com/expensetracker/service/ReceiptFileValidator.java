package com.expensetracker.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Validates receipts before OCR or upload. Extension checks alone are not
 * sufficient because a renamed non-image file must not reach Tesseract or
 * storage.
 */
public final class ReceiptFileValidator {
    private static final long DEFAULT_MAX_SIZE_BYTES = 10 * 1024 * 1024;

    private ReceiptFileValidator() {
    }

    public static void validate(Path file, long maxSizeBytes) {
        if (file == null) {
            throw new ServiceException("Choose a receipt image first.");
        }
        if (!Files.isRegularFile(file)) {
            throw new ServiceException("The selected receipt file could not be found.");
        }
        long limit = maxSizeBytes > 0 ? maxSizeBytes : DEFAULT_MAX_SIZE_BYTES;
        try {
            long size = Files.size(file);
            if (size == 0) {
                throw new ServiceException("The receipt image is empty.");
            }
            if (size > limit) {
                throw new ServiceException("Receipt images must be no larger than "
                        + formatMegabytes(limit) + ".");
            }
            String extension = extension(file);
            if (!extension.equals("jpg") && !extension.equals("jpeg") && !extension.equals("png")) {
                throw new ServiceException("Receipt must be a JPG, JPEG, or PNG image.");
            }
            byte[] header;
            try (var input = Files.newInputStream(file)) {
                header = input.readNBytes(8);
            }
            if (!hasJpegHeader(header) && !hasPngHeader(header)) {
                throw new ServiceException("The selected file is not a valid JPG or PNG image.");
            }
            if (extension.equals("png") && !hasPngHeader(header)) {
                throw new ServiceException("The .png receipt does not contain a PNG image.");
            }
            if ((extension.equals("jpg") || extension.equals("jpeg")) && !hasJpegHeader(header)) {
                throw new ServiceException("The .jpg receipt does not contain a JPEG image.");
            }
        } catch (IOException exception) {
            throw new ServiceException("The receipt file could not be read.", exception);
        }
    }

    public static String extension(Path file) {
        String name = file == null || file.getFileName() == null
                ? ""
                : file.getFileName().toString().toLowerCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        return dot >= 0 && dot < name.length() - 1 ? name.substring(dot + 1) : "";
    }

    public static String contentType(Path file) {
        return extension(file).equals("png") ? "image/png" : "image/jpeg";
    }

    private static boolean hasJpegHeader(byte[] bytes) {
        return bytes.length >= 3
                && (bytes[0] & 0xff) == 0xff
                && (bytes[1] & 0xff) == 0xd8
                && (bytes[2] & 0xff) == 0xff;
    }

    private static boolean hasPngHeader(byte[] bytes) {
        byte[] signature = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
        if (bytes.length < signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if (bytes[index] != signature[index]) {
                return false;
            }
        }
        return true;
    }

    private static String formatMegabytes(long bytes) {
        if (bytes < 1024 * 1024) {
            return bytes < 1024
                    ? bytes + " bytes"
                    : String.format(Locale.ROOT, "%.0f KB", bytes / 1024d);
        }
        double megabytes = bytes / (1024d * 1024d);
        return megabytes == Math.rint(megabytes)
                ? String.format(Locale.ROOT, "%.0f MB", megabytes)
                : String.format(Locale.ROOT, "%.1f MB", megabytes);
    }
}