package scm.ui.db;

import com.jackfruit.scm.database.facade.SupplyChainDatabaseFacade;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Bootstraps the shared database module and aligns configuration keys.
 */
public final class DatabaseModuleBootstrap {

    private static volatile boolean initialised;

    private DatabaseModuleBootstrap() {}

    public static void bootstrap() {
        if (initialised) return;
        synchronized (DatabaseModuleBootstrap.class) {
            if (initialised) return;

            Properties fileProps = loadDatabaseProperties();
            setSystemPropertyIfMissing("db.url",
                    firstNonBlank(System.getenv("DB_URL"),
                            System.getenv("SCM_DB_URL"),
                            fileProps.getProperty("db.url")));
            setSystemPropertyIfMissing("db.username",
                    firstNonBlank(System.getenv("DB_USERNAME"),
                            System.getenv("SCM_DB_USER"),
                            fileProps.getProperty("db.username"),
                            fileProps.getProperty("db.user")));
            setSystemPropertyIfMissing("db.password",
                    firstNonBlank(System.getenv("DB_PASSWORD"),
                            System.getenv("SCM_DB_PASSWORD"),
                            fileProps.getProperty("db.password")));
            setSystemPropertyIfMissing("db.pool.size",
                    firstNonBlank(System.getenv("DB_POOL_SIZE"),
                            fileProps.getProperty("db.pool.size")));

            try (SupplyChainDatabaseFacade ignored = new SupplyChainDatabaseFacade()) {
                initialised = true;
            }
        }
    }

    private static Properties loadDatabaseProperties() {
        Properties props = new Properties();
        try (InputStream cp = DatabaseModuleBootstrap.class.getClassLoader().getResourceAsStream("database.properties")) {
            if (cp != null) props.load(cp);
        } catch (IOException e) {
            System.err.println("[DB] Could not read classpath database.properties: " + e.getMessage());
        }

        Path localFile = Path.of("database.properties");
        if (Files.exists(localFile)) {
            try (InputStream file = Files.newInputStream(localFile)) {
                Properties local = new Properties();
                local.load(file);
                props.putAll(local);
            } catch (IOException e) {
                System.err.println("[DB] Could not read local database.properties: " + e.getMessage());
            }
        }
        return props;
    }

    private static void setSystemPropertyIfMissing(String key, String value) {
        if (isBlank(value)) return;
        String existing = System.getProperty(key);
        if (isBlank(existing)) {
            System.setProperty(key, value.trim());
        }
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (!isBlank(v)) return v.trim();
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
