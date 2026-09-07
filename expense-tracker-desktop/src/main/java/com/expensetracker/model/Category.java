package com.expensetracker.model;

import java.util.UUID;

public record Category(UUID id, String name) {
    @Override
    public String toString() {
        return name;
    }
}