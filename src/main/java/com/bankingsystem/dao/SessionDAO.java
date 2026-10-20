package com.bankingsystem.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Persists login sessions (saveSession(customerId, token) in the Login sequence diagram).
 */
public class SessionDAO {
    private final Database database;

    public SessionDAO(Database database) {
        this.database = database;
    }

    public void saveSession(String customerId, String token) {
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO sessions (token, customer_id) VALUES (?, ?)")) {
            ps.setString(1, token);
            ps.setString(2, customerId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not save session", e);
        }
    }

    public Optional<String> findCustomerId(String token) {
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT customer_id FROM sessions WHERE token = ?")) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getString(1)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load session", e);
        }
    }

    public void deleteSession(String token) {
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM sessions WHERE token = ?")) {
            ps.setString(1, token);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete session", e);
        }
    }
}
