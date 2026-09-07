package com.expensetracker.service;

import com.expensetracker.model.OcrResult;

import java.nio.file.Path;

public interface OcrService {
    OcrResult processReceipt(Path receipt);
}