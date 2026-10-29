package com.nexus.chat.desktop.controller;

import com.nexus.chat.desktop.config.ClientConfig;
import com.nexus.chat.desktop.navigation.ViewId;
import com.nexus.chat.desktop.navigation.ViewRouter;
import com.nexus.chat.desktop.service.ChatClientService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for the initial welcome, server connection, and username login screen.
 */
public class LoginController {
    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    private final ChatClientService chatService;
    private final ViewRouter router;

    @FXML private ComboBox<String> serverProfileCombo;
    @FXML private TextField hostField;
    @FXML private TextField portField;
    @FXML private TextField usernameField;
    @FXML private Button connectButton;
    @FXML private Button openAdminButton;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator progressIndicator;

    public LoginController(ChatClientService chatService, ViewRouter router) {
        this.chatService = chatService;
        this.router = router;
    }

    @FXML
    public void initialize() {
        serverProfileCombo.getItems().addAll(
                "Server A (Local Node A: 1099)",
                "Server B (Local Node B: 2099)",
                "Custom Network Endpoint..."
        );
        serverProfileCombo.getSelectionModel().select(0);
        updateFieldsFromProfile(0);

        serverProfileCombo.getSelectionModel().selectedIndexProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                updateFieldsFromProfile(newV.intValue());
            }
        });

        progressIndicator.setVisible(false);
        progressIndicator.managedProperty().bind(progressIndicator.visibleProperty());

        statusLabel.textProperty().bind(chatService.statusMessageProperty());
    }

    private void updateFieldsFromProfile(int index) {
        switch (index) {
            case 0 -> {
                hostField.setText(ClientConfig.DEFAULT_SERVER_A_HOST);
                portField.setText(String.valueOf(ClientConfig.DEFAULT_SERVER_A_PORT));
                hostField.setDisable(true);
                portField.setDisable(true);
            }
            case 1 -> {
                hostField.setText(ClientConfig.DEFAULT_SERVER_B_HOST);
                portField.setText(String.valueOf(ClientConfig.DEFAULT_SERVER_B_PORT));
                hostField.setDisable(true);
                portField.setDisable(true);
            }
            case 2 -> {
                hostField.setDisable(false);
                portField.setDisable(false);
            }
        }
    }

    @FXML
    public void onConnectClicked() {
        String username = usernameField.getText();
        if (username == null || username.trim().isEmpty()) {
            chatService.statusMessageProperty().set("Please enter a username or display name");
            usernameField.requestFocus();
            return;
        }

        String host = hostField.getText().trim();
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            chatService.statusMessageProperty().set("Invalid port number");
            portField.requestFocus();
            return;
        }

        connectButton.setDisable(true);
        progressIndicator.setVisible(true);

        chatService.connectAndRegister(host, port, username.trim(), result -> {
            Platform.runLater(() -> {
                connectButton.setDisable(false);
                progressIndicator.setVisible(false);

                if (result.isSuccess()) {
                    log.info("Client connected successfully! Navigating to chat view...");
                    router.navigateTo(ViewId.CHAT);
                }
            });
        });
    }

    @FXML
    public void onAdminClicked() {
        router.navigateTo(ViewId.ADMIN);
    }
}
