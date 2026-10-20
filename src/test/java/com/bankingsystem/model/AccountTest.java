package com.bankingsystem.model;

import com.bankingsystem.model.exception.BankingException;
import com.bankingsystem.model.exception.InsufficientFundsException;
import com.bankingsystem.model.exception.WithdrawalNotAllowedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {
    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = new IndividualCustomer("C1", "tester", "hash", LocalDate.of(1990, 1, 1),
                "Test", "User", "123456789", "P.O. Box 1", "Plot 1", 10000);
    }

    @Test
    void savingsAccountAcceptsDepositsButRejectsWithdrawals() throws BankingException {
        Account savings = new SavingsAccount(customer, "S1", "Gaborone Main");
        savings.open(100);
        savings.deposit(50);
        assertEquals(150, savings.getBalance(), 0.001);
        assertThrows(WithdrawalNotAllowedException.class, () -> savings.withdraw(10));
        assertFalse(savings.allowsWithdrawals());
    }

    @Test
    void investmentAccountRequiresMinimumOpeningDepositOf500() throws BankingException {
        Account tooSmall = new InvestmentAccount(customer, "I1", "Gaborone Main");
        assertThrows(BankingException.class, () -> tooSmall.open(499.99));
        Account ok = new InvestmentAccount(customer, "I2", "Gaborone Main");
        ok.open(500);
        assertEquals(AccountStatus.ACTIVE, ok.getStatus());
    }

    @Test
    void interestRatesMatchTheSpecification() throws BankingException {
        Account savings = new SavingsAccount(customer, "S1", "Maun");
        savings.open(1000);
        Account investment = new InvestmentAccount(customer, "I1", "Maun");
        investment.open(1000);
        Account cheque = new ChequeAccount(customer, "C1", "Maun",
                new EmploymentDetails("Debswana", "Jwaneng", "Engineer"));
        cheque.open(1000);

        // Polymorphism: the same call gives a different result per subclass.
        List<Account> accounts = List.of(savings, investment, cheque);
        double[] expected = {0.50, 50.00, 0.0};
        for (int i = 0; i < accounts.size(); i++) {
            assertEquals(expected[i], accounts.get(i).calculateInterest(), 0.001);
        }

        investment.applyMonthlyInterest();
        assertEquals(1050, investment.getBalance(), 0.001);
    }

    @Test
    void chequeAccountRequiresEmploymentDetails() {
        Account cheque = new ChequeAccount(customer, "C1", "Maun", new EmploymentDetails("", "", ""));
        assertThrows(BankingException.class, () -> cheque.open(100));
    }

    @Test
    void withdrawalsAreLimitedByBalanceAndPerTransactionLimit() throws BankingException {
        Account investment = new InvestmentAccount(customer, "I1", "Maun");
        investment.open(3000);
        assertThrows(InsufficientFundsException.class, () -> investment.withdraw(4000));
        assertThrows(BankingException.class, () -> investment.withdraw(Account.MAX_WITHDRAWAL_PER_TRANSACTION + 1));
        investment.withdraw(1000, "ATM");
        assertEquals(2000, investment.getBalance(), 0.001);
    }

    @Test
    void closedAccountRejectsTransactions() throws BankingException {
        Account investment = new InvestmentAccount(customer, "I1", "Maun");
        investment.open(1000);
        investment.close();
        assertEquals(AccountStatus.CLOSED, investment.getStatus());
        assertThrows(BankingException.class, () -> investment.deposit(10));
    }

    @Test
    void invalidAmountsAreRejected() throws BankingException {
        Account savings = new SavingsAccount(customer, "S1", "Maun");
        savings.open(10);
        assertThrows(BankingException.class, () -> savings.deposit(0));
        assertThrows(BankingException.class, () -> savings.deposit(-5));
    }

    @Test
    void customersMustBeAdults() {
        Customer minor = new IndividualCustomer(null, "minor", "hash", LocalDate.now().minusYears(17),
                "Young", "Person", "999", "P.O. Box 2", "Plot 2", 0);
        assertFalse(minor.isAdult());
        assertThrows(BankingException.class, minor::register);
    }

    @Test
    void userIsLockedAfterThreeFailedAttempts() {
        assertFalse(customer.registerFailedAttempt());
        assertFalse(customer.registerFailedAttempt());
        assertTrue(customer.registerFailedAttempt());
        assertEquals(UserStatus.LOCKED, customer.getStatus());
    }

    @Test
    void transactionFollowsStateMachine() {
        Transaction tx = new Transaction("S1", 100, TransactionType.DEPOSIT, "test");
        assertEquals(TransactionStatus.INITIATED, tx.getStatus());
        tx.validate();
        assertEquals(TransactionStatus.VALIDATED, tx.getStatus());
        tx.complete(100);
        assertEquals(TransactionStatus.COMPLETED, tx.getStatus());
    }
}
