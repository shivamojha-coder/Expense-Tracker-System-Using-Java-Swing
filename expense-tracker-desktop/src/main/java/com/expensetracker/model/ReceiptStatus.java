package com.expensetracker.model;

public enum ReceiptStatus {
    AVAILABLE("Available"),
    NO_RECEIPT("No Receipt"),
    UPLOAD_FAILED("Upload Failed");

    private final String displayName;

    ReceiptStatus(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}