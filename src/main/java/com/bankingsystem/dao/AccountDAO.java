package com.bankingsystem.dao;

import com.bankingsystem.model.Account;
import com.bankingsystem.model.AccountStatus;
import com.bankingsystem.model.AccountType;
import com.bankingsystem.model.ChequeAccount;
import com.bankingsystem.model.Customer;
import com.bankingsystem.model.EmploymentDetails;
import com.bankingsystem.model.InvestmentAccount;
import com.bankingsystem.model.SavingsAccount;
import com.bankingsystem.service.AccountNumberGenerator;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * CRUD operations for the {@code accounts} and {@code employment_details} tables.
 * Rebuilds the correct {@link Account} subclass for each row (polymorphic mapping).
 */
public class AccountDAO implements AccountNumberGenerator {
    private static final String SELECT = "SELECT a.*, e.company_name AS emp_company, e.company_address AS emp_address, "
            + "e.position AS emp_position FROM accounts a "
            + "LEFT JOIN employment_details e ON e.account_number = a.account_number ";

    private final Database database;
    private final CustomerDAO customerDAO;

    public AccountDAO(Database database, CustomerDAO customerDAO) {
        this.database = database;
        this.customerDAO = customerDAO;
    }

    /** Account numbers start with 1 (Savings), 2 (Investment) or 3 (Cheque), followed by 7 digits. */
    @Override
    public String nextAccountNumber(AccountType type) {
        String prefix = switch (type) {
            case SAVINGS -> "1";
            case INVESTMENT -> "2";
            case CHEQUE -> "3";
        };
        try (Connection conn = database.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT NEXT VALUE FOR account_seq")) {
            rs.next();
            return prefix + rs.getLong(1);
        } catch (SQLException e) {
            throw new DataAccessException("Could not generate account number", e);
        }
    }

    public void insert(Account account) {
        String sql = "INSERT INTO accounts (account_number, customer_id, account_type, balance, branch, status, "
                + "interest_rate, opened_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = database.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, account.getAccountNumber());
                ps.setString(2, account.getHolder().getCustomerId());
                ps.setString(3, account.getAccountType().name());
                ps.setDouble(4, account.getBalance());
                ps.setString(5, account.getBranch());
                ps.setString(6, account.getStatus().name());
                ps.setDouble(7, account.getInterestRate());
                ps.setDate(8, Date.valueOf(account.getOpenedDate()));
                ps.executeUpdate();
                if (account instanceof ChequeAccount cheque && cheque.getEmploymentDetails() != null) {
                    insertEmployment(conn, account.getAccountNumber(), cheque.getEmploymentDetails());
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not insert account " + account.getAccountNumber(), e);
        }
    }

    private static void insertEmployment(Connection conn, String accountNumber, EmploymentDetails details)
            throws SQLException {
        String sql = "INSERT INTO employment_details (account_number, company_name, company_address, position) "
                + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            ps.setString(2, details.getCompanyName());
            ps.setString(3, details.getCompanyAddress());
            ps.setString(4, details.getPosition());
            ps.executeUpdate();
        }
    }

    /** getAccount(accountNumber) in the Deposit sequence diagram. */
    public Optional<Account> getAccount(String accountNumber) {
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE a.account_number = ?")) {
            ps.setString(1, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                Optional<Customer> holder = customerDAO.findById(rs.getString("customer_id"));
                return holder.isPresent() ? Optional.of(map(rs, holder.get())) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load account " + accountNumber, e);
        }
    }

    /** Loads all accounts of a customer and attaches them to that customer object. */
    public List<Account> findByCustomer(Customer customer) {
        List<Account> result = new ArrayList<>();
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE a.customer_id = ? ORDER BY a.account_number")) {
            ps.setString(1, customer.getCustomerId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String number = rs.getString("account_number");
                    Optional<Account> existing = customer.findAccount(number);
                    result.add(existing.isPresent() ? existing.get() : map(rs, customer));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load accounts of " + customer.getCustomerId(), e);
        }
    }

    public List<Account> findAll() {
        List<Account> result = new ArrayList<>();
        Map<String, Customer> holders = new HashMap<>();
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT + "ORDER BY a.account_number");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String customerId = rs.getString("customer_id");
                Customer holder = holders.computeIfAbsent(customerId, id -> customerDAO.findById(id).orElse(null));
                if (holder != null) {
                    result.add(map(rs, holder));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load accounts", e);
        }
    }

    /** saveAccount(account) in the Deposit sequence diagram: persists balance, branch and status. */
    public void saveAccount(Account account) {
        String sql = "UPDATE accounts SET balance = ?, branch = ?, status = ? WHERE account_number = ?";
        try (Connection conn = database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, account.getBalance());
            ps.setString(2, account.getBranch());
            ps.setString(3, account.getStatus().name());
            ps.setString(4, account.getAccountNumber());
            if (ps.executeUpdate() == 0) {
                throw new DataAccessException("Account " + account.getAccountNumber() + " does not exist", null);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not save account " + account.getAccountNumber(), e);
        }
    }

    public void delete(String accountNumber) {
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM accounts WHERE account_number = ?")) {
            ps.setString(1, accountNumber);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete account " + accountNumber, e);
        }
    }

    public int count() {
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM accounts");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new DataAccessException("Could not count accounts", e);
        }
    }

    private static Account map(ResultSet rs, Customer holder) throws SQLException {
        String number = rs.getString("account_number");
        String branch = rs.getString("branch");
        double balance = rs.getDouble("balance");
        AccountStatus status = AccountStatus.valueOf(rs.getString("status"));
        LocalDate opened = rs.getDate("opened_date").toLocalDate();
        return switch (AccountType.valueOf(rs.getString("account_type"))) {
            case SAVINGS -> new SavingsAccount(holder, number, branch, balance, status, opened);
            case INVESTMENT -> new InvestmentAccount(holder, number, branch, balance, status, opened);
            case CHEQUE -> new ChequeAccount(holder, number, branch,
                    new EmploymentDetails(rs.getString("emp_company"), rs.getString("emp_address"),
                            rs.getString("emp_position")),
                    balance, status, opened);
        };
    }
}
