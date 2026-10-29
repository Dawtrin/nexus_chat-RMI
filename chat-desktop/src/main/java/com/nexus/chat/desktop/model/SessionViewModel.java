package com.nexus.chat.desktop.model;

import com.nexus.chat.contract.dto.ClientSessionDTO;
import com.nexus.chat.contract.enums.SessionStatus;
import javafx.beans.property.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Model representing a client session row in the Admin Dashboard table.
 */
public class SessionViewModel {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final StringProperty sessionId = new SimpleStringProperty();
    private final StringProperty username = new SimpleStringProperty();
    private final StringProperty remoteIp = new SimpleStringProperty();
    private final ObjectProperty<SessionStatus> status = new SimpleObjectProperty<>();
    private final StringProperty connectedServer = new SimpleStringProperty();
    private final StringProperty connectedTime = new SimpleStringProperty();
    private final StringProperty lastSeenTime = new SimpleStringProperty();

    public SessionViewModel(ClientSessionDTO dto) {
        this.sessionId.set(dto.getSessionId());
        this.username.set(dto.getUsername());
        this.remoteIp.set(dto.getRemoteIp());
        this.status.set(dto.getStatus());
        this.connectedServer.set(dto.getConnectedServerId());
        this.connectedTime.set(dto.getConnectedAt() != null ? FORMATTER.format(dto.getConnectedAt()) : "-");
        this.lastSeenTime.set(dto.getLastSeenAt() != null ? FORMATTER.format(dto.getLastSeenAt()) : "-");
    }

    public String getSessionId() { return sessionId.get(); }
    public StringProperty sessionIdProperty() { return sessionId; }

    public String getUsername() { return username.get(); }
    public StringProperty usernameProperty() { return username; }

    public String getRemoteIp() { return remoteIp.get(); }
    public StringProperty remoteIpProperty() { return remoteIp; }

    public SessionStatus getStatus() { return status.get(); }
    public ObjectProperty<SessionStatus> statusProperty() { return status; }

    public String getConnectedServer() { return connectedServer.get(); }
    public StringProperty connectedServerProperty() { return connectedServer; }

    public String getConnectedTime() { return connectedTime.get(); }
    public StringProperty connectedTimeProperty() { return connectedTime; }

    public String getLastSeenTime() { return lastSeenTime.get(); }
    public StringProperty lastSeenTimeProperty() { return lastSeenTime; }
}
