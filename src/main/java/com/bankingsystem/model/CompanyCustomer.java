package com.bankingsystem.model;

import com.bankingsystem.model.exception.BankingException;

import java.time.LocalDate;

/**
 * A company / business customer. The date of birth belongs to the company's
 * authorised representative, who must also be 18 or older.
 */
public class CompanyCustomer extends Customer {
    private String companyName;
    private String registrationNumber;

    public CompanyCustomer(String customerId, String username, String password, LocalDate representativeDateOfBirth,
                           String companyName, String registrationNumber,
                           String postalAddress, String physicalAddress, double socialIncome) {
        super(customerId, username, password, representativeDateOfBirth, postalAddress, physicalAddress, socialIncome);
        this.companyName = companyName;
        this.registrationNumber = registrationNumber;
    }

    @Override
    protected void validateSpecificDetails() throws BankingException {
        if (isBlank(companyName) || isBlank(registrationNumber)) {
            throw new BankingException("Company name and registration number are required");
        }
    }

    public void updateCompanyInfo(String companyName, String registrationNumber) {
        this.companyName = companyName;
        this.registrationNumber = registrationNumber;
    }

    @Override
    public String getCustomerType() {
        return "COMPANY";
    }

    @Override
    public String getDisplayName() {
        return companyName;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }
}
