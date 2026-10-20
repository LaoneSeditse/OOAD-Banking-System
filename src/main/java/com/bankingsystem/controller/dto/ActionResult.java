package com.bankingsystem.controller.dto;

/**
 * Result of a user request handled by a controller: a success flag, a user-facing message
 * and optional data for the view to display.
 */
public record ActionResult<T>(boolean success, String message, T data) {

    public static <T> ActionResult<T> ok(String message, T data) {
        return new ActionResult<>(true, message, data);
    }

    public static <T> ActionResult<T> ok(String message) {
        return new ActionResult<>(true, message, null);
    }

    public static <T> ActionResult<T> fail(String message) {
        return new ActionResult<>(false, message, null);
    }
}
