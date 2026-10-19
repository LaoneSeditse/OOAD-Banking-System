package com.bankingsystem.controller;

/**
 * Holds who is currently logged in to the GUI (a customer or an administrator).
 */
public class UserSession {
    public enum Role { NONE, CUSTOMER, ADMIN }

    private Role role = Role.NONE;
    private String userId;
    private String displayName;
    private String customerType;
    private String token;

    void startCustomer(String customerId, String displayName, String customerType, String token) {
        this.role = Role.CUSTOMER;
        this.userId = customerId;
        this.displayName = displayName;
        this.customerType = customerType;
        this.token = token;
    }

    void startAdmin(String adminId, String displayName) {
        this.role = Role.ADMIN;
        this.userId = adminId;
        this.displayName = displayName;
        this.customerType = null;
        this.token = null;
    }

    void clear() {
        role = Role.NONE;
        userId = null;
        displayName = null;
        customerType = null;
        token = null;
    }

    public boolean isLoggedIn() {
        return role != Role.NONE;
    }

    public boolean isCustomer() {
        return role == Role.CUSTOMER;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public Role getRole() {
        return role;
    }

    public String getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCustomerType() {
        return customerType;
    }

    public String getToken() {
        return token;
    }
}
