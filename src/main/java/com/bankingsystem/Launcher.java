package com.bankingsystem;

/**
 * Plain main class so the application can be started from a fat jar / IDE without JavaFX module flags.
 */
public final class Launcher {
    private Launcher() {
    }

    public static void main(String[] args) {
        BankingApp.main(args);
    }
}
