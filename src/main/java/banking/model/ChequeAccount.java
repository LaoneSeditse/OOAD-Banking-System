package banking.model;

import banking.exception.ValidationException;

public class ChequeAccount extends Account {
    private final InterestStrategy interestStrategy = new NoInterestStrategy();
    private final EmploymentDetails employmentDetails;
    private boolean employmentVerified;

    private ChequeAccount(String accountNumber, Customer customer, String branch, double balance,
                          AccountStatus status, EmploymentDetails employmentDetails) {
        super(accountNumber, customer, branch, balance, status);
        this.employmentDetails = employmentDetails;
        this.employmentVerified = employmentDetails.verify();
    }

    public static ChequeAccount open(String accountNumber, Customer customer, String branch,
                                     double initialDeposit, EmploymentDetails employment) {
        requireEmployment(employment);
        if (initialDeposit < 0) {
            throw new ValidationException("Initial deposit cannot be negative.");
        }
        ChequeAccount account = new ChequeAccount(accountNumber, customer, branch, 0.0,
                AccountStatus.ACTIVE, employment);
        if (initialDeposit > 0) {
            account.deposit(initialDeposit, "Initial deposit");
        }
        return account;
    }

    public static ChequeAccount restore(String accountNumber, Customer customer, String branch, double balance,
                                        AccountStatus status, EmploymentDetails employment) {
        return new ChequeAccount(accountNumber, customer, branch, balance, status, employment);
    }

    private static void requireEmployment(EmploymentDetails employment) {
        if (employment == null || !employment.verify()) {
            throw new ValidationException(
                    "A Cheque account can only be opened for someone who is working. "
                            + "Please provide the company name and company address.");
        }
    }

    public boolean verifyEmployment() {
        employmentVerified = employmentDetails.verify();
        return employmentVerified;
    }

    @Override
    public void deposit(double amount) {
        deposit(amount, "Cheque account deposit");
    }

    @Override
    public void withdraw(double amount) {
        withdrawFunds(amount);
    }

    @Override
    protected void checkWithdrawalRules(double amount) {
        enforceWithdrawalLimit(amount);
    }

    @Override
    public double calculateInterest() {
        return interestStrategy.calculateInterest(getBalance());
    }

    @Override
    public double getInterestRate() {
        return interestStrategy.getRate();
    }

    public EmploymentDetails getEmploymentDetails() { return employmentDetails; }
    public boolean isEmploymentVerified() { return employmentVerified; }

    @Override
    public AccountType getAccountType() {
        return AccountType.CHEQUE;
    }
}
