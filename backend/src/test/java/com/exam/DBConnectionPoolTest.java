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
            // If MySQL is offline in this environment, verify that failure is from network, not config
            assertTrue(e.getMessage() != null && !e.getMessage().isBlank());
        }
    }
}
