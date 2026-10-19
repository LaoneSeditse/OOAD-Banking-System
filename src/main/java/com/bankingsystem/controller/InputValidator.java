package com.bankingsystem.controller;

import java.util.OptionalDouble;

/**
 * Input validation helpers used by the controllers before calling the domain model.
 */
final class InputValidator {
    private InputValidator() {
    }

    static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** @return the parsed amount if it is a positive number with at most 2 decimals */
    static OptionalDouble parseAmount(String text) {
        if (isBlank(text)) {
            return OptionalDouble.empty();
        }
        try {
            double value = Double.parseDouble(text.trim().replace(",", ""));
            if (value <= 0 || Double.isNaN(value) || Double.isInfinite(value)) {
                return OptionalDouble.empty();
            }
            if (Math.round(value * 100) != value * 100) {
                return OptionalDouble.empty();
            }
            return OptionalDouble.of(value);
        } catch (NumberFormatException e) {
            return OptionalDouble.empty();
        }
    }

    /** Like {@link #parseAmount(String)} but also accepts zero. */
    static OptionalDouble parseNonNegative(String text) {
        if (!isBlank(text) && text.trim().matches("0+(\\.0+)?")) {
            return OptionalDouble.of(0);
        }
        return parseAmount(text);
    }
}
