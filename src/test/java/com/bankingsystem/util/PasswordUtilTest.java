package com.bankingsystem.util;

import org.junit.jupiter.api.Test;

import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordUtilTest {

    @Test
    void newHashesAreSaltedPbkdf2() {
        String a = PasswordUtil.hash("secret1");
        assertTrue(a.startsWith("pbkdf2$"));
        assertNotEquals(a, PasswordUtil.hash("secret1"));
        assertTrue(PasswordUtil.matches("alice", "secret1", a));
        assertFalse(PasswordUtil.matches("alice", "wrong", a));
        assertFalse(PasswordUtil.needsRehash(a));
    }

    @Test
    void legacySha256HashesStillVerifyAndAreFlaggedForRehash() {
        String legacy = HexFormat.of().formatHex(PasswordUtil.legacySha256("Admin", "admin123"));
        assertTrue(PasswordUtil.matches("admin", "admin123", legacy));
        assertFalse(PasswordUtil.matches("admin", "nope", legacy));
        assertTrue(PasswordUtil.needsRehash(legacy));
    }
}
