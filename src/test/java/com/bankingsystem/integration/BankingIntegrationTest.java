package com.bankingsystem.integration;

import com.bankingsystem.BankingSystem;
import com.bankingsystem.controller.AccountController;
import com.bankingsystem.controller.AdminController;
import com.bankingsystem.controller.AuthenticationController;
import com.bankingsystem.controller.dto.AccountSummary;
import com.bankingsystem.controller.dto.CustomerSummary;
import com.bankingsystem.controller.dto.ActionResult;
import com.bankingsystem.controller.dto.RegistrationRequest;
import com.bankingsystem.controller.dto.TransactionRow;
import com.bankingsystem.dao.DataSeeder;
import com.bankingsystem.dao.Database;
import com.bankingsystem.model.Account;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end tests through the controller, domain, service and JDBC/H2 layers
 * (the same path the JavaFX views use).
 */
class BankingIntegrationTest {
    private BankingSystem system;
    private AuthenticationController auth;
    private AccountController accounts;
    private AdminController admin;

    @BeforeEach
    void setUp() {
        system = new BankingSystem(Database.inMemory("test-" + UUID.randomUUID()));
        system.startSystem(true);
        auth = system.getAuthenticationController();
        accounts = system.getAccountController();
        admin = system.getAdminController();
    }

    @AfterEach
    void tearDown() {
        system.stopSystem();
    }

    @Test
    void sampleDataContainsAtLeastTenRecords() {
        assertTrue(system.getCustomerDAO().count() >= 10);
        assertTrue(system.getAccountDAO().count() >= 10);
        assertTrue(system.getTransactionDAO().count() >= 10);
    }

    @Test
    void loginDepositAndPersistFlow() {
        ActionResult<String> login = auth.validateCredentials("kmolefe", DataSeeder.SAMPLE_PASSWORD);
        assertTrue(login.success(), login.message());
        assertTrue(auth.getSession().isCustomer());

        AccountSummary first = accounts.getMyAccounts().get(0);
        int before = system.getTransactionDAO().findByAccount(first.accountNumber()).size();

        ActionResult<TransactionRow> deposit = accounts.processDeposit(first.accountNumber(), "250.00",
                DataSeeder.SAMPLE_PASSWORD);
        assertTrue(deposit.success(), deposit.message());

        Account persisted = system.getAccountDAO().getAccount(first.accountNumber()).orElseThrow();
        assertEquals(first.balance() + 250.0, persisted.getBalance(), 0.001);
        assertEquals(before + 1, system.getTransactionDAO().findByAccount(first.accountNumber()).size());
    }

    @Test
    void depositIsRejectedWithWrongPasswordOrInvalidAmount() {
        auth.validateCredentials("kmolefe", DataSeeder.SAMPLE_PASSWORD);
        String number = accounts.getMyAccounts().get(0).accountNumber();
        assertFalse(accounts.processDeposit(number, "100", "wrong-password").success());
        assertFalse(accounts.processDeposit(number, "-5", DataSeeder.SAMPLE_PASSWORD).success());
        assertFalse(accounts.processDeposit(number, "abc", DataSeeder.SAMPLE_PASSWORD).success());
    }

    @Test
    void savingsWithdrawalIsBlockedAndInvestmentWithdrawalWorks() {
        auth.validateCredentials("kmolefe", DataSeeder.SAMPLE_PASSWORD);
        ActionResult<AccountSummary> savings = accounts.openAccount("SAVINGS", "Gaborone Main", "100", null, null, null);
        assertTrue(savings.success(), savings.message());
        assertFalse(accounts.processWithdrawal(savings.data().accountNumber(), "10", DataSeeder.SAMPLE_PASSWORD).success());

        assertFalse(accounts.openAccount("INVESTMENT", "Gaborone Main", "100", null, null, null).success());
        ActionResult<AccountSummary> investment = accounts.openAccount("INVESTMENT", "Gaborone Main", "1000",
                null, null, null);
        assertTrue(investment.success(), investment.message());
        ActionResult<TransactionRow> withdrawal = accounts.processWithdrawal(investment.data().accountNumber(), "400",
                DataSeeder.SAMPLE_PASSWORD);
        assertTrue(withdrawal.success(), withdrawal.message());
        assertEquals(600, withdrawal.data().balanceAfter(), 0.001);
    }

