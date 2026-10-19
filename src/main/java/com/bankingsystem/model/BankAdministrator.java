package com.bankingsystem.model;

import com.bankingsystem.util.PasswordUtil;

/**
 * Bank staff member who manages customers and accounts and generates reports.
 */
public class BankAdministrator {
    private final String adminId;
    private final String name;
    private final String email;
    private final String username;
    private final String passwordHash;

    public BankAdministrator(String adminId, String name, String email, String username, String passwordHash) {
        this.adminId = adminId;
        this.name = name;
        this.email = email;
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public boolean login(String username, String password) {
        return this.username.equalsIgnoreCase(username) && PasswordUtil.matches(password, passwordHash);
    }

    public String getAdminId() {
        return adminId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
