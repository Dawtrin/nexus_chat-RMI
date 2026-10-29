package com.nexus.chat.contract.dto;

import com.nexus.chat.contract.enums.ServerStatus;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Database health metrics without exposing passwords or sensitive connection strings.
 */
public class DatabaseHealthDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String databaseName;
    private final boolean connected;
    private final ServerStatus status;
    private final int activeConnections;
    private final int idleConnections;
    private final int totalConnections;
    private final String lastError;
    private final Instant lastCheckedAt;
    private final String persistenceMode;

    public DatabaseHealthDTO(String databaseName,
                             boolean connected,
                             ServerStatus status,
                             int activeConnections,
                             int idleConnections,
                             int totalConnections,
                             String lastError,
                             Instant lastCheckedAt) {
        this(databaseName, connected, status, activeConnections, idleConnections, totalConnections, lastError, lastCheckedAt,
                connected ? "MYSQL_DURABLE" : "IN_MEMORY_FALLBACK");
    }

    public DatabaseHealthDTO(String databaseName,
                             boolean connected,
                             ServerStatus status,
                             int activeConnections,
                             int idleConnections,
                             int totalConnections,
                             String lastError,
                             Instant lastCheckedAt,
                             String persistenceMode) {
        this.databaseName = Objects.requireNonNull(databaseName, "databaseName cannot be null");
        this.connected = connected;
        this.status = status != null ? status : ServerStatus.UNKNOWN;
        this.activeConnections = activeConnections;
        this.idleConnections = idleConnections;
        this.totalConnections = totalConnections;
        this.lastError = lastError != null ? lastError : "";
        this.lastCheckedAt = lastCheckedAt != null ? lastCheckedAt : Instant.now();
        this.persistenceMode = persistenceMode != null ? persistenceMode : (connected ? "MYSQL_DURABLE" : "IN_MEMORY_FALLBACK");
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public boolean isConnected() {
        return connected;
    }

    public ServerStatus getStatus() {
        return status;
    }

    public int getActiveConnections() {
        return activeConnections;
    }

    public int getIdleConnections() {
        return idleConnections;
    }

    public int getTotalConnections() {
        return totalConnections;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getLastCheckedAt() {
        return lastCheckedAt;
    }

    public String getPersistenceMode() {
        return persistenceMode;
    }

    @Override
    public String toString() {
        return "DatabaseHealthDTO{" +
                "databaseName='" + databaseName + '\'' +
                ", connected=" + connected +
                ", status=" + status +
                ", persistenceMode='" + persistenceMode + '\'' +
                ", active=" + activeConnections +
                ", idle=" + idleConnections +
                '}';
    }
}
