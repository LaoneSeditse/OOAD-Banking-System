package banking;

import banking.dao.Database;
import banking.exception.BankException;
import banking.model.Account;
import banking.model.AccountStatus;
import banking.model.AccountType;
import banking.model.Customer;
import banking.model.EmploymentDetails;
import banking.model.Role;
import banking.model.TransactionType;
import banking.service.BankService;
import banking.service.BankServiceImpl;
import banking.service.BankingSystem;
import banking.service.InterestSummary;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BankingIntegrationTest {
    private static final String TEST_DB = "target/test-bank.db";
    private BankService service;
    private BankingSystem system;

    @BeforeEach
    void setUp() {
        new File("target").mkdirs();
        new File(TEST_DB).delete();
        Database.useFile(TEST_DB);
        service = new BankServiceImpl();
        system = new BankingSystem(service);
        system.startSystem();           // creates schema and the 10 sample customers
    }

    @AfterEach
    void tearDown() {
        system.stopSystem();
    }

    @Test
    void sampleDataContainsTenCustomers() {
        assertEquals(10, service.getAllCustomers().size());
    }

    @Test
    void fullUserFlow_login_open_deposit_withdraw_persistedInDatabase() {
        // 1. register and log in
        Customer c = service.registerIndividual("testuser", "Secret1", "Test", "User",
                LocalDate.of(1999, 1, 1), "P.O. Box 1", "Plot 1", 5000);
        assertEquals(Role.CUSTOMER, service.authenticate("testuser", "Secret1").getRole());

        // 2. open an Investment account (needs >= 500) and a Savings account
        Account inv = service.createAccount(c.getCustomerId(), AccountType.INVESTMENT, 1000, "Gaborone Main", null);
        Account sav = service.createAccount(c.getCustomerId(), AccountType.SAVINGS, 0, "Gaborone Main", null);

        // 3. deposit and withdraw
        service.deposit(inv.getAccountNumber(), 500);
        service.withdraw(inv.getAccountNumber(), 200);
        assertEquals(1300.00, service.getAccountBalance(inv.getAccountNumber()), 0.001);

        // 4. savings must refuse withdrawals
        service.deposit(sav.getAccountNumber(), 100);
        assertThrows(BankException.class, () -> service.withdraw(sav.getAccountNumber(), 10));

        // 5. re-read everything from the database: the data really was saved
        Customer reloaded = service.getCustomer(c.getCustomerId());
        assertEquals(2, reloaded.getAccounts().size());
        Account reloadedInv = reloaded.getAccount(inv.getAccountNumber());
        assertEquals(1300.00, reloadedInv.getBalance(), 0.001);
        assertEquals(3, reloadedInv.getTransactions().size());   // initial deposit, deposit, withdrawal
    }

    @Test
    void monthlyInterest_isPaidOncePerMonth_atTheRightRates() {
        Customer c = service.registerCompany("acme1", "Secret1", "Acme", "BW1", "Box 1", "Plot 1", 1000);
        Account inv = service.createAccount(c.getCustomerId(), AccountType.INVESTMENT, 1000, "Main", null);
        Account sav = service.createAccount(c.getCustomerId(), AccountType.SAVINGS, 2000, "Main", null);
        Account chq = service.createAccount(c.getCustomerId(), AccountType.CHEQUE, 500, "Main",
                new EmploymentDetails("Acme", "Plot 1", "Payroll"));

        InterestSummary first = service.payMonthlyInterest();
        assertTrue(first.accountsCredited() >= 2);
        assertEquals(1050.00, service.getAccountBalance(inv.getAccountNumber()), 0.001);   // 5%
        assertEquals(2001.00, service.getAccountBalance(sav.getAccountNumber()), 0.001);   // 0.05%
        assertEquals(500.00, service.getAccountBalance(chq.getAccountNumber()), 0.001);    // none

        assertEquals(0, service.payMonthlyInterest().accountsCredited());                  // already paid
        assertTrue(service.getTransactions(inv.getAccountNumber()).stream()
                .anyMatch(t -> t.getType() == TransactionType.INTEREST));
    }

    @Test
    void businessRulesAreEnforced() {
        Customer c = service.registerCompany("rules1", "Secret1", "Rules", "BW2", "Box 1", "Plot 1", 1);
        assertThrows(BankException.class,
                () -> service.createAccount(c.getCustomerId(), AccountType.INVESTMENT, 499.99, "Main", null));
        assertThrows(BankException.class,
                () -> service.createAccount(c.getCustomerId(), AccountType.CHEQUE, 0, "Main", null));
        Account chq = service.createAccount(c.getCustomerId(), AccountType.CHEQUE, 9000, "Main",
                new EmploymentDetails("Rules", "Plot 1", ""));
        assertThrows(BankException.class, () -> service.withdraw(chq.getAccountNumber(), 5000.01));
        assertThrows(BankException.class, () -> service.deposit(chq.getAccountNumber(), -1));
        service.setAccountStatus(chq.getAccountNumber(), AccountStatus.SUSPENDED);
        assertThrows(BankException.class, () -> service.deposit(chq.getAccountNumber(), 10));
    }

    @Test
    void threeFailedLoginsLockTheUserUntilAdminUnlocks() {
        for (int i = 0; i < 3; i++) {
            assertThrows(BankException.class, () -> service.authenticate("mpho", "wrong"));
        }
        assertThrows(BankException.class, () -> service.authenticate("mpho", "Password1"));
        Customer mpho = service.getAllCustomers().stream()
                .filter(c -> c.getUser().getUsername().equals("mpho")).findFirst().orElseThrow();
        service.unlockCustomer(mpho.getCustomerId());
        assertNotNull(service.authenticate("mpho", "Password1"));
    }
}