    @Test
    void chequeAccountNeedsEmploymentDetails() {
        auth.validateCredentials("kmolefe", DataSeeder.SAMPLE_PASSWORD);
        assertFalse(accounts.openAccount("CHEQUE", "Maun", "100", "", "", "").success());
        assertTrue(accounts.openAccount("CHEQUE", "Maun", "100", "Debswana", "Jwaneng", "Engineer").success());
    }

    @Test
    void transferMovesMoneyBetweenAccounts() {
        auth.validateCredentials("kmolefe", DataSeeder.SAMPLE_PASSWORD);
        String from = accounts.openAccount("INVESTMENT", "Maun", "2000", null, null, null).data().accountNumber();
        String to = accounts.openAccount("SAVINGS", "Maun", "10", null, null, null).data().accountNumber();
        assertTrue(accounts.processTransfer(from, to, "500", DataSeeder.SAMPLE_PASSWORD).success());
        assertEquals(1500, system.getAccountDAO().getAccount(from).orElseThrow().getBalance(), 0.001);
        assertEquals(510, system.getAccountDAO().getAccount(to).orElseThrow().getBalance(), 0.001);
    }

    @Test
    void threeFailedLoginsLockTheCustomerAndAdminCanUnlock() {
        for (int i = 0; i < 3; i++) {
            assertFalse(auth.validateCredentials("kmolefe", "bad").success());
        }
        assertFalse(auth.validateCredentials("kmolefe", DataSeeder.SAMPLE_PASSWORD).success());

        assertTrue(auth.loginAdministrator(DataSeeder.ADMIN_USERNAME, DataSeeder.ADMIN_PASSWORD).success());
        String id = admin.getAllCustomers().stream().filter(c -> c.username().equals("kmolefe"))
                .findFirst().orElseThrow().customerId();
        assertTrue(admin.unlockCustomer(id).success());
        auth.logout();
        assertTrue(auth.validateCredentials("kmolefe", DataSeeder.SAMPLE_PASSWORD).success());
    }

    @Test
    void adminPaysMonthlyInterestOnlyToInterestBearingAccounts() {
        auth.loginAdministrator(DataSeeder.ADMIN_USERNAME, DataSeeder.ADMIN_PASSWORD);
        List<AccountSummary> before = admin.getAllAccounts();
        ActionResult<List<TransactionRow>> result = admin.payMonthlyInterest();
        assertTrue(result.success(), result.message());
        for (AccountSummary old : before) {
            Account now = system.getAccountDAO().getAccount(old.accountNumber()).orElseThrow();
            double expected = "ACTIVE".equals(old.status()) && old.balance() > 0
                    ? old.balance() + old.balance() * old.monthlyInterestPercent() / 100
                    : old.balance();
            assertEquals(expected, now.getBalance(), 0.01, old.accountNumber() + " " + old.accountType());
        }
        assertFalse(admin.generateReport("INTEREST").isBlank());
    }

    @Test
    void registrationValidatesAgeAndCreatesLoginableCustomer() {
        RegistrationRequest minor = new RegistrationRequest("INDIVIDUAL", "Young", "Person", "111", null, null,
                LocalDate.now().minusYears(16), "P.O. Box 1", "Plot 1", "0", "younguser", "secret12", "secret12");
        assertFalse(system.getRegistrationController().register(minor).success());

        RegistrationRequest adult = new RegistrationRequest("INDIVIDUAL", "Neo", "Kgosi", "222", null, null,
                LocalDate.of(1995, 5, 5), "P.O. Box 2", "Plot 2", "8000", "neokgosi", "secret12", "secret12");
        ActionResult<String> result = system.getRegistrationController().register(adult);
        assertTrue(result.success(), result.message());
        assertTrue(auth.validateCredentials("neokgosi", "secret12").success());
        assertTrue(accounts.getMyAccounts().isEmpty());
    }

