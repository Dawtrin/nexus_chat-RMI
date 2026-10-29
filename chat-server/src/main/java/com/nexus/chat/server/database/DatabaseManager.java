package com.nexus.chat.server.database;

import com.nexus.chat.contract.dto.DatabaseHealthDTO;
import com.nexus.chat.contract.enums.ServerStatus;
import com.nexus.chat.server.config.DatabaseConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;

/**
 * Manages database connection pooling with HikariCP and provides health diagnostics.
 * Gracefully degrades if MySQL/MariaDB is offline or disabled.
 */
public class DatabaseManager implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);

    private final DatabaseConfig config;
    private HikariDataSource dataSource;
    private boolean connected = false;
    private String lastError = "";
    private Instant lastCheckedAt = Instant.now();

    public DatabaseManager(DatabaseConfig config) {
        this.config = config;
        initializeDataSource();
    }

    private synchronized void initializeDataSource() {
        if (!config.isEnabled()) {
            log.info("Database persistence is disabled in configuration. Running in resilient in-memory mode.");
            this.connected = false;
            this.lastError = "Database disabled by configuration (database.enabled=false)";
            return;
        }

        try {
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(config.getJdbcUrl());
            hikariConfig.setUsername(config.getUsername());
            hikariConfig.setPassword(config.getPassword());
            hikariConfig.setMaximumPoolSize(config.getPoolMaxSize());
            hikariConfig.setMinimumIdle(1);
            hikariConfig.setConnectionTimeout(3000);
            hikariConfig.setInitializationFailTimeout(2000);
            hikariConfig.setPoolName("NexusChatPool");

            this.dataSource = new HikariDataSource(hikariConfig);

            // Test connection
            try (Connection conn = dataSource.getConnection()) {
                if (conn.isValid(2)) {
                    this.connected = true;
                    this.lastError = "";
                    this.lastCheckedAt = Instant.now();
                    log.info("Successfully connected to MySQL database: {}", config.getDatabaseName());
                }
            }
        } catch (Exception e) {
            this.connected = false;
            this.lastError = e.getMessage();
            this.lastCheckedAt = Instant.now();
            log.warn("Database connection failed (server will operate in resilient in-memory fallback): {}", e.getMessage());
            if (this.dataSource != null) {
                try {
                    this.dataSource.close();
                } catch (Exception ignored) {
                }
                this.dataSource = null;
            }
        }
    }

    public Connection getConnection() throws SQLException {
        if (!connected || dataSource == null) {
            throw new SQLException("Database is not connected: " + lastError);
        }
        return dataSource.getConnection();
    }

    public boolean isConnected() {
        return connected && dataSource != null && !dataSource.isClosed();
    }

    public DatabaseHealthDTO checkHealth() {
        int active = 0;
        int idle = 0;
        int total = 0;
        ServerStatus status;

        if (!config.isEnabled()) {
            status = ServerStatus.UNKNOWN;
        } else if (dataSource != null && !dataSource.isClosed()) {
            try {
                active = dataSource.getHikariPoolMXBean() != null ? dataSource.getHikariPoolMXBean().getActiveConnections() : 0;
                idle = dataSource.getHikariPoolMXBean() != null ? dataSource.getHikariPoolMXBean().getIdleConnections() : 0;
                total = dataSource.getHikariPoolMXBean() != null ? dataSource.getHikariPoolMXBean().getTotalConnections() : 0;
                try (Connection conn = dataSource.getConnection()) {
                    if (conn.isValid(1)) {
                        status = ServerStatus.ONLINE;
                        this.connected = true;
                        this.lastError = "";
                    } else {
                        status = ServerStatus.DEGRADED;
                    }
                }
            } catch (Exception e) {
                status = ServerStatus.UNREACHABLE;
                this.connected = false;
                this.lastError = e.getMessage();
            }
        } else {
            status = ServerStatus.UNREACHABLE;
            this.connected = false;
        }

        this.lastCheckedAt = Instant.now();
        return new DatabaseHealthDTO(
                config.getDatabaseName(),
                this.connected,
                status,
                active,
                idle,
                total,
                this.lastError,
                this.lastCheckedAt
        );
    }

    @Override
    public synchronized void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            log.info("Closing database connection pool...");
            dataSource.close();
            this.connected = false;
        }
    }
}
