package com.bankingsystem.dao;

import com.bankingsystem.model.CompanyCustomer;
import com.bankingsystem.model.Customer;
import com.bankingsystem.model.IndividualCustomer;
import com.bankingsystem.model.UserStatus;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * CRUD operations for the {@code customers} table.
 */
public class CustomerDAO {
    private final Database database;

    public CustomerDAO(Database database) {
        this.database = database;
    }

    /** Inserts a new customer and assigns the generated customer id to it. */
    public void insert(Customer customer) {
        String sql = "INSERT INTO customers (customer_id, customer_type, username, password_hash, date_of_birth, "
                + "first_name, last_name, id_number, company_name, registration_number, postal_address, "
                + "physical_address, social_income, is_active, status, failed_login_attempts) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = database.getConnection()) {
            String id = "CUS" + nextSequence(conn);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, id);
                ps.setString(2, customer.getCustomerType());
                ps.setString(3, customer.getUsername());
                ps.setString(4, customer.getPassword());
                ps.setDate(5, Date.valueOf(customer.getDateOfBirth()));
                if (customer instanceof IndividualCustomer ind) {
                    ps.setString(6, ind.getFirstName());
                    ps.setString(7, ind.getLastName());
                    ps.setString(8, ind.getIdNumber());
                    ps.setString(9, null);
                    ps.setString(10, null);
                } else if (customer instanceof CompanyCustomer comp) {
                    ps.setString(6, null);
                    ps.setString(7, null);
                    ps.setString(8, null);
                    ps.setString(9, comp.getCompanyName());
                    ps.setString(10, comp.getRegistrationNumber());
                }
                ps.setString(11, customer.getPostalAddress());
                ps.setString(12, customer.getPhysicalAddress());
                ps.setDouble(13, customer.getSocialIncome());
                ps.setBoolean(14, customer.isActive());
                ps.setString(15, customer.getStatus().name());
                ps.setInt(16, customer.getFailedLoginAttempts());
                ps.executeUpdate();
            }
            customer.setCustomerId(id);
        } catch (SQLException e) {
            throw new DataAccessException("Could not insert customer " + customer.getUsername(), e);
        }
    }

    private static long nextSequence(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT NEXT VALUE FOR customer_seq")) {
            rs.next();
            return rs.getLong(1);
        }
    }

    public Optional<Customer> findById(String customerId) {
        return queryOne("SELECT * FROM customers WHERE customer_id = ?", customerId);
    }

    /** findCustomer(username) in the Login sequence diagram. */
    public Optional<Customer> findCustomer(String username) {
        return queryOne("SELECT * FROM customers WHERE LOWER(username) = LOWER(?)", username);
    }

    public List<Customer> findAll() {
        List<Customer> result = new ArrayList<>();
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM customers ORDER BY customer_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(map(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load customers", e);
        }
    }

    public boolean usernameExists(String username) {
        return findCustomer(username).isPresent();
    }

    public void update(Customer customer) {
        String sql = "UPDATE customers SET password_hash = ?, date_of_birth = ?, first_name = ?, last_name = ?, "
                + "company_name = ?, registration_number = ?, postal_address = ?, physical_address = ?, "
                + "social_income = ?, is_active = ?, status = ?, failed_login_attempts = ? WHERE customer_id = ?";
        try (Connection conn = database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customer.getPassword());
            ps.setDate(2, Date.valueOf(customer.getDateOfBirth()));
            boolean individual = customer instanceof IndividualCustomer;
            ps.setString(3, individual ? ((IndividualCustomer) customer).getFirstName() : null);
            ps.setString(4, individual ? ((IndividualCustomer) customer).getLastName() : null);
            ps.setString(5, individual ? null : ((CompanyCustomer) customer).getCompanyName());
            ps.setString(6, individual ? null : ((CompanyCustomer) customer).getRegistrationNumber());
            ps.setString(7, customer.getPostalAddress());
            ps.setString(8, customer.getPhysicalAddress());
            ps.setDouble(9, customer.getSocialIncome());
            ps.setBoolean(10, customer.isActive());
            ps.setString(11, customer.getStatus().name());
            ps.setInt(12, customer.getFailedLoginAttempts());
            ps.setString(13, customer.getCustomerId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not update customer " + customer.getCustomerId(), e);
        }
    }

    public void updateLoginState(Customer customer) {
        String sql = "UPDATE customers SET status = ?, failed_login_attempts = ? WHERE customer_id = ?";
        try (Connection conn = database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customer.getStatus().name());
            ps.setInt(2, customer.getFailedLoginAttempts());
            ps.setString(3, customer.getCustomerId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not update login state", e);
        }
    }

    /** updateStatus(customerId, LOCKED) in the Login sequence diagram. Unlocking resets failed attempts. */
    public void updateStatus(String customerId, UserStatus status) {
        String sql = "UPDATE customers SET status = ?, failed_login_attempts = "
                + "CASE WHEN ? = 'ACTIVE' THEN 0 ELSE failed_login_attempts END WHERE customer_id = ?";
        try (Connection conn = database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setString(2, status.name());
            ps.setString(3, customerId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not update customer status", e);
        }
    }

    public void delete(String customerId) {
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM customers WHERE customer_id = ?")) {
            ps.setString(1, customerId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete customer " + customerId, e);
        }
    }

    public int count() {
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM customers");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new DataAccessException("Could not count customers", e);
        }
    }

    private Optional<Customer> queryOne(String sql, String param) {
        try (Connection conn = database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load customer", e);
        }
    }

    private static Customer map(ResultSet rs) throws SQLException {
        String id = rs.getString("customer_id");
        Customer customer;
        if ("COMPANY".equals(rs.getString("customer_type"))) {
            customer = new CompanyCustomer(id, rs.getString("username"), rs.getString("password_hash"),
                    rs.getDate("date_of_birth").toLocalDate(), rs.getString("company_name"),
                    rs.getString("registration_number"), rs.getString("postal_address"),
                    rs.getString("physical_address"), rs.getDouble("social_income"));
        } else {
            customer = new IndividualCustomer(id, rs.getString("username"), rs.getString("password_hash"),
                    rs.getDate("date_of_birth").toLocalDate(), rs.getString("first_name"),
                    rs.getString("last_name"), rs.getString("id_number"), rs.getString("postal_address"),
                    rs.getString("physical_address"), rs.getDouble("social_income"));
        }
        customer.setActive(rs.getBoolean("is_active"));
        customer.setStatus(UserStatus.valueOf(rs.getString("status")));
        customer.setFailedLoginAttempts(rs.getInt("failed_login_attempts"));
        return customer;
    }
}
