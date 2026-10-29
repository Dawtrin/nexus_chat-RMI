package com.nexus.chat.desktop.service;

import com.nexus.chat.contract.dto.ChatMessageDTO;
import com.nexus.chat.contract.dto.ChatRoomDTO;
import com.nexus.chat.contract.dto.ClientSessionDTO;
import com.nexus.chat.contract.dto.OperationResult;
import com.nexus.chat.contract.remote.ChatRemote;
import com.nexus.chat.desktop.config.ClientConfig;
import com.nexus.chat.desktop.model.ChatMessageModel;
import com.nexus.chat.desktop.model.ChatRoomModel;
import com.nexus.chat.desktop.model.ConnectionState;
import com.nexus.chat.desktop.rmi.RmiClientManager;
import com.nexus.chat.desktop.util.FxAsync;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * High-level desktop service orchestrating chat sessions, message polling, and room navigation.
 */
public class ChatClientService implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(ChatClientService.class);

    private final RmiClientManager rmiManager;
    private final ObjectProperty<ConnectionState> connectionState = new SimpleObjectProperty<>(ConnectionState.DISCONNECTED);
    private final StringProperty currentServerId = new SimpleStringProperty("-");
    private final StringProperty currentSessionId = new SimpleStringProperty("");
    private final StringProperty currentUsername = new SimpleStringProperty("");
    private final StringProperty currentRoomId = new SimpleStringProperty("general");
    private final StringProperty statusMessage = new SimpleStringProperty("Ready to connect");

    private final ObservableList<ChatRoomModel> rooms = FXCollections.observableArrayList();
    private final ObservableList<ChatMessageModel> messages = FXCollections.observableArrayList();
    private final Set<String> receivedMessageIds = Collections.synchronizedSet(new HashSet<>());

    private ScheduledExecutorService pollingExecutor;
    private long lastSeenSequence = 0;

    public ChatClientService(RmiClientManager rmiManager) {
        this.rmiManager = rmiManager;
    }

    public void connectAndRegister(String host, int port, String username, Consumer<OperationResult<ClientSessionDTO>> onComplete) {
        connectionState.set(ConnectionState.CONNECTING);
        statusMessage.set("Connecting to server at " + host + ":" + port + "...");

        FxAsync.execute(() -> {
            rmiManager.connect(host, port);
            ChatRemote service = rmiManager.getChatService();
            String instanceId = UUID.randomUUID().toString();

            OperationResult<ClientSessionDTO> result = service.registerClient(username, instanceId);
            if (!result.isSuccess()) {
                throw new RemoteException(result.getMessage());
            }

            String serverId = service.getServerId();
            List<ChatRoomDTO> roomDTOs = service.getRooms(result.getData().getSessionId());

            return new ConnectionBootstrapData(result.getData(), serverId, roomDTOs);
        }, data -> {
            connectionState.set(ConnectionState.CONNECTED);
            currentSessionId.set(data.session.getSessionId());
            currentUsername.set(data.session.getUsername());
            currentServerId.set(data.serverId);
            statusMessage.set("Connected to " + data.serverId + " (" + host + ":" + port + ")");

            rooms.clear();
            for (ChatRoomDTO dto : data.rooms) {
                rooms.add(new ChatRoomModel(dto));
            }

            startPolling();
            joinRoom(currentRoomId.get());

            if (onComplete != null) {
                onComplete.accept(OperationResult.success("Connected", data.session));
            }
        }, error -> {
            connectionState.set(ConnectionState.FAILED);
            statusMessage.set("Connection error: " + error.getMessage());
            rmiManager.reset();
            if (onComplete != null) {
                onComplete.accept(OperationResult.failure("CONNECTION_FAILED", error.getMessage()));
            }
        });
    }

    public void joinRoom(String roomId) {
        if (!isConnected()) return;

        this.currentRoomId.set(roomId);
        messages.clear();
        receivedMessageIds.clear();
        lastSeenSequence = 0;

        String sessId = currentSessionId.get();
        FxAsync.execute(() -> {
            ChatRemote service = rmiManager.getChatService();
            service.joinRoom(sessId, roomId);
            return service.getMessages(sessId, roomId, 0);
        }, initialMessages -> {
            for (ChatMessageDTO dto : initialMessages) {
                appendMessageIfNew(dto);
            }
        }, error -> log.warn("Failed joining room {}: {}", roomId, error.getMessage()));
    }

    public void sendMessage(String content, Consumer<Boolean> onComplete) {
        if (!isConnected() || content == null || content.trim().isEmpty()) {
            if (onComplete != null) onComplete.accept(false);
            return;
        }

        String sessId = currentSessionId.get();
        String roomId = currentRoomId.get();

        FxAsync.execute(() -> {
            ChatRemote service = rmiManager.getChatService();
            return service.sendMessage(sessId, roomId, content);
        }, result -> {
            if (result.isSuccess() && result.getData() != null) {
                appendMessageIfNew(result.getData());
                if (onComplete != null) onComplete.accept(true);
            } else {
                statusMessage.set("Send failed: " + result.getMessage());
                if (onComplete != null) onComplete.accept(false);
            }
        }, error -> {
            statusMessage.set("Send failed: " + error.getMessage());
            if (onComplete != null) onComplete.accept(false);
        });
    }

    private void startPolling() {
        stopPolling();
        pollingExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "nexus-message-poller");
            t.setDaemon(true);
            return t;
        });

        pollingExecutor.scheduleWithFixedDelay(this::pollMessagesAndHeartbeat,
                ClientConfig.DEFAULT_POLL_INTERVAL_MS,
                ClientConfig.DEFAULT_POLL_INTERVAL_MS,
                TimeUnit.MILLISECONDS);
    }

    private void pollMessagesAndHeartbeat() {
        if (!isConnected()) return;

        String sessId = currentSessionId.get();
        String roomId = currentRoomId.get();

        try {
            ChatRemote service = rmiManager.getChatService();
            if (service == null) return;

            // Heartbeat
            service.heartbeat(sessId);

            // Fetch new messages
            List<ChatMessageDTO> newMessages = service.getMessages(sessId, roomId, lastSeenSequence);
            if (!newMessages.isEmpty()) {
                Platform.runLater(() -> {
                    for (ChatMessageDTO dto : newMessages) {
                        appendMessageIfNew(dto);
                    }
                });
            }
        } catch (Exception e) {
            log.warn("Polling / Heartbeat failed: {}", e.getMessage());
            Platform.runLater(() -> {
                if (connectionState.get() == ConnectionState.CONNECTED) {
                    connectionState.set(ConnectionState.RECONNECTING);
                    statusMessage.set("Connection interrupted. Attempting reconnection...");
                }
            });
        }
    }

    private void appendMessageIfNew(ChatMessageDTO dto) {
        if (receivedMessageIds.add(dto.getMessageId())) {
            boolean isOwn = dto.getSenderName().equalsIgnoreCase(currentUsername.get());
            messages.add(new ChatMessageModel(dto, isOwn));
            long seq = dto.getLocalSequence() > 0 ? dto.getLocalSequence() : dto.getOriginSequence();
            if (seq > lastSeenSequence) {
                lastSeenSequence = seq;
            }
        }
    }

    public void disconnect() {
        stopPolling();
        if (isConnected()) {
            String sessId = currentSessionId.get();
            FxAsync.runAsync(() -> {
                try {
                    ChatRemote service = rmiManager.getChatService();
                    if (service != null && !sessId.isEmpty()) {
                        service.disconnectClient(sessId);
                    }
                } catch (Exception ignored) {
                } finally {
                    rmiManager.reset();
                }
            });
        }
        connectionState.set(ConnectionState.DISCONNECTED);
        statusMessage.set("Disconnected");
        currentSessionId.set("");
        currentServerId.set("-");
    }

    public void stopPolling() {
        if (pollingExecutor != null && !pollingExecutor.isShutdown()) {
            pollingExecutor.shutdownNow();
            pollingExecutor = null;
        }
    }

    public boolean isConnected() {
        return connectionState.get() == ConnectionState.CONNECTED && rmiManager.isConnected();
    }

    @Override
    public void close() {
        disconnect();
    }

    // Properties
    public ObjectProperty<ConnectionState> connectionStateProperty() { return connectionState; }
    public StringProperty currentServerIdProperty() { return currentServerId; }
    public StringProperty currentUsernameProperty() { return currentUsername; }
    public StringProperty currentRoomIdProperty() { return currentRoomId; }
    public StringProperty statusMessageProperty() { return statusMessage; }
    public ObservableList<ChatRoomModel> getRooms() { return rooms; }
    public ObservableList<ChatMessageModel> getMessages() { return messages; }

    private record ConnectionBootstrapData(ClientSessionDTO session, String serverId, List<ChatRoomDTO> rooms) {}
}
