package banking.model;

import banking.util.Validator;

public class CompanyCustomer extends Customer {
    private String companyName;
    private final String registrationName;   // company registration number

    public CompanyCustomer(String customerId, User user, String companyName, String registrationName,
                           String postalAddress, String physicalAddress, double socialIncome) {
        super(customerId, user, postalAddress, physicalAddress, socialIncome);
        this.companyName = companyName;
        this.registrationName = registrationName;
    }

    @Override
    protected void validateSpecificDetails() {
        Validator.requireNotBlank(companyName, "Company name");
        Validator.requireNotBlank(registrationName, "Registration number");
    }

    public void updateCompanyInfo(String companyName) {
        this.companyName = Validator.requireNotBlank(companyName, "Company name");
    }

    @Override
    public String getCustomerType() {
        return "Company";
    }

    @Override
    public String getDisplayName() {
        return companyName;
    }

    public String getCompanyName() { return companyName; }
    public String getRegistrationName() { return registrationName; }
}
