package com.bankingsystem.controller;

import java.math.BigDecimal;
import java.util.regex.Pattern;

import java.util.OptionalDouble;

/**
 * Input validation helpers used by the controllers before calling the domain model.
 */
final class InputValidator {
    /** Largest single amount accepted (keeps balances well inside the DECIMAL(15,2) columns). */
    static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000000.00");
    /** Plain decimal with at most 2 decimal places: no signs, exponents or other characters. */
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("\\d{1,10}(\\.\\d{1,2})?");

    private InputValidator() {
    }

    static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** @return the parsed amount if it is a positive number with at most 2 decimals and at most BWP 1 billion */
    static OptionalDouble parseAmount(String text) {
        if (isBlank(text)) {
            return OptionalDouble.empty();
        }
        String cleaned = text.trim().replace(",", "");
        if (!AMOUNT_PATTERN.matcher(cleaned).matches()) {
            return OptionalDouble.empty();
        }
        BigDecimal value = new BigDecimal(cleaned);
        if (value.signum() <= 0 || value.compareTo(MAX_AMOUNT) > 0) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(value.doubleValue());
    }

    /** Like {@link #parseAmount(String)} but also accepts zero. */
    static OptionalDouble parseNonNegative(String text) {
        if (!isBlank(text) && text.trim().matches("0+(\\.0+)?")) {
            return OptionalDouble.of(0);
        }
        return parseAmount(text);
    }
}
