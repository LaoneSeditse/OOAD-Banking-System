package com.bankingsystem.dao;

import com.bankingsystem.model.AccountType;
import com.bankingsystem.model.BankAdministrator;
import com.bankingsystem.model.Customer;
import com.bankingsystem.model.EmploymentDetails;
import com.bankingsystem.model.exception.BankingException;
import com.bankingsystem.service.AccountService;
import com.bankingsystem.service.CustomerService;
import com.bankingsystem.util.PasswordUtil;

import java.time.LocalDate;

/**
 * Inserts sample records (10 customers with accounts and transactions, plus an administrator)
 * the first time the application runs against an empty database.
 * All sample customers use the password {@value #SAMPLE_PASSWORD}.
 */
public class DataSeeder {
    public static final String SAMPLE_PASSWORD = "password123";
    public static final String ADMIN_USERNAME = "admin";
    public static final String ADMIN_PASSWORD = "admin123";

    private final CustomerDAO customerDAO;
    private final AdministratorDAO administratorDAO;
    private final CustomerService customerService;
    private final AccountService accountService;

    public DataSeeder(CustomerDAO customerDAO, AdministratorDAO administratorDAO,
                      CustomerService customerService, AccountService accountService) {
        this.customerDAO = customerDAO;
        this.administratorDAO = administratorDAO;
        this.customerService = customerService;
        this.accountService = accountService;
    }

    public void seedIfEmpty() {
        try {
            if (administratorDAO.count() == 0) {
                administratorDAO.insert(new BankAdministrator("ADM001", "System Administrator", "admin@bank.co.bw",
                        ADMIN_USERNAME, PasswordUtil.hash(ADMIN_USERNAME, ADMIN_PASSWORD)));
            }
            if (customerDAO.count() == 0) {
                seedCustomers();
            }
        } catch (BankingException e) {
            throw new IllegalStateException("Sample data could not be inserted: " + e.getMessage(), e);
        }
    }

