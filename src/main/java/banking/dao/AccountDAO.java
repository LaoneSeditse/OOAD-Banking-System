package banking.dao;

import banking.exception.DataAccessException;
import banking.model.Account;
import banking.model.AccountStatus;
import banking.model.AccountType;
import banking.model.ChequeAccount;
import banking.model.Customer;
import banking.model.EmploymentDetails;
import banking.model.InvestmentAccount;
import banking.model.SavingsAccount;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AccountDAO {
    private final TransactionDAO transactionDAO = new TransactionDAO();

    private Connection conn() {
        return Database.getInstance().getConnection();
    }

    public void insert(Account account) {
        String sql = "INSERT INTO accounts (account_number, customer_id, account_type, balance, branch, status, "
                + "employer_name, employer_address, employer_position, employment_verified) VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, account.getAccountNumber());
            ps.setString(2, account.getCustomer().getCustomerId());
            ps.setString(3, account.getAccountType().name());
            ps.setDouble(4, account.getBalance());
            ps.setString(5, account.getBranch());
            ps.setString(6, account.getStatus().name());
            if (account instanceof ChequeAccount cheque) {
                ps.setString(7, cheque.getEmploymentDetails().getCompanyName());
                ps.setString(8, cheque.getEmploymentDetails().getCompanyAddress());
                ps.setString(9, cheque.getEmploymentDetails().getPosition());
                ps.setInt(10, cheque.isEmploymentVerified() ? 1 : 0);
            } else {
                ps.setString(7, null);
                ps.setString(8, null);
                ps.setString(9, null);
                ps.setInt(10, 0);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not save account: " + e.getMessage(), e);
        }
    }

    public void update(Account account) {
        String sql = "UPDATE accounts SET balance = ?, status = ? WHERE account_number = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setDouble(1, account.getBalance());
            ps.setString(2, account.getStatus().name());
            ps.setString(3, account.getAccountNumber());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not update account: " + e.getMessage(), e);
        }
    }

    public void delete(String accountNumber) {
        try (PreparedStatement ps = conn().prepareStatement("DELETE FROM accounts WHERE account_number = ?")) {
            ps.setString(1, accountNumber);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete account: " + e.getMessage(), e);
        }
    }

    public String findCustomerIdByAccountNumber(String accountNumber) {
        try (PreparedStatement ps = conn().prepareStatement("SELECT customer_id FROM accounts WHERE account_number = ?")) {
            ps.setString(1, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("customer_id") : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not read account: " + e.getMessage(), e);
        }
    }

    public void loadAccountsFor(Customer customer) {
        record Row(String number, AccountType type, double balance, String branch, AccountStatus status,
                   String empName, String empAddress, String empPosition) { }

        List<Row> rows = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT * FROM accounts WHERE customer_id = ? ORDER BY rowid")) {
            ps.setString(1, customer.getCustomerId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Row(
                            rs.getString("account_number"),
                            AccountType.valueOf(rs.getString("account_type")),
                            rs.getDouble("balance"),
                            rs.getString("branch"),
                            AccountStatus.valueOf(rs.getString("status")),
                            rs.getString("employer_name"),
                            rs.getString("employer_address"),
                            rs.getString("employer_position")));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not read accounts: " + e.getMessage(), e);
        }

        for (Row r : rows) {
            Account account = switch (r.type()) {
                case SAVINGS -> SavingsAccount.restore(r.number(), customer, r.branch(), r.balance(), r.status());
                case INVESTMENT -> InvestmentAccount.restore(r.number(), customer, r.branch(), r.balance(), r.status());
                case CHEQUE -> ChequeAccount.restore(r.number(), customer, r.branch(), r.balance(), r.status(),
                        new EmploymentDetails(r.empName(), r.empAddress(), r.empPosition()));
            };
            transactionDAO.findByAccount(r.number()).forEach(account::attachTransaction);
        }
    }
}
