package com.exam.listener;

import com.exam.util.DBConnection;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * ServletContextListener managing application lifecycle events.
 * Eagerly warms up the HikariCP connection pool on container startup and
 * guarantees graceful release of pooled connections on container shutdown.
 */
@WebListener
public class AppContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            DBConnection.initPool();
            sce.getServletContext().log("EzExam: HikariCP connection pool initialized successfully.");
        } catch (Exception e) {
            sce.getServletContext().log("EzExam: HikariCP connection pool initialization deferred: " + e.getMessage());
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        try {
            DBConnection.closePool();
            sce.getServletContext().log("EzExam: HikariCP connection pool closed cleanly.");
        } catch (Exception e) {
            sce.getServletContext().log("EzExam: Error closing HikariCP connection pool: " + e.getMessage());
        }
    }
}
