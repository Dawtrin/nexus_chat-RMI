package com.nexus.chat.contract.dto;

import com.nexus.chat.contract.enums.ServerStatus;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Observed operational health status of a server node in the cluster.
 */
public class ServerStatusDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String serverId;
    private final String hostname;
    private final String configuredAddress;
    private final String bindAddress;
    private final String advertisedRmiHost;
    private final int registryPort;
    private final int servicePort;
    private final ServerStatus serverStatus;
    private final ServerStatus peerStatus;
    private final ServerStatus databaseStatus;
    private final long uptimeSeconds;
    private final Instant startedAt;
    private final int activeSessionCount;
    private final int pendingReplicationCount;
    private final Instant lastReplicationTimestamp;
    private final String persistenceMode;

    public ServerStatusDTO(String serverId,
                           String hostname,
                           String configuredAddress,
                           String bindAddress,
                           String advertisedRmiHost,
                           int registryPort,
                           int servicePort,
                           ServerStatus serverStatus,
                           ServerStatus peerStatus,
                           ServerStatus databaseStatus,
                           long uptimeSeconds,
                           Instant startedAt,
                           int activeSessionCount) {
        this(serverId, hostname, configuredAddress, bindAddress, advertisedRmiHost, registryPort, servicePort,
                serverStatus, peerStatus, databaseStatus, uptimeSeconds, startedAt, activeSessionCount, 0, null, "IN_MEMORY_FALLBACK");
    }

    public ServerStatusDTO(String serverId,
                           String hostname,
                           String configuredAddress,
                           String bindAddress,
                           String advertisedRmiHost,
                           int registryPort,
                           int servicePort,
                           ServerStatus serverStatus,
                           ServerStatus peerStatus,
                           ServerStatus databaseStatus,
                           long uptimeSeconds,
                           Instant startedAt,
                           int activeSessionCount,
                           int pendingReplicationCount,
                           Instant lastReplicationTimestamp,
                           String persistenceMode) {
        this.serverId = Objects.requireNonNull(serverId, "serverId cannot be null");
        this.hostname = hostname != null ? hostname : "UNKNOWN";
        this.configuredAddress = configuredAddress != null ? configuredAddress : "UNKNOWN";
        this.bindAddress = bindAddress != null ? bindAddress : "UNKNOWN";
        this.advertisedRmiHost = advertisedRmiHost != null ? advertisedRmiHost : "UNKNOWN";
        this.registryPort = registryPort;
        this.servicePort = servicePort;
        this.serverStatus = serverStatus != null ? serverStatus : ServerStatus.UNKNOWN;
        this.peerStatus = peerStatus != null ? peerStatus : ServerStatus.UNKNOWN;
        this.databaseStatus = databaseStatus != null ? databaseStatus : ServerStatus.UNKNOWN;
        this.uptimeSeconds = uptimeSeconds;
        this.startedAt = startedAt != null ? startedAt : Instant.now();
        this.activeSessionCount = activeSessionCount;
        this.pendingReplicationCount = pendingReplicationCount;
        this.lastReplicationTimestamp = lastReplicationTimestamp;
        this.persistenceMode = persistenceMode != null ? persistenceMode : "IN_MEMORY_FALLBACK";
    }

    public String getServerId() {
        return serverId;
    }

    public String getHostname() {
        return hostname;
    }

    public String getConfiguredAddress() {
        return configuredAddress;
    }

    public String getBindAddress() {
        return bindAddress;
    }

    public String getAdvertisedRmiHost() {
        return advertisedRmiHost;
    }

    public int getRegistryPort() {
        return registryPort;
    }

    public int getServicePort() {
        return servicePort;
    }

    public ServerStatus getServerStatus() {
        return serverStatus;
    }

    public ServerStatus getPeerStatus() {
        return peerStatus;
    }

    public ServerStatus getDatabaseStatus() {
        return databaseStatus;
    }

    public long getUptimeSeconds() {
        return uptimeSeconds;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public int getActiveSessionCount() {
        return activeSessionCount;
    }

    public int getPendingReplicationCount() {
        return pendingReplicationCount;
    }

    public Instant getLastReplicationTimestamp() {
        return lastReplicationTimestamp;
    }

    public String getPersistenceMode() {
        return persistenceMode;
    }

    @Override
    public String toString() {
        return "ServerStatusDTO{" +
                "serverId='" + serverId + '\'' +
                ", serverStatus=" + serverStatus +
                ", peerStatus=" + peerStatus +
                ", databaseStatus=" + databaseStatus +
                ", persistenceMode='" + persistenceMode + '\'' +
                ", activeSessions=" + activeSessionCount +
                ", pendingReplication=" + pendingReplicationCount +
                ", uptime=" + uptimeSeconds + "s" +
                '}';
    }
}
