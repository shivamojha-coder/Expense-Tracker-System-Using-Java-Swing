package com.expensetracker.model;

import java.time.Instant;

public record SupabaseSession(
        String accessToken,
        String refreshToken,
        Instant expiresAt,
        User user
) {
}