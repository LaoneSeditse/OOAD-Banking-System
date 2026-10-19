package com.bankingsystem.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Hashes passwords with SHA-256 so plain-text passwords are never stored in the database.
 * The (lower-cased) username is used as a per-user salt.
 */
public final class PasswordUtil {
    private PasswordUtil() {
    }

    public static String hash(String username, String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String salted = username.toLowerCase(Locale.ROOT) + ":" + rawPassword;
            return HexFormat.of().formatHex(digest.digest(salted.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public static boolean matches(String username, String rawPassword, String storedHash) {
        return rawPassword != null && hash(username, rawPassword).equals(storedHash);
    }
}
