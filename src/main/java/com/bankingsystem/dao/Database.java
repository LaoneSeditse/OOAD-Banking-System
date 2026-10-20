package com.bankingsystem.dao;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.concurrent.locks.ReentrantLock;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * JDBC connection provider (Database / Repository in the diagrams). Uses an embedded H2
 * database stored in {@code ./data/bankingdb} by default, or an in-memory database for tests.
 */
public class Database {
    public static final String DEFAULT_URL = "jdbc:h2:./data/bankingdb;AUTO_SERVER=TRUE";
    private static final String SCHEMA_RESOURCE = "/db/schema.sql";

    private final String url;
    private final String user;
    private final String password;
    private Connection keepAlive;
    private final ThreadLocal<Connection> transactionConnection = new ThreadLocal<>();
    private final ReentrantLock writeLock = new ReentrantLock();

    /** A unit of work that runs inside {@link #inTransaction(TransactionWork)}. */
    @FunctionalInterface
    public interface TransactionWork<T, E extends Exception> {
        T run() throws E;
    }

    public Database(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public Database(String url) {
        this(url, "sa", "");
    }

    public static Database inMemory(String name) {
        return new Database("jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1");
    }

    /** Opens the database and creates the schema if it does not exist yet. */
    public void connect() {
        try {
            if (keepAlive == null || keepAlive.isClosed()) {
                keepAlive = DriverManager.getConnection(url, user, password);
            }
            initialiseSchema();
        } catch (SQLException e) {
            throw new DataAccessException("Could not connect to database " + url, e);
        }
    }

    public void disconnect() {
        try {
            if (keepAlive != null && !keepAlive.isClosed()) {
                keepAlive.close();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not close database connection", e);
        } finally {
            keepAlive = null;
        }
    }

    public boolean isConnected() {
        try {
            return keepAlive != null && !keepAlive.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Returns a connection. Inside {@link #inTransaction(TransactionWork)} every DAO call shares the
     * transaction's connection (its close() is ignored until the transaction ends).
     */
    public Connection getConnection() throws SQLException {
        Connection tx = transactionConnection.get();
        if (tx != null) {
            return nonClosing(tx);
        }
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Runs {@code work} as one JDBC transaction: all DAO writes commit together or are rolled back
     * together. Write transactions are serialised so concurrent balance updates cannot be lost.
     */
    public <T, E extends Exception> T inTransaction(TransactionWork<T, E> work) throws E {
        if (transactionConnection.get() != null) {
            return work.run();
        }
        writeLock.lock();
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url, user, password);
            conn.setAutoCommit(false);
            transactionConnection.set(conn);
            T result = work.run();
            conn.commit();
            return result;
        } catch (SQLException e) {
            rollback(conn);
            throw new DataAccessException("Database transaction failed", e);
        } catch (RuntimeException | Error e) {
            rollback(conn);
            throw e;
        } catch (Exception e) {
            rollback(conn);
            throw e;
        } finally {
            transactionConnection.remove();
            closeQuietly(conn);
            writeLock.unlock();
        }
    }

    private static Connection nonClosing(Connection target) {
        return (Connection) Proxy.newProxyInstance(Database.class.getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    if ("close".equals(method.getName())) {
                        return null;
                    }
                    try {
                        return method.invoke(target, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }

    private static void rollback(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ignored) {
                // the original failure is more relevant than a failed rollback
            }
        }
    }

    private static void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException ignored) {
                // nothing useful to do
            }
        }
    }

    public String getUrl() {
        return url;
    }

    private void initialiseSchema() throws SQLException {
        String script = readSchema();
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            for (String sql : script.split(";")) {
                String trimmed = stripComments(sql).trim();
                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed);
                }
            }
        }
    }

    private static String stripComments(String sql) {
        StringBuilder sb = new StringBuilder();
        for (String line : sql.split("\\R")) {
            if (!line.trim().startsWith("--")) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    private static String readSchema() {
        try (InputStream in = Database.class.getResourceAsStream(SCHEMA_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Missing " + SCHEMA_RESOURCE);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + SCHEMA_RESOURCE, e);
        }
    }
}
