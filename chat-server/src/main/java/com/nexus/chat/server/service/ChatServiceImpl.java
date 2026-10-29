package com.nexus.chat.server.service;

import com.nexus.chat.contract.dto.ChatMessageDTO;
import com.nexus.chat.contract.dto.ChatRoomDTO;
import com.nexus.chat.contract.dto.ClientSessionDTO;
import com.nexus.chat.contract.dto.OperationResult;
import com.nexus.chat.contract.enums.EventSeverity;
import com.nexus.chat.contract.enums.MessageType;
import com.nexus.chat.contract.remote.ChatRemote;
import com.nexus.chat.server.config.ServerConfig;
import com.nexus.chat.server.monitoring.ServerMetrics;
import com.nexus.chat.server.monitoring.SystemLogService;
import com.nexus.chat.server.repository.ChatRoomRepository;
import com.nexus.chat.server.repository.MessageRepository;
import com.nexus.chat.server.rmi.RemoteClientContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of the client-facing ChatRemote RMI interface.
 */
public class ChatServiceImpl implements ChatRemote {
    private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);

    private final ServerConfig config;
    private final MessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ClientSessionManager sessionManager;
    private final PeerReplicationManager replicationManager;
    private final ServerMetrics metrics;
    private final SystemLogService logService;

    public ChatServiceImpl(ServerConfig config,
                           MessageRepository messageRepository,
                           ChatRoomRepository chatRoomRepository,
                           ClientSessionManager sessionManager,
                           PeerReplicationManager replicationManager,
                           ServerMetrics metrics,
                           SystemLogService logService) {
        this.config = config;
        this.messageRepository = messageRepository;
        this.chatRoomRepository = chatRoomRepository;
        this.sessionManager = sessionManager;
        this.replicationManager = replicationManager;
        this.metrics = metrics;
        this.logService = logService;
    }

    @Override
    public OperationResult<ClientSessionDTO> registerClient(String username, String clientInstanceId) throws RemoteException {
        if (username == null || username.trim().isEmpty()) {
            return OperationResult.failure("INVALID_USERNAME", "Username cannot be empty");
        }

        String observedHost = RemoteClientContext.getObservedClientHost();
        log.info("Client registration request for '{}' from observed IP {}", username, observedHost);

        ClientSessionDTO session = sessionManager.registerSession(
                username.trim(),
                clientInstanceId,
                observedHost,
                0 // Remote port is not provided by getClientHost()
        );

        return OperationResult.success("Connected to " + config.getServerId(), session);
    }

    @Override
    public OperationResult<Void> joinRoom(String sessionId, String roomId) throws RemoteException {
        Optional<ClientSessionDTO> sessionOpt = sessionManager.getValidSession(sessionId);
        if (sessionOpt.isEmpty()) {
            return OperationResult.failure("INVALID_SESSION", "Session is expired or invalid");
        }

        Optional<ChatRoomDTO> roomOpt = chatRoomRepository.findById(roomId);
        if (roomOpt.isEmpty()) {
            return OperationResult.failure("ROOM_NOT_FOUND", "Room does not exist: " + roomId);
        }

        sessionManager.recordHeartbeat(sessionId);
        logService.logEvent(EventSeverity.INFO, "CHAT",
                String.format("User '%s' joined room '%s'", sessionOpt.get().getUsername(), roomId));
        return OperationResult.ok();
    }

    @Override
    public OperationResult<ChatMessageDTO> sendMessage(String sessionId, String roomId, String content) throws RemoteException {
        Optional<ClientSessionDTO> sessionOpt = sessionManager.getValidSession(sessionId);
        if (sessionOpt.isEmpty()) {
            return OperationResult.failure("INVALID_SESSION", "Session has expired. Please reconnect.");
        }

        if (content == null || content.trim().isEmpty()) {
            return OperationResult.failure("EMPTY_MESSAGE", "Message content cannot be empty.");
        }

        if (content.length() > 4000) {
            return OperationResult.failure("MESSAGE_TOO_LARGE", "Message exceeds 4000 character limit.");
        }

        ClientSessionDTO session = sessionOpt.get();
        String messageId = UUID.randomUUID().toString();
        long sequence = messageRepository.getNextSequence(config.getServerId());

        ChatMessageDTO message = new ChatMessageDTO(
                messageId,
                roomId,
                session.getSessionId(),
                session.getUsername(),
                content.trim(),
                Instant.now(),
                config.getServerId(),
                sequence,
                MessageType.CHAT
        );

        // 1. Save locally
        messageRepository.save(message);
        metrics.incrementMessagesProcessed();
        sessionManager.recordHeartbeat(sessionId);

        // 2. Submit for asynchronous peer replication (with loop prevention)
        replicationManager.submitReplication(message);

        return OperationResult.success("Message sent", message);
    }

    @Override
    public List<ChatMessageDTO> getMessages(String sessionId, String roomId, long afterSequence) throws RemoteException {
        sessionManager.recordHeartbeat(sessionId);
        return messageRepository.findByRoomAfterSequence(roomId, afterSequence);
    }

    @Override
    public List<ChatRoomDTO> getRooms(String sessionId) throws RemoteException {
        sessionManager.recordHeartbeat(sessionId);
        return chatRoomRepository.findAll();
    }

    @Override
    public void heartbeat(String sessionId) throws RemoteException {
        sessionManager.recordHeartbeat(sessionId);
    }

    @Override
    public void disconnectClient(String sessionId) throws RemoteException {
        sessionManager.disconnect(sessionId);
    }

    @Override
    public String getServerId() throws RemoteException {
        return config.getServerId();
    }
}
