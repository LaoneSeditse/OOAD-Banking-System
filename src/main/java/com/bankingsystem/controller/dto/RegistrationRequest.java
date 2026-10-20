package com.bankingsystem.controller.dto;

import java.time.LocalDate;

/**
 * Raw registration form input collected by the RegisterView. The controller validates it.
 *
 * @param customerType "INDIVIDUAL" or "COMPANY"
 */
public record RegistrationRequest(String customerType, String firstName, String lastName, String idNumber,
                                  String companyName, String registrationNumber, LocalDate dateOfBirth,
                                  String postalAddress, String physicalAddress, String income,
                                  String username, String password, String confirmPassword) {
}
