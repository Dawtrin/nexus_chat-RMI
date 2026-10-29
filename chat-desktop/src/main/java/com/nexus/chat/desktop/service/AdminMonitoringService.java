package com.nexus.chat.desktop.service;

import com.nexus.chat.contract.dto.ClientSessionDTO;
import com.nexus.chat.contract.dto.DatabaseHealthDTO;
import com.nexus.chat.contract.dto.ServerEventDTO;
import com.nexus.chat.contract.dto.ServerStatusDTO;
import com.nexus.chat.contract.remote.MonitoringRemote;
import com.nexus.chat.desktop.model.EventViewModel;
import com.nexus.chat.desktop.model.SessionViewModel;
import com.nexus.chat.desktop.rmi.RmiClientManager;
import com.nexus.chat.desktop.util.FxAsync;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.function.Consumer;

/**
 * Service providing asynchronous telemetry data from the server's MonitoringRemote API.
 */
public class AdminMonitoringService {
    private static final Logger log = LoggerFactory.getLogger(AdminMonitoringService.class);

    private final ObjectProperty<ServerStatusDTO> serverStatus = new SimpleObjectProperty<>();
    private final ObjectProperty<DatabaseHealthDTO> databaseHealth = new SimpleObjectProperty<>();
    private final ObservableList<SessionViewModel> sessions = FXCollections.observableArrayList();
    private final ObservableList<EventViewModel> events = FXCollections.observableArrayList();
    private final BooleanProperty connected = new BooleanPropertyBase(false) {
        @Override
        public Object getBean() { return AdminMonitoringService.this; }
        @Override
        public String getName() { return "connected"; }
    };
    private final StringProperty statusMessage = new SimpleStringProperty("Not connected to monitoring endpoint");

    public void fetchDiagnostics(String host, int port, String adminToken, Consumer<Boolean> onComplete) {
        String queryingMsg = "Querying monitoring service on " + host + ":" + port + "...";
        if (javafx.application.Platform.isFxApplicationThread()) {
            statusMessage.set(queryingMsg);
        } else {
            javafx.application.Platform.runLater(() -> statusMessage.set(queryingMsg));
        }

        FxAsync.execute(() -> {
            Registry registry = LocateRegistry.getRegistry(host, port);
            MonitoringRemote monitoring = (MonitoringRemote) registry.lookup(RmiClientManager.MONITORING_SERVICE_NAME);

            ServerStatusDTO status = monitoring.getServerStatus();
            List<ClientSessionDTO> sessionList = monitoring.getActiveSessions(adminToken);
            List<ServerEventDTO> eventList = monitoring.getRecentEvents(adminToken, 100);
            DatabaseHealthDTO dbHealth = monitoring.getDatabaseHealth(adminToken);

            return new DiagnosticsPayload(status, sessionList, eventList, dbHealth);
        }, payload -> {
            this.serverStatus.set(payload.status);
            this.databaseHealth.set(payload.dbHealth);

            this.sessions.clear();
            for (ClientSessionDTO s : payload.sessions) {
                this.sessions.add(new SessionViewModel(s));
            }

            this.events.clear();
            for (ServerEventDTO e : payload.events) {
                this.events.add(new EventViewModel(e));
            }

            this.connected.set(true);
            this.statusMessage.set(String.format("Live telemetry updated from %s (%s:%d)",
                    payload.status.getServerId(), host, port));

            if (onComplete != null) onComplete.accept(true);
        }, error -> {
            this.connected.set(false);
            this.serverStatus.set(null);
            this.databaseHealth.set(null);
            this.sessions.clear();
            this.events.clear();
            this.statusMessage.set("Diagnostics failed: " + error.getMessage());
            log.warn("Diagnostics query failed: {}", error.getMessage());

            if (onComplete != null) onComplete.accept(false);
        });
    }

    public ObjectProperty<ServerStatusDTO> serverStatusProperty() { return serverStatus; }
    public ObjectProperty<DatabaseHealthDTO> databaseHealthProperty() { return databaseHealth; }
    public ObservableList<SessionViewModel> getSessions() { return sessions; }
    public ObservableList<EventViewModel> getEvents() { return events; }
    public BooleanProperty connectedProperty() { return connected; }
    public StringProperty statusMessageProperty() { return statusMessage; }

    private record DiagnosticsPayload(ServerStatusDTO status,
                                      List<ClientSessionDTO> sessions,
                                      List<ServerEventDTO> events,
                                      DatabaseHealthDTO dbHealth) {}
}
