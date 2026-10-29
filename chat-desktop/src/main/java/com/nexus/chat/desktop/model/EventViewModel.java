package com.nexus.chat.desktop.model;

import com.nexus.chat.contract.dto.ServerEventDTO;
import com.nexus.chat.contract.enums.EventSeverity;
import javafx.beans.property.*;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Model representing a server operational event row in the Admin Dashboard logs table.
 */
public class EventViewModel {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
            .withZone(ZoneId.systemDefault());

    private final StringProperty eventId = new SimpleStringProperty();
    private final StringProperty timestamp = new SimpleStringProperty();
    private final ObjectProperty<EventSeverity> severity = new SimpleObjectProperty<>();
    private final StringProperty component = new SimpleStringProperty();
    private final StringProperty message = new SimpleStringProperty();
    private final StringProperty serverId = new SimpleStringProperty();

    public EventViewModel(ServerEventDTO dto) {
        this.eventId.set(dto.getEventId());
        this.timestamp.set(dto.getTimestamp() != null ? FORMATTER.format(dto.getTimestamp()) : "-");
        this.severity.set(dto.getSeverity());
        this.component.set(dto.getSourceComponent());
        this.message.set(dto.getMessage());
        this.serverId.set(dto.getServerId());
    }

    public String getEventId() { return eventId.get(); }
    public StringProperty eventIdProperty() { return eventId; }

    public String getTimestamp() { return timestamp.get(); }
    public StringProperty timestampProperty() { return timestamp; }

    public EventSeverity getSeverity() { return severity.get(); }
    public ObjectProperty<EventSeverity> severityProperty() { return severity; }

    public String getComponent() { return component.get(); }
    public StringProperty componentProperty() { return component; }

    public String getMessage() { return message.get(); }
    public StringProperty messageProperty() { return message; }

    public String getServerId() { return serverId.get(); }
    public StringProperty serverIdProperty() { return serverId; }
}
