package com.bankingsystem.model;

/**
 * Where a Cheque account holder works. Owned by (composed into) a {@link ChequeAccount}.
 */
public class EmploymentDetails {
    private final String companyName;
    private final String companyAddress;
    private final String position;

    public EmploymentDetails(String companyName, String companyAddress, String position) {
        this.companyName = companyName;
        this.companyAddress = companyAddress;
        this.position = position;
    }

    /** Employment is verified when the employer name, address and position are all supplied. */
    public boolean verify() {
        return notBlank(companyName) && notBlank(companyAddress) && notBlank(position);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getCompanyAddress() {
        return companyAddress;
    }

    public String getPosition() {
        return position;
    }

    @Override
    public String toString() {
        return position + " at " + companyName + " (" + companyAddress + ")";
    }
}
