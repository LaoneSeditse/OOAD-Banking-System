package com.bankingsystem.dao;

/**
 * Unchecked wrapper for JDBC {@link java.sql.SQLException}s raised by the data access layer.
 */
public class DataAccessException extends RuntimeException {
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
