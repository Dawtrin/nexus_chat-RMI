package com.nexus.chat.contract.dto;

import com.nexus.chat.contract.enums.EventSeverity;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Diagnostic or operational event record for administrator monitoring.
 */
public class ServerEventDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String eventId;
    private final Instant timestamp;
    private final EventSeverity severity;
    private final String sourceComponent;
    private final String message;
    private final String serverId;

    public ServerEventDTO(String eventId,
                          Instant timestamp,
                          EventSeverity severity,
                          String sourceComponent,
                          String message,
                          String serverId) {
        this.eventId = Objects.requireNonNull(eventId, "eventId cannot be null");
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.severity = severity != null ? severity : EventSeverity.INFO;
        this.sourceComponent = Objects.requireNonNull(sourceComponent, "sourceComponent cannot be null");
        this.message = Objects.requireNonNull(message, "message cannot be null");
        this.serverId = Objects.requireNonNull(serverId, "serverId cannot be null");
    }

    public String getEventId() {
        return eventId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public EventSeverity getSeverity() {
        return severity;
    }

    public String getSourceComponent() {
        return sourceComponent;
    }

    public String getMessage() {
        return message;
    }

    public String getServerId() {
        return serverId;
    }

    @Override
    public String toString() {
        return "ServerEventDTO{" +
                "timestamp=" + timestamp +
                ", severity=" + severity +
                ", sourceComponent='" + sourceComponent + '\'' +
                ", message='" + message + '\'' +
                ", serverId='" + serverId + '\'' +
                '}';
    }
}
