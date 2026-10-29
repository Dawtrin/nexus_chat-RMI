package com.nexus.chat.contract.remote;

import com.nexus.chat.contract.dto.ClientSessionDTO;
import com.nexus.chat.contract.dto.DatabaseHealthDTO;
import com.nexus.chat.contract.dto.ServerEventDTO;
import com.nexus.chat.contract.dto.ServerStatusDTO;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Administrative RMI interface for cluster monitoring, session inspection, and health telemetry.
 */
public interface MonitoringRemote extends Remote {

    /**
     * Retrieves high-level server status, uptime, and cluster link health.
     *
     * @return ServerStatusDTO
     * @throws RemoteException on network or RMI failure
     */
    ServerStatusDTO getServerStatus() throws RemoteException;

    /**
     * Retrieves active and recent client sessions, including observed remote IP addresses.
     * Requires administrative authorization.
     *
     * @param adminToken Configured administrator authorization token
     * @return List of client sessions
     * @throws RemoteException on network or authorization failure
     */
    List<ClientSessionDTO> getActiveSessions(String adminToken) throws RemoteException;

    /**
     * Retrieves recent server operational logs and events.
     * Requires administrative authorization.
     *
     * @param adminToken Configured administrator authorization token
     * @param limit Maximum number of recent events to retrieve
     * @return List of server events
     * @throws RemoteException on network or authorization failure
     */
    List<ServerEventDTO> getRecentEvents(String adminToken, int limit) throws RemoteException;

    /**
     * Retrieves database connectivity and connection pool metrics.
     * Requires administrative authorization.
     *
     * @param adminToken Configured administrator authorization token
     * @return DatabaseHealthDTO
     * @throws RemoteException on network or authorization failure
     */
    DatabaseHealthDTO getDatabaseHealth(String adminToken) throws RemoteException;
}
