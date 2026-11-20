package banking.model;

public enum AccountType {
    SAVINGS("Savings"),
    INVESTMENT("Investment"),
    CHEQUE("Cheque");

    private final String label;

    AccountType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
