package banking.service;

import banking.model.AccountType;
import banking.model.Customer;
import banking.model.EmploymentDetails;

import java.time.LocalDate;

public final class DataSeeder {
    public static final String SAMPLE_PASSWORD = "Password1";
    public static final String ADMIN_USERNAME = "admin";
    public static final String ADMIN_PASSWORD = "Admin123";
    private static final String BRANCH = "Gaborone Main";

    private DataSeeder() { }

    public static void seedIfEmpty(BankService service) {
        if (!service.usernameExists(ADMIN_USERNAME)) {
            service.createAdministrator(ADMIN_USERNAME, ADMIN_PASSWORD);
        }
        if (!service.getAllCustomers().isEmpty()) {
            return;
        }

        // ---- 6 individual customers ----
        Customer thabo = individual(service, "thabo", "Thabo", "Mokoena", LocalDate.of(1995, 3, 14),
                "P.O. Box 101, Gaborone", "Plot 1021, Phakalane", 18000);
        Customer neo = individual(service, "neo", "Neo", "Kgosi", LocalDate.of(1990, 7, 2),
                "P.O. Box 220, Francistown", "Plot 55, Phase 4, Francistown", 24000);
        Customer lesedi = individual(service, "lesedi", "Lesedi", "Molefe", LocalDate.of(1988, 11, 25),
                "P.O. Box 78, Maun", "Plot 310, Maun", 15000);
        Customer kefilwe = individual(service, "kefilwe", "Kefilwe", "Sebina", LocalDate.of(2000, 1, 30),
                "P.O. Box 12, Palapye", "Plot 88, Palapye", 9500);
        Customer tebogo = individual(service, "tebogo", "Tebogo", "Dube", LocalDate.of(1993, 9, 9),
                "P.O. Box 400, Gaborone", "Plot 2200, Block 8, Gaborone", 21000);
        Customer mpho = individual(service, "mpho", "Mpho", "Ramotswe", LocalDate.of(1985, 5, 18),
                "P.O. Box 66, Lobatse", "Plot 14, Lobatse", 30000);

        // ---- 4 company customers ----
        Customer kalahari = company(service, "kalahari", "Kalahari Traders (Pty) Ltd", "BW00012345",
                "P.O. Box 900, Gaborone", "Plot 5, Broadhurst Industrial", 450000);
        Customer okavango = company(service, "okavango", "Okavango Logistics", "BW00067890",
                "P.O. Box 15, Maun", "Plot 41, Maun Industrial", 780000);
        Customer limpopo = company(service, "limpopo", "Limpopo Foods", "BW00024680",
                "P.O. Box 512, Selebi-Phikwe", "Plot 9, Selebi-Phikwe", 320000);
        Customer gabtech = company(service, "gabtech", "Gaborone Tech Solutions", "BW00013579",
                "P.O. Box 1450, Gaborone", "Plot 64511, CBD, Gaborone", 560000);

        // ---- accounts (all three types, some customers hold several) ----
        service.createAccount(thabo.getCustomerId(), AccountType.SAVINGS, 1500, BRANCH, null);
        service.createAccount(thabo.getCustomerId(), AccountType.CHEQUE, 4200, BRANCH,
                new EmploymentDetails("Botswana Telecom", "Plot 1, Gaborone", "Network Technician"));
        service.createAccount(neo.getCustomerId(), AccountType.INVESTMENT, 2500, BRANCH, null);
        service.createAccount(lesedi.getCustomerId(), AccountType.SAVINGS, 800, BRANCH, null);
        service.createAccount(lesedi.getCustomerId(), AccountType.INVESTMENT, 1000, BRANCH, null);
        service.createAccount(kefilwe.getCustomerId(), AccountType.CHEQUE, 1800, BRANCH,
                new EmploymentDetails("Palapye Clinic", "Plot 3, Palapye", "Nurse"));
        service.createAccount(tebogo.getCustomerId(), AccountType.SAVINGS, 3000, BRANCH, null);
        service.createAccount(tebogo.getCustomerId(), AccountType.INVESTMENT, 5000, BRANCH, null);
        service.createAccount(tebogo.getCustomerId(), AccountType.CHEQUE, 6500, BRANCH,
                new EmploymentDetails("Ministry of Finance", "Government Enclave, Gaborone", "Accountant"));
        service.createAccount(mpho.getCustomerId(), AccountType.INVESTMENT, 10000, BRANCH, null);
        service.createAccount(kalahari.getCustomerId(), AccountType.CHEQUE, 25000, BRANCH,
                new EmploymentDetails("Kalahari Traders (Pty) Ltd", "Plot 5, Broadhurst Industrial", "Payroll Account"));
        service.createAccount(kalahari.getCustomerId(), AccountType.INVESTMENT, 50000, BRANCH, null);
        service.createAccount(okavango.getCustomerId(), AccountType.SAVINGS, 12000, BRANCH, null);
        service.createAccount(limpopo.getCustomerId(), AccountType.INVESTMENT, 7500, BRANCH, null);
        service.createAccount(gabtech.getCustomerId(), AccountType.CHEQUE, 18000, BRANCH,
                new EmploymentDetails("Gaborone Tech Solutions", "Plot 64511, CBD, Gaborone", "Payroll Account"));

        // ---- a little transaction history ----
        for (Customer c : new Customer[]{thabo, tebogo, kalahari, gabtech}) {
            service.getAccounts(c.getCustomerId()).stream()
                    .filter(a -> a.getAccountType() != AccountType.SAVINGS)
                    .findFirst()
                    .ifPresent(a -> {
                        service.deposit(a.getAccountNumber(), 500);
                        service.withdraw(a.getAccountNumber(), 200);
                    });
        }
    }

    private static Customer individual(BankService s, String username, String first, String last,
                                       LocalDate dob, String postal, String physical, double income) {
        return s.registerIndividual(username, SAMPLE_PASSWORD, first, last, dob, postal, physical, income);
    }

    private static Customer company(BankService s, String username, String name, String reg,
                                    String postal, String physical, double income) {
        return s.registerCompany(username, SAMPLE_PASSWORD, name, reg, postal, physical, income);
    }
}
