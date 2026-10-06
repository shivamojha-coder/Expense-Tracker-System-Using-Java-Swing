package com.expensetracker.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A monthly spending limit.
 *
 * @param categoryId the category the limit applies to, or null for the overall monthly budget
 */
public record Budget(UUID id, UUID categoryId, BigDecimal monthlyLimit) {
}
