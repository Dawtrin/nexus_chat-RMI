package com.nexus.chat.desktop.controller;

import com.nexus.chat.contract.dto.DatabaseHealthDTO;
import com.nexus.chat.contract.dto.ServerStatusDTO;
import com.nexus.chat.contract.enums.EventSeverity;
import com.nexus.chat.contract.enums.SessionStatus;
import com.nexus.chat.desktop.config.ClientConfig;
import com.nexus.chat.desktop.model.EventViewModel;
import com.nexus.chat.desktop.model.SessionViewModel;
import com.nexus.chat.desktop.navigation.ViewId;
import com.nexus.chat.desktop.navigation.ViewRouter;
import com.nexus.chat.desktop.service.AdminMonitoringService;
import com.nexus.chat.desktop.service.ChatClientService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Controller for the NEXUS CONTROL administrative monitoring dashboard.
 */
public class AdminController {
    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final ChatClientService chatService;
    private final AdminMonitoringService adminService;
    private final ViewRouter router;

    @FXML private TextField adminHostField;
    @FXML private TextField adminPortField;
    @FXML private PasswordField adminTokenField;
    @FXML private Button refreshButton;
    @FXML private CheckBox autoRefreshCheck;
    @FXML private Label adminStatusLabel;

    // KPI Cards
    @FXML private Label kpiServerId;
    @FXML private Label kpiServerStatus;
    @FXML private Label kpiPeerStatus;
    @FXML private Label kpiPeerEndpoint;
    @FXML private Label kpiDbStatus;
    @FXML private Label kpiDbName;
    @FXML private Label kpiActiveSessions;
    @FXML private Label kpiUptime;

    // Tables
    @FXML private TableView<SessionViewModel> sessionTable;
    @FXML private TableColumn<SessionViewModel, String> colSessionIp;
    @FXML private TableColumn<SessionViewModel, String> colSessionUser;
    @FXML private TableColumn<SessionViewModel, String> colSessionId;
    @FXML private TableColumn<SessionViewModel, SessionStatus> colSessionStatus;
    @FXML private TableColumn<SessionViewModel, String> colSessionConnected;
    @FXML private TableColumn<SessionViewModel, String> colSessionLastSeen;

    @FXML private TableView<EventViewModel> eventTable;
    @FXML private TableColumn<EventViewModel, String> colEventTime;
    @FXML private TableColumn<EventViewModel, EventSeverity> colEventSeverity;
    @FXML private TableColumn<EventViewModel, String> colEventComponent;
    @FXML private TableColumn<EventViewModel, String> colEventMessage;

    private ScheduledExecutorService autoRefreshExecutor;

    public AdminController(ChatClientService chatService, AdminMonitoringService adminService, ViewRouter router) {
        this.chatService = chatService;
        this.adminService = adminService;
        this.router = router;
    }

    @FXML
    public void initialize() {
        // Defaults
        adminHostField.setText(ClientConfig.DEFAULT_SERVER_A_HOST);
        adminPortField.setText(String.valueOf(ClientConfig.DEFAULT_SERVER_A_PORT));
        adminTokenField.setText("nexus-admin-secret");

        adminStatusLabel.textProperty().bind(adminService.statusMessageProperty());

        // Setup session table columns
        colSessionIp.setCellValueFactory(data -> data.getValue().remoteIpProperty());
        colSessionUser.setCellValueFactory(data -> data.getValue().usernameProperty());
        colSessionId.setCellValueFactory(data -> data.getValue().sessionIdProperty());
        colSessionStatus.setCellValueFactory(data -> data.getValue().statusProperty());
        colSessionConnected.setCellValueFactory(data -> data.getValue().connectedTimeProperty());
        colSessionLastSeen.setCellValueFactory(data -> data.getValue().lastSeenTimeProperty());

        sessionTable.setItems(adminService.getSessions());
        sessionTable.setPlaceholder(new Label("No client sessions monitored. Connect with valid admin authorization to inspect observed IPs."));

        // Setup events table columns
        colEventTime.setCellValueFactory(data -> data.getValue().timestampProperty());
        colEventSeverity.setCellValueFactory(data -> data.getValue().severityProperty());
        colEventComponent.setCellValueFactory(data -> data.getValue().componentProperty());
        colEventMessage.setCellValueFactory(data -> data.getValue().messageProperty());

        eventTable.setItems(adminService.getEvents());
        eventTable.setPlaceholder(new Label("No server events monitored. Connect to monitoring endpoint to inspect logs."));

        // Bind KPI labels to live telemetry
        adminService.serverStatusProperty().addListener((obs, oldV, newV) -> updateServerKpis(newV));
        adminService.databaseHealthProperty().addListener((obs, oldV, newV) -> updateDatabaseKpis(newV));

        // Auto-refresh handler
        autoRefreshCheck.selectedProperty().addListener((obs, oldV, isSelected) -> {
            if (isSelected) {
                startAutoRefresh();
            } else {
                stopAutoRefresh();
            }
        });

        // Initialize honest default empty state
        updateServerKpis(null);
        updateDatabaseKpis(null);
    }

