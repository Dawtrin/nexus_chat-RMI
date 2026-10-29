package com.nexus.chat.desktop.controller;

import com.nexus.chat.desktop.navigation.ViewId;
import com.nexus.chat.desktop.navigation.ViewRouter;
import com.nexus.chat.desktop.service.ChatClientService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * Controller for application preferences and distributed system information.
 */
public class SettingsController {

    private final ChatClientService chatService;
    private final ViewRouter router;

    @FXML private Label currentServerLabel;
    @FXML private Label currentSessionLabel;
    @FXML private Label currentUsernameLabel;

    public SettingsController(ChatClientService chatService, ViewRouter router) {
        this.chatService = chatService;
        this.router = router;
    }

    @FXML
    public void initialize() {
        currentServerLabel.textProperty().bind(chatService.currentServerIdProperty());
        currentSessionLabel.textProperty().bind(chatService.currentRoomIdProperty());
        currentUsernameLabel.textProperty().bind(chatService.currentUsernameProperty());
    }

    @FXML
    public void onBackClicked() {
        if (chatService.isConnected()) {
            router.navigateTo(ViewId.CHAT);
        } else {
            router.navigateTo(ViewId.LOGIN);
        }
    }
}
