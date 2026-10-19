package com.bankingsystem.dao;

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

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
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
