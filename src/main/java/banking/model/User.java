package banking.model;

import banking.exception.ValidationException;
import banking.util.PasswordUtil;

import java.time.LocalDate;
import java.time.Period;

public class User {
    public static final int MAX_FAILED_ATTEMPTS = 3;

    private final String userId;
    private final String username;
    private String passwordHash;
    private final LocalDate dateOfBirth;   // null for company users
    private final Role role;
    private boolean active;
    private int failedAttempts;
    private boolean loggedIn;

    public User(String userId, String username, String passwordHash, LocalDate dateOfBirth,
                Role role, boolean active, int failedAttempts) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.dateOfBirth = dateOfBirth;
        this.role = role;
        this.active = active;
        this.failedAttempts = failedAttempts;
    }

    public boolean login(String username, String password) {
        if (!active) {
            return false;
        }
        if (this.username.equals(username) && PasswordUtil.verify(password, passwordHash)) {
            failedAttempts = 0;
            loggedIn = true;
            return true;
        }
        failedAttempts++;
        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            active = false;
        }
        return false;
    }

    public void logout() {
        loggedIn = false;
    }

    public void updatePassword(String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new ValidationException("Password must be at least 6 characters.");
        }
        this.passwordHash = PasswordUtil.hash(newPassword);
    }

    public void unlock() {
        this.active = true;
        this.failedAttempts = 0;
    }

    public boolean passwordMatches(String password) {
        return PasswordUtil.verify(password, passwordHash);
    }

    public int getAge() {
        return dateOfBirth == null ? 0 : Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public Role getRole() { return role; }
    public boolean isActive() { return active; }
    public int getFailedAttempts() { return failedAttempts; }
    public boolean isLoggedIn() { return loggedIn; }
    public int getRemainingAttempts() { return Math.max(0, MAX_FAILED_ATTEMPTS - failedAttempts); }
}
