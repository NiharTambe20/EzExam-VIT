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
            String rawUrl = System.getenv("EZEXAM_DB_URL");
            if (rawUrl == null || rawUrl.isBlank()) {
                rawUrl = props.getProperty("db.url", 
                    "jdbc:mysql://localhost:3306/ezexam?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
            }

            String rawUser = System.getenv("EZEXAM_DB_USER");
            if (rawUser == null || rawUser.isBlank()) {
                rawUser = props.getProperty("db.user", null);
            }

            String rawPass = System.getenv("EZEXAM_DB_PASSWORD");
            if (rawPass == null) {
                rawPass = props.getProperty("db.password", null);
            }

            NormalizedConfig parsed = normalizeConfig(rawUrl, rawUser, rawPass);
            url = parsed.jdbcUrl;
            user = parsed.username;
            password = parsed.password;

            if (System.getenv("EZEXAM_DB_URL") == null) {
                System.out.println("EzExam: EZEXAM_DB_URL not set in environment. Using properties/fallback target: " + maskUrl(url));
            }

            // Register JVM shutdown hook as fallback for non-web executions (e.g. tests)
            Runtime.getRuntime().addShutdownHook(new Thread(DBConnection::closePool, "EzExam-HikariPool-ShutdownHook"));

        } catch (ClassNotFoundException e) {
            System.err.println("CRITICAL: MySQL JDBC Driver not found on classpath!");
            e.printStackTrace();
        }
    }

    public static class NormalizedConfig {
        public final String jdbcUrl;
        public final String username;
        public final String password;

        public NormalizedConfig(String jdbcUrl, String username, String password) {
            this.jdbcUrl = jdbcUrl;
            this.username = username;
            this.password = password;
        }
    }

    /**
     * Normalizes JDBC and cloud service URIs (e.g. Aiven, Railway, AWS RDS).
     * Handles 'mysql://' prefixes, URI-embedded credentials, and cloud SSL parameters.
     */
    public static NormalizedConfig normalizeConfig(String rawUrl, String rawUser, String rawPass) {
        if (rawUrl == null || rawUrl.isBlank()) {
            rawUrl = "jdbc:mysql://localhost:3306/ezexam?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        }

        String normalizedUrl = rawUrl.trim();
        if (normalizedUrl.startsWith("mysql://")) {
            normalizedUrl = "jdbc:" + normalizedUrl;
        }

        String resolvedUser = rawUser;
        String resolvedPassword = rawPass;

        // Extract credentials if embedded in authority, e.g. jdbc:mysql://user:pass@host:port/db
        int schemeEnd = normalizedUrl.indexOf("://");
        if (schemeEnd != -1) {
            int atIndex = normalizedUrl.indexOf('@', schemeEnd + 3);
            int slashIndex = normalizedUrl.indexOf('/', schemeEnd + 3);
            if (atIndex != -1 && (slashIndex == -1 || atIndex < slashIndex)) {
                String userInfo = normalizedUrl.substring(schemeEnd + 3, atIndex);
                int colonIndex = userInfo.indexOf(':');
                if (colonIndex != -1) {
                    if (resolvedUser == null || resolvedUser.isBlank() || "root".equals(resolvedUser)) {
                        resolvedUser = userInfo.substring(0, colonIndex);
                    }
                    if (resolvedPassword == null || resolvedPassword.isBlank()) {
                        resolvedPassword = userInfo.substring(colonIndex + 1);
                    }
                } else if (resolvedUser == null || resolvedUser.isBlank() || "root".equals(resolvedUser)) {
                    resolvedUser = userInfo;
                }
                // Strip userInfo@ from the JDBC URL for clean driver parsing
                normalizedUrl = normalizedUrl.substring(0, schemeEnd + 3) + normalizedUrl.substring(atIndex + 1);
            }
        }

        if (resolvedUser == null || resolvedUser.isBlank()) {
            resolvedUser = "root";
        }
        if (resolvedPassword == null) {
            resolvedPassword = "";
        }

        // Normalize parameter naming (e.g. Aiven URI ssl-mode=REQUIRED -> sslMode=REQUIRED)
        normalizedUrl = normalizedUrl.replace("ssl-mode=", "sslMode=");

        // For remote/cloud hosts, ensure SSL is active unless explicitly configured
        boolean isLocal = normalizedUrl.contains("localhost") 
                || normalizedUrl.contains("127.0.0.1") 
                || normalizedUrl.contains("host.docker.internal");
        if (!isLocal && !normalizedUrl.contains("sslMode=") && !normalizedUrl.contains("useSSL=")) {
            normalizedUrl += (normalizedUrl.contains("?") ? "&" : "?") + "sslMode=REQUIRED";
        }

        // Ensure deterministic UTC timezone handling
        if (!normalizedUrl.contains("serverTimezone=")) {
            normalizedUrl += (normalizedUrl.contains("?") ? "&" : "?") + "serverTimezone=UTC";
        }

        // Ensure public key retrieval for MySQL 8+ caching_sha2_password authentication
        if (!normalizedUrl.contains("allowPublicKeyRetrieval=")) {
            normalizedUrl += (normalizedUrl.contains("?") ? "&" : "?") + "allowPublicKeyRetrieval=true";
        }

        return new NormalizedConfig(normalizedUrl, resolvedUser, resolvedPassword);
    }

    /**
     * Masks database URL for safe logging without exposing passwords or query secrets.
     */
    public static String maskUrl(String jdbcUrl) {
        if (jdbcUrl == null) return "null";
        int queryIndex = jdbcUrl.indexOf('?');
        return queryIndex != -1 ? jdbcUrl.substring(0, queryIndex) + "?..." : jdbcUrl;
    }

    /**
     * Initializes the HikariCP connection pool with conservative sizing.
     */
    public static HikariDataSource initPool() {
        if (dataSource == null) {
            synchronized (LOCK) {
                if (dataSource == null) {
                    try {
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

                        System.out.println("EzExam: Initializing connection pool for " + maskUrl(url) + " [user=" + user + "]");
                        dataSource = new HikariDataSource(config);
                        System.out.println("EzExam: HikariCP connection pool successfully initialized.");
                    } catch (Exception e) {
                        System.err.println("EzExam: Failed to initialize connection pool for " + maskUrl(url) + ": " + e.getMessage());
                        throw e;
                    }
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

