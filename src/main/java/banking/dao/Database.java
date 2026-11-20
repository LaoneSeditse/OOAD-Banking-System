package banking.dao;

import banking.exception.DataAccessException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {
    private static String databasePath = "bank.db";
    private static Database instance;

    private Connection connection;
    private int transactionDepth = 0;

    private Database() { }

    public static synchronized Database getInstance() {
        if (instance == null) {
            instance = new Database();
        }
        return instance;
    }

    public static synchronized void useFile(String path) {
        if (instance != null) {
            instance.disconnect();
        }
        instance = null;
        databasePath = path;
    }

    public synchronized void connect() {
        try {
            if (connection != null && !connection.isClosed()) {
                return;
            }
            connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
            }
            createSchema();
        } catch (SQLException e) {
            throw new DataAccessException("Could not connect to the database: " + e.getMessage(), e);
        }
    }

    public synchronized void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not close the database: " + e.getMessage(), e);
        } finally {
            connection = null;
        }
    }

    public synchronized Connection getConnection() {
        connect();
        return connection;
    }

    public synchronized void runInTransaction(Runnable work) {
        Connection c = getConnection();
        boolean outermost = transactionDepth == 0;
        transactionDepth++;
        try {
            if (outermost) {
                c.setAutoCommit(false);
            }
            work.run();
            if (outermost) {
                c.commit();
            }
        } catch (SQLException e) {
            rollbackQuietly(c, outermost);
            throw new DataAccessException("Database transaction failed: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            rollbackQuietly(c, outermost);
            throw e;
        } finally {
            transactionDepth--;
            if (outermost) {
                try {
                    c.setAutoCommit(true);
                } catch (SQLException ignored) {
                    // connection already unusable
                }
            }
        }
    }

    private void rollbackQuietly(Connection c, boolean outermost) {
        if (outermost) {
            try {
                c.rollback();
            } catch (SQLException ignored) {
                // nothing more we can do
            }
        }
    }

    private void createSchema() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS users ("
                    + "user_id TEXT PRIMARY KEY, "
                    + "username TEXT NOT NULL UNIQUE, "
                    + "password_hash TEXT NOT NULL, "
                    + "date_of_birth TEXT, "
                    + "role TEXT NOT NULL CHECK (role IN ('CUSTOMER','ADMIN')), "
                    + "is_active INTEGER NOT NULL DEFAULT 1, "
                    + "failed_attempts INTEGER NOT NULL DEFAULT 0)");

            st.execute("CREATE TABLE IF NOT EXISTS customers ("
                    + "customer_id TEXT PRIMARY KEY, "
                    + "customer_type TEXT NOT NULL CHECK (customer_type IN ('INDIVIDUAL','COMPANY')), "
                    + "user_id TEXT NOT NULL UNIQUE REFERENCES users(user_id), "
                    + "first_name TEXT, last_name TEXT, "
                    + "company_name TEXT, registration_name TEXT, "
                    + "postal_address TEXT NOT NULL, physical_address TEXT NOT NULL, "
                    + "social_income REAL NOT NULL DEFAULT 0)");

            st.execute("CREATE TABLE IF NOT EXISTS accounts ("
                    + "account_number TEXT PRIMARY KEY, "
                    + "customer_id TEXT NOT NULL REFERENCES customers(customer_id) ON DELETE CASCADE, "
                    + "account_type TEXT NOT NULL CHECK (account_type IN ('SAVINGS','INVESTMENT','CHEQUE')), "
                    + "balance REAL NOT NULL DEFAULT 0, "
                    + "branch TEXT NOT NULL, "
                    + "status TEXT NOT NULL CHECK (status IN ('ACTIVE','SUSPENDED','CLOSED')), "
                    + "employer_name TEXT, employer_address TEXT, employer_position TEXT, "
                    + "employment_verified INTEGER NOT NULL DEFAULT 0)");

            st.execute("CREATE TABLE IF NOT EXISTS transactions ("
                    + "transaction_id TEXT PRIMARY KEY, "
                    + "account_number TEXT NOT NULL REFERENCES accounts(account_number) ON DELETE CASCADE, "
                    + "tx_date TEXT NOT NULL, "
                    + "amount REAL NOT NULL, "
                    + "tx_type TEXT NOT NULL CHECK (tx_type IN ('DEPOSIT','WITHDRAWAL','TRANSFER','INTEREST')), "
                    + "description TEXT)");
        }
    }

    public String getName() {
        return databasePath;
    }
}
