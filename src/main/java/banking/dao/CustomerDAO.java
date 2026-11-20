package banking.dao;

import banking.exception.DataAccessException;
import banking.model.CompanyCustomer;
import banking.model.Customer;
import banking.model.IndividualCustomer;
import banking.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CustomerDAO {
    private final UserDAO userDAO = new UserDAO();
    private final AccountDAO accountDAO = new AccountDAO();

    private Connection conn() {
        return Database.getInstance().getConnection();
    }

    public void insert(Customer customer) {
        Database.getInstance().runInTransaction(() -> {
            userDAO.insert(customer.getUser());
            String sql = "INSERT INTO customers (customer_id, customer_type, user_id, first_name, last_name, "
                    + "company_name, registration_name, postal_address, physical_address, social_income) "
                    + "VALUES (?,?,?,?,?,?,?,?,?,?)";
            try (PreparedStatement ps = conn().prepareStatement(sql)) {
                ps.setString(1, customer.getCustomerId());
                ps.setString(3, customer.getUser().getUserId());
                ps.setString(8, customer.getPostalAddress());
                ps.setString(9, customer.getPhysicalAddress());
                ps.setDouble(10, customer.getSocialIncome());
                if (customer instanceof IndividualCustomer ind) {
                    ps.setString(2, "INDIVIDUAL");
                    ps.setString(4, ind.getFirstName());
                    ps.setString(5, ind.getLastName());
                    ps.setString(6, null);
                    ps.setString(7, null);
                } else if (customer instanceof CompanyCustomer comp) {
                    ps.setString(2, "COMPANY");
                    ps.setString(4, null);
                    ps.setString(5, null);
                    ps.setString(6, comp.getCompanyName());
                    ps.setString(7, comp.getRegistrationName());
                }
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new DataAccessException("Could not save customer: " + e.getMessage(), e);
            }
        });
    }

    public Customer findById(String customerId) {
        return findOne("SELECT * FROM customers WHERE customer_id = ?", customerId);
    }

    public Customer findByUserId(String userId) {
        return findOne("SELECT * FROM customers WHERE user_id = ?", userId);
    }

    public Customer findByAccountNumber(String accountNumber) {
        String customerId = accountDAO.findCustomerIdByAccountNumber(accountNumber);
        return customerId == null ? null : findById(customerId);
    }

    public List<Customer> findAll() {
        List<String> ids = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement("SELECT customer_id FROM customers ORDER BY rowid");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ids.add(rs.getString("customer_id"));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not read customers: " + e.getMessage(), e);
        }
        List<Customer> customers = new ArrayList<>();
        for (String id : ids) {
            customers.add(findById(id));
        }
        return customers;
    }

    public void update(Customer customer) {
        String sql = "UPDATE customers SET postal_address = ?, physical_address = ?, social_income = ?, company_name = ? "
                + "WHERE customer_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, customer.getPostalAddress());
            ps.setString(2, customer.getPhysicalAddress());
            ps.setDouble(3, customer.getSocialIncome());
            ps.setString(4, customer instanceof CompanyCustomer c ? c.getCompanyName() : null);
            ps.setString(5, customer.getCustomerId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not update customer: " + e.getMessage(), e);
        }
    }

    public void delete(String customerId) {
        Customer customer = findById(customerId);
        if (customer == null) {
            return;
        }
        Database.getInstance().runInTransaction(() -> {
            try (PreparedStatement ps = conn().prepareStatement("DELETE FROM customers WHERE customer_id = ?")) {
                ps.setString(1, customerId);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new DataAccessException("Could not delete customer: " + e.getMessage(), e);
            }
            userDAO.delete(customer.getUser().getUserId());
        });
    }

    private Customer findOne(String sql, String value) {
        String id, type, userId, first, last, companyName, regName, postal, physical;
        double income;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                id = rs.getString("customer_id");
                type = rs.getString("customer_type");
                userId = rs.getString("user_id");
                first = rs.getString("first_name");
                last = rs.getString("last_name");
                companyName = rs.getString("company_name");
                regName = rs.getString("registration_name");
                postal = rs.getString("postal_address");
                physical = rs.getString("physical_address");
                income = rs.getDouble("social_income");
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not read customer: " + e.getMessage(), e);
        }

        User user = userDAO.findById(userId);
        Customer customer = "INDIVIDUAL".equals(type)
                ? new IndividualCustomer(id, user, first, last, postal, physical, income)
                : new CompanyCustomer(id, user, companyName, regName, postal, physical, income);
        accountDAO.loadAccountsFor(customer);
        return customer;
    }
}
