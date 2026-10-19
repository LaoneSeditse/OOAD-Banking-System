package com.bankingsystem.controller.dto;

/** Read-only customer data shown in the administrator view. */
public record CustomerSummary(String customerId, String name, String customerType, String username, String status,
                              int age, int accountCount, double totalBalance) {
}
