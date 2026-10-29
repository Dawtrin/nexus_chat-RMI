package com.nexus.chat.desktop.controller;

import com.nexus.chat.desktop.model.ChatMessageModel;
import com.nexus.chat.desktop.model.ChatRoomModel;
import com.nexus.chat.desktop.model.ConnectionState;
import com.nexus.chat.desktop.navigation.ViewId;
import com.nexus.chat.desktop.navigation.ViewRouter;
import com.nexus.chat.desktop.service.ChatClientService;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for the main messaging workspace, room browsing, and timeline interaction.
 */
public class ChatController {
    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatClientService chatService;
    private final ViewRouter router;

    @FXML private Label serverStatusBadge;
    @FXML private Label currentUserLabel;
    @FXML private ListView<ChatRoomModel> roomListView;
    @FXML private Label currentRoomTitle;
    @FXML private Label currentRoomDesc;
    @FXML private ListView<ChatMessageModel> messageListView;
    @FXML private VBox emptyStateBox;
    @FXML private TextArea messageInput;
    @FXML private Button sendButton;
    @FXML private Label charCounterLabel;

    public ChatController(ChatClientService chatService, ViewRouter router) {
        this.chatService = chatService;
        this.router = router;
    }

    @FXML
    public void initialize() {
        // Bind user and server labels
        currentUserLabel.textProperty().bind(Bindings.concat("@", chatService.currentUsernameProperty()));
        serverStatusBadge.textProperty().bind(Bindings.concat("CONNECTED · ", chatService.currentServerIdProperty()));

        // Setup room list
        roomListView.setItems(chatService.getRooms());
        roomListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ChatRoomModel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    getStyleClass().remove("room-list-cell");
                } else {
                    setText("# " + item.getRoomName());
                    getStyleClass().add("room-list-cell");
                }
            }
        });

        roomListView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                currentRoomTitle.setText("# " + newV.getRoomName());
                currentRoomDesc.setText(newV.getDescription());
                chatService.joinRoom(newV.getRoomId());
            }
        });

        // Setup message list view
        messageListView.setItems(chatService.getMessages());
        messageListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ChatMessageModel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setGraphic(createMessageBubble(item));
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                }
            }
        });

        // Toggle empty state visibility
        emptyStateBox.visibleProperty().bind(Bindings.isEmpty(chatService.getMessages()));
        emptyStateBox.managedProperty().bind(emptyStateBox.visibleProperty());

        // Auto-scroll when new messages arrive
        chatService.getMessages().addListener((javafx.collections.ListChangeListener<ChatMessageModel>) c -> {
            while (c.next()) {
                if (c.wasAdded() && !chatService.getMessages().isEmpty()) {
                    Platform.runLater(() -> messageListView.scrollTo(chatService.getMessages().size() - 1));
                }
            }
        });

        // Character counter and input validation
        messageInput.textProperty().addListener((obs, oldV, newV) -> {
            int len = newV != null ? newV.length() : 0;
            charCounterLabel.setText(len + "/4000");
            sendButton.setDisable(len == 0 || len > 4000);
        });

        // Select first room if none selected
        if (!chatService.getRooms().isEmpty() && roomListView.getSelectionModel().isEmpty()) {
            roomListView.getSelectionModel().select(0);
        }
    }

    private HBox createMessageBubble(ChatMessageModel item) {
        VBox bubble = new VBox(4);
        bubble.getStyleClass().add("message-bubble");

        HBox metaHeader = new HBox(8);
        metaHeader.setAlignment(Pos.CENTER_LEFT);

        Label senderLabel = new Label(item.getSenderName());
        Label timeLabel = new Label(item.getFormattedTime());
        timeLabel.getStyleClass().add("message-time");

        Label originBadge = new Label(item.getOriginServerId());
        originBadge.getStyleClass().add("message-origin-badge");

        metaHeader.getChildren().addAll(senderLabel, originBadge, timeLabel);

        Label contentLabel = new Label(item.getContent());
        contentLabel.setWrapText(true);
        contentLabel.getStyleClass().add("message-content");

        bubble.getChildren().addAll(metaHeader, contentLabel);

        HBox container = new HBox();
        container.getStyleClass().add("message-row");

        if (item.isOwnMessage()) {
            bubble.getStyleClass().add("message-bubble-own");
            senderLabel.getStyleClass().add("message-sender-own");
            container.setAlignment(Pos.CENTER_RIGHT);
            container.getChildren().add(bubble);
        } else {
            bubble.getStyleClass().add("message-bubble-other");
            senderLabel.getStyleClass().add("message-sender");
            container.setAlignment(Pos.CENTER_LEFT);
            container.getChildren().add(bubble);
        }

        return container;
    }

    @FXML
    public void onInputKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            if (event.isShiftDown()) {
                // Shift+Enter inserts newline
                messageInput.appendText("\n");
            } else {
                event.consume();
                onSendMessage();
            }
        }
    }

    @FXML
    public void onSendMessage() {
        String text = messageInput.getText();
        if (text == null || text.trim().isEmpty()) return;

        sendButton.setDisable(true);
        chatService.sendMessage(text.trim(), success -> {
            Platform.runLater(() -> {
                sendButton.setDisable(false);
                if (success) {
                    messageInput.clear();
                    messageInput.requestFocus();
                }
            });
        });
    }

    @FXML
    public void onAdminNavClicked() {
        router.navigateTo(ViewId.ADMIN);
    }

    @FXML
    public void onSettingsNavClicked() {
        router.navigateTo(ViewId.SETTINGS);
    }

    @FXML
    public void onDisconnectNavClicked() {
        chatService.disconnect();
        router.navigateTo(ViewId.LOGIN);
    }
}