    private void updateServerKpis(ServerStatusDTO status) {
        if (status == null) {
            kpiServerId.setText("—");
            kpiServerStatus.setText("NOT CONNECTED");
            kpiServerStatus.setStyle("-fx-text-fill: -color-text-muted;");
            kpiPeerStatus.setText("UNKNOWN");
            kpiPeerEndpoint.setText("—");
            kpiActiveSessions.setText("0");
            kpiUptime.setText("—");
        } else {
            kpiServerId.setText(status.getServerId().toUpperCase());
            kpiServerStatus.setText(status.getServerStatus().name());
            kpiServerStatus.setStyle(status.getServerStatus().name().equals("ONLINE") ? "-fx-text-fill: -color-success;" : "-fx-text-fill: -color-warning;");

            kpiPeerStatus.setText(status.getPeerStatus().name());
            kpiPeerEndpoint.setText(status.getAdvertisedRmiHost() + ":" + status.getRegistryPort());
            kpiActiveSessions.setText(String.valueOf(status.getActiveSessionCount()));
            kpiUptime.setText(formatUptime(status.getUptimeSeconds()));
        }
    }

    private void updateDatabaseKpis(DatabaseHealthDTO db) {
        if (db == null) {
            kpiDbStatus.setText("NOT MONITORED");
            kpiDbName.setText("—");
        } else {
            kpiDbStatus.setText(db.isConnected() ? "CONNECTED (MySQL)" : "IN-MEMORY FALLBACK");
            kpiDbName.setText(db.getDatabaseName() + " (" + db.getActiveConnections() + " active)");
        }
    }

    private String formatUptime(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        return String.format("%02dh %02dm %02ds", h, m, s);
    }

    @FXML
    public void onRefreshClicked() {
        String host = adminHostField.getText().trim();
        int port;
        try {
            port = Integer.parseInt(adminPortField.getText().trim());
        } catch (NumberFormatException e) {
            adminService.statusMessageProperty().set("Invalid port number");
            return;
        }

        String token = adminTokenField.getText();
        refreshButton.setDisable(true);

        adminService.fetchDiagnostics(host, port, token, success -> {
            Platform.runLater(() -> refreshButton.setDisable(false));
        });
    }

    private void startAutoRefresh() {
        stopAutoRefresh();
        autoRefreshExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "nexus-admin-autorefresh");
            t.setDaemon(true);
            return t;
        });

        autoRefreshExecutor.scheduleAtFixedRate(this::onRefreshClicked, 3, 3, TimeUnit.SECONDS);
    }

    private void stopAutoRefresh() {
        if (autoRefreshExecutor != null && !autoRefreshExecutor.isShutdown()) {
            autoRefreshExecutor.shutdownNow();
            autoRefreshExecutor = null;
        }
    }

    @FXML
    public void onBackToChatClicked() {
        stopAutoRefresh();
        if (chatService.isConnected()) {
            router.navigateTo(ViewId.CHAT);
        } else {
            router.navigateTo(ViewId.LOGIN);
        }
    }
}
