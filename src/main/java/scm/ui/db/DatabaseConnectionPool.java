package scm.ui.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.Properties;

/**
 * CREATIONAL PATTERN — SINGLETON
 * DatabaseConnectionPool ensures a single shared pool of JDBC connections
 * to the OOAD MySQL database (schema.sql + schema-extension.sql).
 */
public class DatabaseConnectionPool {

    // ── Singleton instance ──────────────────────────────────────────────────
    private static volatile DatabaseConnectionPool instance;

    // ── Connection config (compatible with database module + legacy keys) ───
    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/OOAD?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "tarun12345";
    private static final int DEFAULT_POOL_SIZE = 10;

    private static final Properties FILE_PROPS = loadProperties();
    private static final String URL = resolveUrl();
    private static final String USER = resolveUser();
    private static final String PASSWORD = resolvePassword();
    private static final int POOL_SIZE = resolvePoolSize();

    private final BlockingQueue<Connection> pool = new ArrayBlockingQueue<>(POOL_SIZE);

    // ── Private constructor ─────────────────────────────────────────────────
    private DatabaseConnectionPool() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            for (int i = 0; i < POOL_SIZE; i++) {
                pool.offer(DriverManager.getConnection(URL, USER, PASSWORD));
            }
        } catch (ClassNotFoundException | SQLException e) {
            // Allow the UI to run without DB (demo mode)
            System.err.println("[DB] Cannot connect to MySQL — running in DEMO mode: " + e.getMessage());
        }
    }

    private static Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream cp = DatabaseConnectionPool.class.getClassLoader().getResourceAsStream("database.properties")) {
            if (cp != null) props.load(cp);
        } catch (IOException e) {
            System.err.println("[DB] Could not load classpath database.properties: " + e.getMessage());
        }
        Path localFile = Path.of("database.properties");
        if (Files.exists(localFile)) {
            try (InputStream file = Files.newInputStream(localFile)) {
                Properties local = new Properties();
                local.load(file);
                props.putAll(local);
            } catch (IOException e) {
                System.err.println("[DB] Could not load local database.properties: " + e.getMessage());
            }
        }
        return props;
    }

    private static String resolveUrl() {
        return firstNonBlank(
                System.getProperty("db.url"),
                System.getenv("DB_URL"),
                System.getenv("SCM_DB_URL"),
                FILE_PROPS.getProperty("db.url"),
                DEFAULT_URL
        );
    }

    private static String resolveUser() {
        return firstNonBlank(
                System.getProperty("db.username"),
                System.getenv("DB_USERNAME"),
                System.getenv("SCM_DB_USER"),
                FILE_PROPS.getProperty("db.username"),
                FILE_PROPS.getProperty("db.user"),
                DEFAULT_USER
        );
    }

    private static String resolvePassword() {
        return firstNonBlank(
                System.getProperty("db.password"),
                System.getenv("DB_PASSWORD"),
                System.getenv("SCM_DB_PASSWORD"),
                FILE_PROPS.getProperty("db.password"),
                DEFAULT_PASSWORD
        );
    }

    private static int resolvePoolSize() {
        String raw = firstNonBlank(
                System.getProperty("db.pool.size"),
                System.getenv("DB_POOL_SIZE"),
                FILE_PROPS.getProperty("db.pool.size")
        );
        if (raw == null) return DEFAULT_POOL_SIZE;
        try {
            int parsed = Integer.parseInt(raw.trim());
            return parsed > 0 ? parsed : DEFAULT_POOL_SIZE;
        } catch (NumberFormatException e) {
            return DEFAULT_POOL_SIZE;
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    // ── Thread-safe lazy initialisation ────────────────────────────────────
    public static DatabaseConnectionPool getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnectionPool.class) {
                if (instance == null) {
                    instance = new DatabaseConnectionPool();
                }
            }
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        Connection c = pool.poll();
        if (c == null || c.isClosed()) {
            c = DriverManager.getConnection(URL, USER, PASSWORD);
        }
        return c;
    }

    public void releaseConnection(Connection c) {
        if (c != null) {
            try {
                if (!c.isClosed()) pool.offer(c);
            } catch (SQLException ignored) {}
        }
    }

    public void shutdown() {
        for (Connection c : pool) {
            try { c.close(); } catch (SQLException ignored) {}
        }
        pool.clear();
    }
}
