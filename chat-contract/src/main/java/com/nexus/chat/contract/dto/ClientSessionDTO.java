package com.nexus.chat.contract.dto;

import com.nexus.chat.contract.enums.SessionStatus;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Server-tracked client session representing an active or recent desktop connection.
 */
public class ClientSessionDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String sessionId;
    private final String username;
    private final String clientInstanceId;
    private final String remoteIp;
    private final int remotePort;
    private final Instant connectedAt;
    private final Instant lastSeenAt;
    private final SessionStatus status;
    private final String connectedServerId;

    public ClientSessionDTO(String sessionId,
                            String username,
                            String clientInstanceId,
                            String remoteIp,
                            int remotePort,
                            Instant connectedAt,
                            Instant lastSeenAt,
                            SessionStatus status,
                            String connectedServerId) {
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId cannot be null");
        this.username = Objects.requireNonNull(username, "username cannot be null");
        this.clientInstanceId = clientInstanceId != null ? clientInstanceId : "";
        this.remoteIp = remoteIp != null ? remoteIp : "UNKNOWN";
        this.remotePort = remotePort;
        this.connectedAt = connectedAt != null ? connectedAt : Instant.now();
        this.lastSeenAt = lastSeenAt != null ? lastSeenAt : Instant.now();
        this.status = status != null ? status : SessionStatus.ACTIVE;
        this.connectedServerId = connectedServerId != null ? connectedServerId : "UNKNOWN";
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getUsername() {
        return username;
    }

    public String getClientInstanceId() {
        return clientInstanceId;
    }

    public String getRemoteIp() {
        return remoteIp;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public Instant getConnectedAt() {
        return connectedAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public String getConnectedServerId() {
        return connectedServerId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientSessionDTO that = (ClientSessionDTO) o;
        return Objects.equals(sessionId, that.sessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId);
    }

    @Override
    public String toString() {
        return "ClientSessionDTO{" +
                "sessionId='" + sessionId + '\'' +
                ", username='" + username + '\'' +
                ", remoteIp='" + remoteIp + '\'' +
                ", remotePort=" + remotePort +
                ", status=" + status +
                ", connectedServerId='" + connectedServerId + '\'' +
                '}';
    }
}
