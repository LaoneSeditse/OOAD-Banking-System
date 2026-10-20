package com.bankingsystem.model;

/**
 * Login status of a user (see Customer State Machine diagram).
 * LOCKED = "Blocked" state after 3 failed login attempts or suspicious activity.
 */
public enum UserStatus {
    ACTIVE,
    LOCKED
}
