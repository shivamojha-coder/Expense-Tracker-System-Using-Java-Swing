package com.expensetracker.service;

import java.nio.file.Path;
import java.util.UUID;

public interface StorageService {
    String uploadReceipt(UUID expenseId, Path localFile);

    byte[] downloadReceipt(String receiptPath);

    void deleteReceipt(String receiptPath);
}