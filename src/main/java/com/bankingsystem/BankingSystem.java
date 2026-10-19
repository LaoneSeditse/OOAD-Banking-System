package com.bankingsystem;

import com.bankingsystem.controller.AccountController;
import com.bankingsystem.controller.AdminController;
import com.bankingsystem.controller.AuthenticationController;
import com.bankingsystem.controller.RegistrationController;
import com.bankingsystem.controller.UserSession;
import com.bankingsystem.dao.AccountDAO;
import com.bankingsystem.dao.AdministratorDAO;
import com.bankingsystem.dao.CustomerDAO;
import com.bankingsystem.dao.DataSeeder;
import com.bankingsystem.dao.Database;
import com.bankingsystem.dao.SessionDAO;
import com.bankingsystem.dao.TransactionDAO;
import com.bankingsystem.service.AccountService;
import com.bankingsystem.service.AdminService;
import com.bankingsystem.service.BankService;
import com.bankingsystem.service.BankServiceImpl;
import com.bankingsystem.service.CustomerService;
import com.bankingsystem.service.TransactionService;
import com.bankingsystem.service.TransactionServiceImpl;

/**
 * The banking system as a whole (BankingSystem class in the Class Diagram). It wires the
 * layers together: Database -> DAOs -> services -> controllers, which the views then use.
 */
public class BankingSystem {
    private final String systemId = "BW-BANK-001";
    private final String name = "OOAD Community Bank";
    private final String address = "Plot 1000, Main Mall, Gaborone, Botswana";

    private final Database database;
    private final CustomerDAO customerDAO;
    private final AccountDAO accountDAO;
    private final TransactionDAO transactionDAO;
    private final AdministratorDAO administratorDAO;
    private final CustomerService customerService;
    private final AccountService accountService;
    private final AdminService adminService;
    private final UserSession session = new UserSession();
    private final AuthenticationController authenticationController;
    private final RegistrationController registrationController;
    private final AccountController accountController;
    private final AdminController adminController;

    public BankingSystem(Database database) {
        this.database = database;
        customerDAO = new CustomerDAO(database);
        accountDAO = new AccountDAO(database, customerDAO);
        transactionDAO = new TransactionDAO(database);
        administratorDAO = new AdministratorDAO(database);
        SessionDAO sessionDAO = new SessionDAO(database);

        BankService bankService = new BankServiceImpl(accountDAO);
        TransactionService transactionService = new TransactionServiceImpl(transactionDAO);
        customerService = new CustomerService(customerDAO, sessionDAO);
        accountService = new AccountService(bankService, accountDAO, customerDAO, transactionService);
        adminService = new AdminService(administratorDAO, customerService, accountService);

        authenticationController = new AuthenticationController(customerService, adminService, session);
        registrationController = new RegistrationController(customerService);
        accountController = new AccountController(accountService, customerService, session);
        adminController = new AdminController(customerService, accountService, adminService, session);
    }

    public static BankingSystem createDefault() {
        return new BankingSystem(new Database(Database.DEFAULT_URL));
    }

    /** Connects to the database, creates the schema and (optionally) inserts the sample records. */
    public void startSystem(boolean seedSampleData) {
        database.connect();
        if (seedSampleData) {
            new DataSeeder(customerDAO, administratorDAO, customerService, accountService).seedIfEmpty();
        }
    }

    public void stopSystem() {
        if (session.isLoggedIn()) {
            authenticationController.logout();
        }
        database.disconnect();
    }

    public String generateReport() {
        return adminService.generateCustomerReport() + "\n" + adminService.generateAccountReport() + "\n"
                + adminService.generateInterestReport();
    }

    public String getSystemId() {
        return systemId;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public Database getDatabase() {
        return database;
    }

    public CustomerDAO getCustomerDAO() {
        return customerDAO;
    }

    public AccountDAO getAccountDAO() {
        return accountDAO;
    }

    public TransactionDAO getTransactionDAO() {
        return transactionDAO;
    }

    public AuthenticationController getAuthenticationController() {
        return authenticationController;
    }

    public RegistrationController getRegistrationController() {
        return registrationController;
    }

    public AccountController getAccountController() {
        return accountController;
    }

    public AdminController getAdminController() {
        return adminController;
    }
}
