package banking.util;

import banking.exception.ValidationException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public final class Validator {
    private Validator() { }

    public static String requireNotBlank(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(field + " is required.");
        }
        return value.trim();
    }

    public static double requirePositive(double amount, String field) {
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0) {
            throw new ValidationException(field + " must be greater than zero.");
        }
        return amount;
    }

    public static double parseAmount(String text, String field) {
        double value = parseNumber(text, field);
        return requirePositive(value, field);
    }

    public static double parseNonNegative(String text, String field) {
        double value = parseNumber(text, field);
        if (value < 0) {
            throw new ValidationException(field + " cannot be negative.");
        }
        return value;
    }

    public static LocalDate parseDate(String text, String field) {
        try {
            return LocalDate.parse(requireNotBlank(text, field));
        } catch (DateTimeParseException e) {
            throw new ValidationException(field + " must be in the format yyyy-MM-dd.");
        }
    }

    private static double parseNumber(String text, String field) {
        String cleaned = requireNotBlank(text, field).replace(",", "");
        try {
            double value = Double.parseDouble(cleaned);
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException e) {
            throw new ValidationException(field + " must be a valid number.");
        }
    }
}
