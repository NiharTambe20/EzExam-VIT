package com.exam.util;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Database connection manager using standard JDBC.
 * Reads configuration with precedence:
 * 1. Environment variables (EZEXAM_DB_URL, EZEXAM_DB_USER, EZEXAM_DB_PASSWORD)
 * 2. Classpath resource: db.properties
 * 3. Default fallback for local development
 */
public class DBConnection {

    private static String url;
    private static String user;
    private static String password;

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

        } catch (ClassNotFoundException e) {
            System.err.println("CRITICAL: MySQL JDBC Driver not found on classpath!");
            e.printStackTrace();
        }
    }

    /**
     * Obtains a new JDBC Connection.
     * Callers must close connections via try-with-resources.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public static String getUrl() {
        return url;
    }

    public static String getUser() {
        return user;
    }
}
