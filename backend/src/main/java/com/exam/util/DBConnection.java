package com.exam.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Database connection manager using HikariCP connection pooling.
 * Reads configuration with precedence:
 * 1. Environment variables (EZEXAM_DB_URL, EZEXAM_DB_USER, EZEXAM_DB_PASSWORD)
 * 2. Classpath resource: db.properties
 * 3. Default fallback for local development
 *
 * Configured with a conservative pool size of 5 connections and 30s connection timeout.
 */
public class DBConnection {

    private static String url;
    private static String user;
    private static String password;

    private static volatile HikariDataSource dataSource;
    private static final Object LOCK = new Object();

    static {
        try {
            // Load MySQL Connector/J driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            // 1. Try reading from db.properties
            Properties props = new Properties();
            try (InputStream in = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
                if (in != null) {
                    props.load(in);
                }
            } catch (Exception ignored) {
                // Ignore missing file, fallback to env / defaults
            }

            // Priority: Environment Variable -> Properties file -> Default
            url = System.getenv("EZEXAM_DB_URL");
            if (url == null || url.isBlank()) {
                url = props.getProperty("db.url", 
                    "jdbc:mysql://localhost:3306/ezexam?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
            }

            user = System.getenv("EZEXAM_DB_USER");
            if (user == null || user.isBlank()) {
                user = props.getProperty("db.user", "root");
            }

            password = System.getenv("EZEXAM_DB_PASSWORD");
            if (password == null) {
                password = props.getProperty("db.password", "");
            }

            // Register JVM shutdown hook as fallback for non-web executions (e.g. tests)
            Runtime.getRuntime().addShutdownHook(new Thread(DBConnection::closePool, "EzExam-HikariPool-ShutdownHook"));

        } catch (ClassNotFoundException e) {
            System.err.println("CRITICAL: MySQL JDBC Driver not found on classpath!");
            e.printStackTrace();
        }
    }

    /**
     * Initializes the HikariCP connection pool with conservative sizing.
     */
    public static HikariDataSource initPool() {
        if (dataSource == null) {
            synchronized (LOCK) {
                if (dataSource == null) {
                    HikariConfig config = new HikariConfig();
                    config.setJdbcUrl(url);
                    config.setUsername(user);
                    config.setPassword(password);

                    // Conservative sizing and timeouts for small production deployment
                    config.setMaximumPoolSize(5);
                    config.setMinimumIdle(2);
                    config.setConnectionTimeout(30000); // 30 seconds
                    config.setIdleTimeout(600000);       // 10 minutes
                    config.setMaxLifetime(1800000);     // 30 minutes
                    config.setPoolName("EzExamHikariPool");

                    // Standard MySQL JDBC driver optimizations
                    config.addDataSourceProperty("cachePrepStmts", "true");
                    config.addDataSourceProperty("prepStmtCacheSize", "250");
                    config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                    config.addDataSourceProperty("useServerPrepStmts", "true");

                    dataSource = new HikariDataSource(config);
                }
            }
        }
        return dataSource;
    }

    /**
     * Obtains a pooled JDBC Connection.
     * Callers must close connections via try-with-resources to return them to the pool.
     */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            initPool();
        }
        return dataSource.getConnection();
    }

    /**
     * Gracefully shuts down the HikariCP connection pool.
     */
    public static void closePool() {
        synchronized (LOCK) {
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
                dataSource = null;
            }
        }
    }

    public static HikariDataSource getDataSource() {
        return dataSource;
    }

    public static String getUrl() {
        return url;
    }

    public static String getUser() {
        return user;
    }
}

