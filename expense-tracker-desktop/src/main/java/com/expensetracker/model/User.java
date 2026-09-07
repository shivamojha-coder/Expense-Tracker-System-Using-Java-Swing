package com.expensetracker.model;

import java.util.UUID;

public record User(UUID id, String email) {
}