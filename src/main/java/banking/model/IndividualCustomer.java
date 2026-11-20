package banking.model;

import banking.exception.ValidationException;
import banking.util.Validator;

import java.time.LocalDate;

public class IndividualCustomer extends Customer {
    private final String firstName;
    private final String lastName;

    public IndividualCustomer(String customerId, User user, String firstName, String lastName,
                              String postalAddress, String physicalAddress, double socialIncome) {
        super(customerId, user, postalAddress, physicalAddress, socialIncome);
        this.firstName = firstName;
        this.lastName = lastName;
    }

    @Override
    protected void validateSpecificDetails() {
        Validator.requireNotBlank(firstName, "First name");
        Validator.requireNotBlank(lastName, "Surname");
        if (getUser().getDateOfBirth() == null) {
            throw new ValidationException("Date of birth is required.");
        }
        if (getAge() < 18) {
            throw new ValidationException("An individual customer must be at least 18 years old.");
        }
    }

    @Override
    public String getCustomerType() {
        return "Individual";
    }

    @Override
    public String getDisplayName() {
        return firstName + " " + lastName;
    }

    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public LocalDate getDateOfBirth() { return getUser().getDateOfBirth(); }
    public int getAge() { return getUser().getAge(); }
}
