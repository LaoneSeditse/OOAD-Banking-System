package com.bankingsystem.dao;

import com.bankingsystem.model.BankAdministrator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * CRUD operations for the {@code administrators} table.
 */
public class AdministratorDAO {
    private final Database database;

    public AdministratorDAO(Database database) {
        this.database = database;
    }

    public void insert(BankAdministrator admin) {
        String sql = "INSERT INTO administrators (admin_id, name, email, username, password_hash) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, admin.getAdminId());
            ps.setString(2, admin.getName());
            ps.setString(3, admin.getEmail());
            ps.setString(4, admin.getUsername());
            ps.setString(5, admin.getPasswordHash());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not insert administrator", e);
        }
    }

    public Optional<BankAdministrator> findByUsername(String username) {
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM administrators WHERE LOWER(username) = LOWER(?)")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new BankAdministrator(rs.getString("admin_id"), rs.getString("name"),
                        rs.getString("email"), rs.getString("username"), rs.getString("password_hash")));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load administrator", e);
        }
    }

    public int count() {
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM administrators");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new DataAccessException("Could not count administrators", e);
        }
    }
}
