package com.bankingsystem.model;

import com.bankingsystem.util.PasswordUtil;

import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;

/**
 * Abstract base class for anybody who can log in to the banking system.
 * Encapsulates credentials and the login lifecycle (Customer State Machine).
 */
public abstract class User {
    public static final int MINIMUM_AGE = 18;
    public static final int MAX_FAILED_ATTEMPTS = 3;

    private String userId;
    private String username;
    private String password;
    private LocalDate dateOfBirth;
    private boolean isActive;
    private UserStatus status;
    private int failedLoginAttempts;
    private boolean loggedIn;

    protected User(String userId, String username, String password, LocalDate dateOfBirth) {
        this.userId = userId;
        this.username = Objects.requireNonNull(username, "username");
        this.password = Objects.requireNonNull(password, "password");
        this.dateOfBirth = Objects.requireNonNull(dateOfBirth, "dateOfBirth");
        this.isActive = true;
        this.status = UserStatus.ACTIVE;
    }

    /**
     * Compares the supplied credentials with the stored ones. {@code password} is the plain password
     * entered by the user; it is checked against the stored salted hash.
     */
    public boolean login(String username, String password) {
        if (!isActive || status == UserStatus.LOCKED) {
            return false;
        }
        boolean ok = this.username.equalsIgnoreCase(username) && PasswordUtil.matches(password, this.password);
        if (ok) {
            failedLoginAttempts = 0;
            loggedIn = true;
        } else {
            registerFailedAttempt();
        }
        return ok;
    }

    public void logout() {
        loggedIn = false;
    }

    public void updatePassword(String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        this.password = newPassword;
    }

    /** @return true if this attempt caused the user to become LOCKED */
    public boolean registerFailedAttempt() {
        failedLoginAttempts++;
        if (failedLoginAttempts >= MAX_FAILED_ATTEMPTS) {
            lock();
            return true;
        }
        return false;
    }

    public void lock() {
        status = UserStatus.LOCKED;
        loggedIn = false;
    }

    public void unlock() {
        status = UserStatus.ACTIVE;
        failedLoginAttempts = 0;
    }

    public int getAge() {
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    public boolean isAdult() {
        return getAge() >= MINIMUM_AGE;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }
}