    @Test
    void customerCannotAccessAnotherCustomersAccount() {
        auth.validateCredentials("kmolefe", DataSeeder.SAMPLE_PASSWORD);
        String mine = accounts.getMyAccounts().get(0).accountNumber();
        auth.logout();
        auth.loginAdministrator(DataSeeder.ADMIN_USERNAME, DataSeeder.ADMIN_PASSWORD);
        String other = admin.getAllAccounts().stream().map(AccountSummary::accountNumber)
                .filter(n -> !n.equals(mine)).filter(n -> !accountBelongsToKmolefe(n)).findFirst().orElseThrow();
        auth.logout();
        auth.validateCredentials("kmolefe", DataSeeder.SAMPLE_PASSWORD);
        assertFalse(accounts.processDeposit(other, "10", DataSeeder.SAMPLE_PASSWORD).success());
    }

    @Test
    void interestIsPaidOnlyOncePerMonth() {
        auth.loginAdministrator(DataSeeder.ADMIN_USERNAME, DataSeeder.ADMIN_PASSWORD);
        assertTrue(admin.payMonthlyInterest().success());
        double total = admin.getAllAccounts().stream().mapToDouble(AccountSummary::balance).sum();
        ActionResult<List<TransactionRow>> second = admin.payMonthlyInterest();
        assertTrue(second.data() == null || second.data().isEmpty(), second.message());
        assertEquals(total, admin.getAllAccounts().stream().mapToDouble(AccountSummary::balance).sum(), 0.001);
    }

    @Test
    void lockedCustomerCannotMoveFundsEvenWhileLoggedIn() {
        auth.validateCredentials("kmolefe", DataSeeder.SAMPLE_PASSWORD);
        String number = accounts.getMyAccounts().get(0).accountNumber();
        String id = auth.getSession().getUserId();
        new com.bankingsystem.service.CustomerService(system.getCustomerDAO(), null).lockAccount(id);
        assertFalse(accounts.processDeposit(number, "10", DataSeeder.SAMPLE_PASSWORD).success());
    }

    @Test
    void centAmountsAreAcceptedAndHugeOrExponentAmountsRejected() {
        auth.validateCredentials("kmolefe", DataSeeder.SAMPLE_PASSWORD);
        String number = accounts.getMyAccounts().get(0).accountNumber();
        assertTrue(accounts.processDeposit(number, "0.29", DataSeeder.SAMPLE_PASSWORD).success());
        assertFalse(accounts.processDeposit(number, "1e16", DataSeeder.SAMPLE_PASSWORD).success());
        assertFalse(accounts.processDeposit(number, "10000000000000", DataSeeder.SAMPLE_PASSWORD).success());
        assertFalse(accounts.processDeposit(number, "1.234", DataSeeder.SAMPLE_PASSWORD).success());
    }

    @Test
    void customerWithAccountsCannotBeDeleted() {
        auth.loginAdministrator(DataSeeder.ADMIN_USERNAME, DataSeeder.ADMIN_PASSWORD);
        String id = admin.getAllCustomers().get(0).customerId();
        assertFalse(admin.deleteCustomer(id).success());
        assertTrue(system.getCustomerDAO().findById(id).isPresent());
    }

    @Test
    void passwordsAreStoredAsSaltedPbkdf2Hashes() {
        String stored = system.getCustomerDAO().findCustomer("kmolefe").orElseThrow().getPassword();
        assertTrue(stored.startsWith("pbkdf2$"));
        assertNotEquals(stored, system.getCustomerDAO().findCustomer("tsetshwane").orElseThrow().getPassword());
    }

    @Test
    void adminCustomerListShowsTotalBalanceOfAllAccounts() {
        auth.loginAdministrator(DataSeeder.ADMIN_USERNAME, DataSeeder.ADMIN_PASSWORD);
        CustomerSummary kagiso = admin.getAllCustomers().stream()
                .filter(c -> c.username().equals("kmolefe")).findFirst().orElseThrow();
        double expected = admin.getAllAccounts().stream()
                .filter(a -> a.customerId().equals(kagiso.customerId()))
                .mapToDouble(AccountSummary::balance).sum();
        assertTrue(expected > 0);
        assertEquals(expected, kagiso.totalBalance(), 0.001);
    }

    private boolean accountBelongsToKmolefe(String accountNumber) {
        return system.getAccountDAO().getAccount(accountNumber).orElseThrow()
                .getHolder().getUsername().equals("kmolefe");
    }
}
