package com.expensetracker.service;

import com.expensetracker.model.OcrResult;

import java.nio.file.Path;

public interface OcrService {
    OcrResult processReceipt(Path receipt);

    /** Selects the recognition language (a Tesseract code such as "eng"); engines without languages ignore it. */
    default void useLanguage(String languageCode) {
    }
}
