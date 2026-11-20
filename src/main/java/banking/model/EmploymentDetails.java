package banking.model;

public class EmploymentDetails {
    private final String companyName;
    private final String companyAddress;
    private final String position;

    public EmploymentDetails(String companyName, String companyAddress, String position) {
        this.companyName = companyName == null ? "" : companyName.trim();
        this.companyAddress = companyAddress == null ? "" : companyAddress.trim();
        this.position = position == null ? "" : position.trim();
    }

    public String getCompanyName() { return companyName; }
    public String getCompanyAddress() { return companyAddress; }
    public String getPosition() { return position; }

    public boolean verify() {
        return !companyName.isEmpty() && !companyAddress.isEmpty();
    }
}
