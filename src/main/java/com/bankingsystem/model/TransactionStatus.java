package com.bankingsystem.model;

/**
 * Transaction lifecycle states (see Transaction State Machine diagram).
 */
public enum TransactionStatus {
    INITIATED,
    VALIDATED,
    COMPLETED,
    FAILED,
    CANCELLED
}
