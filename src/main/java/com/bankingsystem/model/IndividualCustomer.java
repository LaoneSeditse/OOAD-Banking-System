package com.bankingsystem.model;

import com.bankingsystem.model.exception.BankingException;

import java.time.LocalDate;

public class IndividualCustomer extends Customer {
    private String firstName;
    private String lastName;
    private String idNumber;

    public IndividualCustomer(String customerId, String username, String password, LocalDate dateOfBirth,
                              String firstName, String lastName, String idNumber,
                              String postalAddress, String physicalAddress, double socialIncome) {
        super(customerId, username, password, dateOfBirth, postalAddress, physicalAddress, socialIncome);
        this.firstName = firstName;
        this.lastName = lastName;
        this.idNumber = idNumber;
    }

    @Override
    protected void validateSpecificDetails() throws BankingException {
        if (isBlank(firstName) || isBlank(lastName)) {
            throw new BankingException("First name and last name are required");
        }
        if (isBlank(idNumber)) {
            throw new BankingException("ID number is required");
        }
    }

    public void updatePersonalInfo(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    @Override
    public String getCustomerType() {
        return "INDIVIDUAL";
    }

    @Override
    public String getDisplayName() {
        return firstName + " " + lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getIdNumber() {
        return idNumber;
    }
}
