package com.nexus.chat.server.service;

import com.nexus.chat.contract.dto.ClientSessionDTO;
import com.nexus.chat.contract.dto.DatabaseHealthDTO;
import com.nexus.chat.contract.dto.ServerEventDTO;
import com.nexus.chat.contract.dto.ServerStatusDTO;
import com.nexus.chat.contract.enums.ServerStatus;
import com.nexus.chat.contract.remote.MonitoringRemote;
import com.nexus.chat.server.config.ServerConfig;
import com.nexus.chat.server.database.DatabaseManager;
import com.nexus.chat.server.monitoring.ServerMetrics;
import com.nexus.chat.server.monitoring.SystemLogService;
import com.nexus.chat.server.repository.MessageRepository;

import java.net.InetAddress;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Implementation of the administrative MonitoringRemote RMI interface
 * providing authentic cluster telemetry and health observations.
 */
public class MonitoringServiceImpl implements MonitoringRemote {

    private final ServerConfig config;
    private final DatabaseManager databaseManager;
    private final MessageRepository messageRepository;
    private final ClientSessionManager sessionManager;
    private final PeerReplicationManager replicationManager;
    private final ServerMetrics metrics;
    private final SystemLogService logService;

    public MonitoringServiceImpl(ServerConfig config,
                                 DatabaseManager databaseManager,
                                 MessageRepository messageRepository,
                                 ClientSessionManager sessionManager,
                                 PeerReplicationManager replicationManager,
                                 ServerMetrics metrics,
                                 SystemLogService logService) {
        this.config = config;
        this.databaseManager = databaseManager;
        this.messageRepository = messageRepository;
        this.sessionManager = sessionManager;
        this.replicationManager = replicationManager;
        this.metrics = metrics;
        this.logService = logService;
    }

    private void validateAdminToken(String adminToken) throws RemoteException {
        if (adminToken == null || !adminToken.equals(config.getAdminToken())) {
            throw new RemoteException("Access denied: Invalid administrator token");
        }
    }

    @Override
    public ServerStatusDTO getServerStatus() throws RemoteException {
        String hostname = "UNKNOWN";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {
        }

        ServerStatus dbStatus = databaseManager.checkHealth().getStatus();
        ServerStatus peerStatus = replicationManager.getPeerStatus();

        return new ServerStatusDTO(
                config.getServerId(),
                hostname,
                config.getAdvertisedHost(),
                "0.0.0.0",
                config.getAdvertisedHost(),
                config.getRmiRegistryPort(),
                config.getRmiServicePort(),
                ServerStatus.ONLINE,
                peerStatus,
                dbStatus,
                metrics.getUptimeSeconds(),
                metrics.getStartedAt(),
                sessionManager.getActiveSessionCount(),
                replicationManager.getPendingOutboxCount(),
                replicationManager.getLastSuccessfulReplication(),
                messageRepository.getPersistenceMode()
        );
    }

    @Override
    public List<ClientSessionDTO> getActiveSessions(String adminToken) throws RemoteException {
        validateAdminToken(adminToken);
        return sessionManager.getAllSessions();
    }

    @Override
    public List<ServerEventDTO> getRecentEvents(String adminToken, int limit) throws RemoteException {
        validateAdminToken(adminToken);
        return logService.getRecentEvents(limit);
    }

    @Override
    public DatabaseHealthDTO getDatabaseHealth(String adminToken) throws RemoteException {
        validateAdminToken(adminToken);
        return databaseManager.checkHealth();
    }
}
