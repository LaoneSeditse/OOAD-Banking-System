package banking.service;

import banking.dao.Database;

public class BankingSystem {
    private final String systemId = "BS-001";
    private final String name = "BSBS Banking System";
    private final String address = "Gaborone, Botswana";
    private final BankService bankService;

    public BankingSystem(BankService bankService) {
        this.bankService = bankService;
    }

    public void startSystem() {
        Database.getInstance().connect();
        DataSeeder.seedIfEmpty(bankService);
    }

    public void stopSystem() {
        Database.getInstance().disconnect();
    }

    public String generateReport() {
        return bankService.generateSummaryReport();
    }

    public String getSystemId() { return systemId; }
    public String getName() { return name; }
    public String getAddress() { return address; }
}
