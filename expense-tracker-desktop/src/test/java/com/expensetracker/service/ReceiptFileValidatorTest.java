package com.expensetracker.service;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReceiptFileValidatorTest {
    @Test
    void acceptsJpgJpegAndPngSignatures() throws IOException {
        Path jpg = writeFile("receipt.JPG", new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x00});
        Path jpeg = writeFile("receipt.jpeg", new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x00});
        Path png = writeFile("receipt.png", new byte[]{
                (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
        });

        assertDoesNotThrow(() -> ReceiptFileValidator.validate(jpg, 1024));
        assertDoesNotThrow(() -> ReceiptFileValidator.validate(jpeg, 1024));
        assertDoesNotThrow(() -> ReceiptFileValidator.validate(png, 1024));
        assertEquals("image/jpeg", ReceiptFileValidator.contentType(jpg));
        assertEquals("image/png", ReceiptFileValidator.contentType(png));
    }

    @Test
    void rejectsFilesAboveConfiguredLimit() throws IOException {
        Path file = writeFile("receipt.jpg", new byte[]{
                (byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x00, 0x01
        });

        ServiceException exception = assertThrows(
                ServiceException.class,
                () -> ReceiptFileValidator.validate(file, 4)
        );

        assertTrue(exception.getMessage().contains("no larger than 4 bytes"));
    }

    @Test
    void rejectsInvalidImageSignaturesAndMismatchedExtensions() throws IOException {
        Path invalid = writeFile("receipt.png", "not an image".getBytes());
        Path mismatched = writeFile("receipt.jpg", new byte[]{
                (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
        });

        ServiceException invalidException = assertThrows(
                ServiceException.class,
                () -> ReceiptFileValidator.validate(invalid, 1024)
        );
        ServiceException mismatchedException = assertThrows(
                ServiceException.class,
                () -> ReceiptFileValidator.validate(mismatched, 1024)
        );

        assertEquals("The selected file is not a valid JPG or PNG image.", invalidException.getMessage());
        assertEquals("The .jpg receipt does not contain a JPEG image.", mismatchedException.getMessage());
    }

    @Test
    void rejectsUnsupportedAndEmptyFiles() throws IOException {
        Path unsupported = writeFile("receipt.gif", new byte[]{1});
        Path empty = writeFile("empty.png", new byte[0]);

        assertEquals(
                "Receipt must be a JPG, JPEG, or PNG image.",
                assertThrows(ServiceException.class, () -> ReceiptFileValidator.validate(unsupported, 1024))
                        .getMessage()
        );
        assertEquals(
                "The receipt image is empty.",
                assertThrows(ServiceException.class, () -> ReceiptFileValidator.validate(empty, 1024))
                        .getMessage()
        );
    }

    private Path writeFile(String name, byte[] bytes) throws IOException {
        Path file = Files.createTempFile("receipt-test-", "-" + name);
        Files.write(file, bytes);
        file.toFile().deleteOnExit();
        return file;
    }
}