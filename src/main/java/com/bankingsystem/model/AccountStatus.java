package com.bankingsystem.model;

/**
 * Account lifecycle states (see Account State Machine diagram).
 * PENDING = "Created" state before the opening deposit has been verified.
 */
public enum AccountStatus {
    PENDING,
    ACTIVE,
    SUSPENDED,
    CLOSED
}
