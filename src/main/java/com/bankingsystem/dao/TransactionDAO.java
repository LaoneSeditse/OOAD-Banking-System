package com.bankingsystem.dao;

import com.bankingsystem.model.Transaction;
import com.bankingsystem.model.TransactionStatus;
import com.bankingsystem.model.TransactionType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * CRUD operations for the {@code transactions} table.
 */
public class TransactionDAO {
    private final Database database;

    public TransactionDAO(Database database) {
        this.database = database;
    }

    public void insert(Transaction tx) {
        String sql = "INSERT INTO transactions (transaction_id, account_number, transaction_date, amount, "
                + "transaction_type, description, balance_after, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tx.getTransactionId());
            ps.setString(2, tx.getAccountNumber());
            ps.setTimestamp(3, Timestamp.valueOf(tx.getDate()));
            ps.setDouble(4, tx.getAmount());
            ps.setString(5, tx.getType().name());
            ps.setString(6, tx.getDescription());
            ps.setDouble(7, tx.getBalanceAfter());
            ps.setString(8, tx.getStatus().name());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not insert transaction " + tx.getTransactionId(), e);
        }
    }

    public Optional<Transaction> findById(String transactionId) {
        List<Transaction> list = query("SELECT * FROM transactions WHERE transaction_id = ?", transactionId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public List<Transaction> findByAccount(String accountNumber) {
        return query("SELECT * FROM transactions WHERE account_number = ? "
                + "ORDER BY transaction_date DESC, transaction_id DESC", accountNumber);
    }

    public List<Transaction> findByType(TransactionType type) {
        return query("SELECT * FROM transactions WHERE transaction_type = ? ORDER BY transaction_date DESC", type.name());
    }

    public List<Transaction> findAll() {
        return query("SELECT * FROM transactions ORDER BY transaction_date DESC, transaction_id DESC", null);
    }

    public void updateStatus(String transactionId, TransactionStatus status) {
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE transactions SET status = ? WHERE transaction_id = ?")) {
            ps.setString(1, status.name());
            ps.setString(2, transactionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not update transaction " + transactionId, e);
        }
    }

    public void delete(String transactionId) {
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM transactions WHERE transaction_id = ?")) {
            ps.setString(1, transactionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete transaction " + transactionId, e);
        }
    }

    public int count() {
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM transactions");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new DataAccessException("Could not count transactions", e);
        }
    }

    private List<Transaction> query(String sql, String param) {
        List<Transaction> result = new ArrayList<>();
        try (Connection conn = database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            if (param != null) {
                ps.setString(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new Transaction(rs.getString("transaction_id"), rs.getString("account_number"),
                            rs.getTimestamp("transaction_date").toLocalDateTime(), rs.getDouble("amount"),
                            TransactionType.valueOf(rs.getString("transaction_type")), rs.getString("description"),
                            rs.getDouble("balance_after"), TransactionStatus.valueOf(rs.getString("status"))));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load transactions", e);
        }
    }
}
