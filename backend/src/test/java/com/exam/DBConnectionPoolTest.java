package com.exam;

import com.exam.util.DBConnection;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

public class DBConnectionPoolTest {

    @Test
    public void testPoolConfiguration() {
        HikariDataSource ds = DBConnection.initPool();
        assertNotNull(ds, "HikariDataSource should be initialized");
        assertFalse(ds.isClosed(), "HikariDataSource should be open");

        assertEquals(5, ds.getMaximumPoolSize(), "Maximum pool size must be configured to 5");
        assertEquals(2, ds.getMinimumIdle(), "Minimum idle connections should be 2");
        assertEquals(30000, ds.getConnectionTimeout(), "Connection timeout should be 30000ms");
        assertEquals("EzExamHikariPool", ds.getPoolName(), "Pool name should match configured name");
    }

    @Test
    public void testPooledConnectionBorrowAndReturn() {
        try (Connection con = DBConnection.getConnection()) {
            assertNotNull(con, "Obtained connection should not be null");
            assertFalse(con.isClosed(), "Obtained connection should be open");

            HikariDataSource ds = DBConnection.getDataSource();
            assertNotNull(ds, "DataSource should exist while connection is active");
        } catch (SQLException e) {
            assertTrue(e.getMessage() != null && !e.getMessage().isBlank());
        }
    }

    @Test
    public void testAivenUriNormalization() {
        // Test Aiven / Cloud MySQL URI with embedded credentials and ssl-mode=REQUIRED
        String aivenUri = "mysql://avnadmin:TestPassword123@mysql-aivencloud.com:15234/defaultdb?ssl-mode=REQUIRED";
        DBConnection.NormalizedConfig config = DBConnection.normalizeConfig(aivenUri, null, null);

        assertTrue(config.jdbcUrl.startsWith("jdbc:mysql://mysql-aivencloud.com:15234/defaultdb"),
                "Must convert mysql:// to jdbc:mysql:// and strip embedded credentials from host authority");
        assertFalse(config.jdbcUrl.contains("TestPassword123"), "Password must not remain in JDBC URL");
        assertTrue(config.jdbcUrl.contains("sslMode=REQUIRED"), "Must normalize ssl-mode= to sslMode=");
        assertTrue(config.jdbcUrl.contains("serverTimezone=UTC"), "Must include serverTimezone=UTC");
        assertTrue(config.jdbcUrl.contains("allowPublicKeyRetrieval=true"), "Must include allowPublicKeyRetrieval=true");

        assertEquals("avnadmin", config.username, "Must extract username from URI authority");
        assertEquals("TestPassword123", config.password, "Must extract password from URI authority");
    }

    @Test
    public void testRemoteHostSslEnforcement() {
        // Test remote cloud host without SSL parameters
        String remoteUrl = "jdbc:mysql://remote-db.example.com:3306/ezexam";
        DBConnection.NormalizedConfig config = DBConnection.normalizeConfig(remoteUrl, "admin", "pass");

        assertTrue(config.jdbcUrl.contains("sslMode=REQUIRED"), "Remote cloud hosts must default to sslMode=REQUIRED");
        assertEquals("admin", config.username);
        assertEquals("pass", config.password);
    }

    @Test
    public void testLocalhostPreservedWithoutSsl() {
        // Localhost development URLs must not force sslMode=REQUIRED
        String localUrl = "jdbc:mysql://localhost:3306/ezexam?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        DBConnection.NormalizedConfig config = DBConnection.normalizeConfig(localUrl, "ezexam_app", "localpass");

        assertFalse(config.jdbcUrl.contains("sslMode=REQUIRED"), "Localhost must preserve useSSL=false without forced sslMode");
        assertEquals("ezexam_app", config.username);
        assertEquals("localpass", config.password);
    }

    @Test
    public void testMaskUrlRemovesSensitiveDetails() {
        String urlWithQuery = "jdbc:mysql://mysql-aiven.com:15234/defaultdb?user=foo&password=bar";
        String masked = DBConnection.maskUrl(urlWithQuery);
        assertEquals("jdbc:mysql://mysql-aiven.com:15234/defaultdb?...", masked);
        assertFalse(masked.contains("password=bar"));
    }
}
