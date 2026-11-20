package banking.dao;

import banking.exception.DataAccessException;
import banking.model.Transaction;
import banking.model.TransactionType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAO {

    private Connection conn() {
        return Database.getInstance().getConnection();
    }

    public void insert(Transaction t) {
        String sql = "INSERT INTO transactions (transaction_id, account_number, tx_date, amount, tx_type, description) "
                + "VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, t.getTransactionId());
            ps.setString(2, t.getAccountNumber());
            ps.setString(3, t.getDate().toString());
            ps.setDouble(4, t.getAmount());
            ps.setString(5, t.getType().name());
            ps.setString(6, t.getDescription());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not save transaction: " + e.getMessage(), e);
        }
    }

    public List<Transaction> findByAccount(String accountNumber) {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE account_number = ? ORDER BY tx_date, rowid";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Transaction(
                            rs.getString("transaction_id"),
                            rs.getString("account_number"),
                            LocalDateTime.parse(rs.getString("tx_date")),
                            rs.getDouble("amount"),
                            TransactionType.valueOf(rs.getString("tx_type")),
                            rs.getString("description")));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not read transactions: " + e.getMessage(), e);
        }
        return list;
    }
}
