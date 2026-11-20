package banking.dao;

import banking.exception.DataAccessException;
import banking.model.Role;
import banking.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    private Connection conn() {
        return Database.getInstance().getConnection();
    }

    public void insert(User user) {
        String sql = "INSERT INTO users (user_id, username, password_hash, date_of_birth, role, is_active, failed_attempts) "
                + "VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, user.getUserId());
            ps.setString(2, user.getUsername());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getDateOfBirth() == null ? null : user.getDateOfBirth().toString());
            ps.setString(5, user.getRole().name());
            ps.setInt(6, user.isActive() ? 1 : 0);
            ps.setInt(7, user.getFailedAttempts());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not save user: " + e.getMessage(), e);
        }
    }

    public User findById(String userId) {
        return findOne("SELECT * FROM users WHERE user_id = ?", userId);
    }

    public User findByUsername(String username) {
        return findOne("SELECT * FROM users WHERE username = ?", username);
    }

    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement("SELECT * FROM users ORDER BY username");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not read users: " + e.getMessage(), e);
        }
        return list;
    }

    public void update(User user) {
        String sql = "UPDATE users SET password_hash = ?, is_active = ?, failed_attempts = ? WHERE user_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, user.getPasswordHash());
            ps.setInt(2, user.isActive() ? 1 : 0);
            ps.setInt(3, user.getFailedAttempts());
            ps.setString(4, user.getUserId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not update user: " + e.getMessage(), e);
        }
    }

    public void delete(String userId) {
        try (PreparedStatement ps = conn().prepareStatement("DELETE FROM users WHERE user_id = ?")) {
            ps.setString(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete user: " + e.getMessage(), e);
        }
    }

    private User findOne(String sql, String value) {
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not read user: " + e.getMessage(), e);
        }
    }

    private User map(ResultSet rs) throws SQLException {
        String dob = rs.getString("date_of_birth");
        return new User(
                rs.getString("user_id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                dob == null ? null : LocalDate.parse(dob),
                Role.valueOf(rs.getString("role")),
                rs.getInt("is_active") == 1,
                rs.getInt("failed_attempts"));
    }
}