    private void seedCustomers() throws BankingException {
        Customer c1 = individual("kmolefe", "Kagiso", "Molefe", "198512345", LocalDate.of(1985, 3, 14),
                "P.O. Box 101, Gaborone", "Plot 1234, Block 6, Gaborone", 18500);
        String c1Sav = open(c1, AccountType.SAVINGS, 2500, "Gaborone Main", null);
        String c1Inv = open(c1, AccountType.INVESTMENT, 10000, "Gaborone Main", null);
        String c1Chq = open(c1, AccountType.CHEQUE, 4200, "Gaborone Main",
                new EmploymentDetails("Debswana", "Plot 64, Gaborone CBD", "Mining Engineer"));
        accountService.deposit(c1Chq, 18500);
        accountService.withdraw(c1Chq, 3000);
        accountService.transfer(c1Chq, c1Sav, 1500);
        accountService.deposit(c1Inv, 2000);

        Customer c2 = individual("tsetshwane", "Tumelo", "Setshwane", "199023456", LocalDate.of(1990, 7, 2),
                "P.O. Box 220, Francistown", "Plot 55, Monarch, Francistown", 9200);
        String c2Sav = open(c2, AccountType.SAVINGS, 800, "Francistown", null);
        open(c2, AccountType.CHEQUE, 1500, "Francistown",
                new EmploymentDetails("Botswana Railways", "Mahalapye Station Rd", "Technician"));
        accountService.deposit(c2Sav, 450);

        Customer c3 = individual("nmokoena", "Neo", "Mokoena", "199534567", LocalDate.of(1995, 11, 21),
                "P.O. Box 33, Maun", "Plot 9, Boseja, Maun", 6000);
        String c3Inv = open(c3, AccountType.INVESTMENT, 5000, "Maun", null);
        accountService.withdraw(c3Inv, 750);

        Customer c4 = individual("lkgosi", "Lesego", "Kgosi", "198845678", LocalDate.of(1988, 1, 30),
                "P.O. Box 12, Serowe", "Plot 300, Serowe", 22000);
        open(c4, AccountType.SAVINGS, 12000, "Serowe", null);
        String c4Chq = open(c4, AccountType.CHEQUE, 6500, "Serowe",
                new EmploymentDetails("Ministry of Health", "Government Enclave, Gaborone", "Nurse"));
        accountService.withdraw(c4Chq, 1200);

        Customer c5 = individual("omasire", "Onalenna", "Masire", "200056789", LocalDate.of(2000, 5, 9),
                "P.O. Box 77, Lobatse", "Plot 18, Peleng, Lobatse", 4500);
        open(c5, AccountType.SAVINGS, 350, "Lobatse", null);

        Customer c6 = individual("bseretse", "Boitumelo", "Seretse", "198367890", LocalDate.of(1983, 9, 17),
                "P.O. Box 909, Gaborone", "Plot 77, Phakalane", 35000);
        open(c6, AccountType.INVESTMENT, 25000, "Gaborone Main", null);
        String c6Chq = open(c6, AccountType.CHEQUE, 9000, "Gaborone Main",
                new EmploymentDetails("FNB Botswana", "Plot 54362, CBD Gaborone", "Branch Manager"));
        accountService.deposit(c6Chq, 35000);

        Customer c7 = individual("pdintwe", "Phenyo", "Dintwe", "199778901", LocalDate.of(1997, 12, 3),
                "P.O. Box 44, Palapye", "Plot 2100, Palapye", 7800);
        open(c7, AccountType.CHEQUE, 2000, "Palapye",
                new EmploymentDetails("BIUST", "Private Bag 16, Palapye", "Lab Assistant"));

        Customer c8 = individual("gmotsumi", "Goitseone", "Motsumi", "199289012", LocalDate.of(1992, 4, 25),
                "P.O. Box 5, Kasane", "Plot 41, Kasane", 11000);
        open(c8, AccountType.SAVINGS, 1800, "Kasane", null);
        open(c8, AccountType.INVESTMENT, 750, "Kasane", null);

        Customer c9 = company("kalaharitech", "Kalahari Tech (Pty) Ltd", "BW00001234567", LocalDate.of(1980, 6, 11),
                "P.O. Box 4000, Gaborone", "Plot 50369, Fairgrounds, Gaborone", 250000);
        String c9Inv = open(c9, AccountType.INVESTMENT, 150000, "Gaborone Main", null);
        String c9Chq = open(c9, AccountType.CHEQUE, 80000, "Gaborone Main",
                new EmploymentDetails("Kalahari Tech (Pty) Ltd", "Plot 50369, Fairgrounds", "Managing Director"));
        accountService.transfer(c9Chq, c9Inv, 4500);

        Customer c10 = company("okavangofoods", "Okavango Foods (Pty) Ltd", "BW00007654321", LocalDate.of(1978, 2, 19),
                "P.O. Box 150, Maun", "Plot 12, Industrial Area, Maun", 120000);
        open(c10, AccountType.SAVINGS, 30000, "Maun", null);
        String c10Inv = open(c10, AccountType.INVESTMENT, 45000, "Maun", null);
        accountService.deposit(c10Inv, 5000);
    }

    private Customer individual(String username, String first, String last, String idNumber, LocalDate dob,
                                String postal, String physical, double income) throws BankingException {
        return customerService.registerIndividual(username, SAMPLE_PASSWORD, dob, first, last, idNumber,
                postal, physical, income);
    }

    private Customer company(String username, String name, String regNo, LocalDate repDob,
                             String postal, String physical, double income) throws BankingException {
        return customerService.registerCompany(username, SAMPLE_PASSWORD, repDob, name, regNo, postal, physical, income);
    }

    private String open(Customer customer, AccountType type, double deposit, String branch, EmploymentDetails emp)
            throws BankingException {
        return accountService.openAccount(customer.getCustomerId(), type, deposit, branch, emp).getAccountNumber();
    }
}
