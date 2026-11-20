package banking.model;

import banking.exception.ValidationException;
import banking.util.Money;

public class InvestmentAccount extends Account {
    public static final double MINIMUM_INITIAL_DEPOSIT = 500.00;

    private final InterestStrategy interestStrategy = new InvestmentInterestStrategy();
    private final double minimumInitialDeposit = MINIMUM_INITIAL_DEPOSIT;

    private InvestmentAccount(String accountNumber, Customer customer, String branch,
                              double balance, AccountStatus status) {
        super(accountNumber, customer, branch, balance, status);
    }

    public static InvestmentAccount open(String accountNumber, Customer customer, String branch, double initialDeposit) {
        if (initialDeposit < MINIMUM_INITIAL_DEPOSIT) {
            throw new ValidationException("An Investment account needs an initial deposit of at least "
                    + Money.format(MINIMUM_INITIAL_DEPOSIT) + ".");
        }
        InvestmentAccount account = new InvestmentAccount(accountNumber, customer, branch, 0.0, AccountStatus.ACTIVE);
        account.deposit(initialDeposit, "Initial deposit");
        return account;
    }

    public static InvestmentAccount restore(String accountNumber, Customer customer, String branch,
                                            double balance, AccountStatus status) {
        return new InvestmentAccount(accountNumber, customer, branch, balance, status);
    }

    @Override
    public void deposit(double amount) {
        deposit(amount, "Investment deposit");
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

    public double getMinimumInitialDeposit() {
        return minimumInitialDeposit;
    }

    @Override
    public AccountType getAccountType() {
        return AccountType.INVESTMENT;
    }
}
