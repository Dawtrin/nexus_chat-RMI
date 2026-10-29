package com.nexus.chat.server.service;

import com.nexus.chat.contract.dto.ChatMessageDTO;
import com.nexus.chat.contract.dto.OperationResult;
import com.nexus.chat.contract.dto.ServerStatusDTO;
import com.nexus.chat.contract.enums.EventSeverity;
import com.nexus.chat.contract.enums.ServerStatus;
import com.nexus.chat.contract.remote.ReplicationRemote;
import com.nexus.chat.server.config.ServerConfig;
import com.nexus.chat.server.database.DatabaseManager;
import com.nexus.chat.server.monitoring.ServerMetrics;
import com.nexus.chat.server.monitoring.SystemLogService;
import com.nexus.chat.server.repository.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.util.List;

/**
 * Implementation of the server-to-server ReplicationRemote RMI interface with batch processing
 * and strict loop protection.
 */
public class ReplicationServiceImpl implements ReplicationRemote {
    private static final Logger log = LoggerFactory.getLogger(ReplicationServiceImpl.class);

    private final ServerConfig config;
    private final MessageRepository messageRepository;
    private final DatabaseManager databaseManager;
    private final ServerMetrics metrics;
    private final SystemLogService logService;

    public ReplicationServiceImpl(ServerConfig config,
                                  MessageRepository messageRepository,
                                  DatabaseManager databaseManager,
                                  ServerMetrics metrics,
                                  SystemLogService logService) {
        this.config = config;
        this.messageRepository = messageRepository;
        this.databaseManager = databaseManager;
        this.metrics = metrics;
        this.logService = logService;
    }

    @Override
    public OperationResult<Void> replicateMessage(ChatMessageDTO message, String sourceServerId) throws RemoteException {
        if (message == null || message.getMessageId() == null) {
            return OperationResult.failure("INVALID_MESSAGE", "Replicated message or ID is null");
        }

        // Idempotency check: duplicate message IDs must not create duplicate entries
        if (messageRepository.existsById(message.getMessageId())) {
            log.debug("Duplicate message {} from source {} already stored. Idempotent success.",
                    message.getMessageId(), sourceServerId);
            return OperationResult.ok("Duplicate message ignored");
        }

        // Persist replicated message locally.
        // NOTE: We do NOT re-broadcast or replicate this message back, preventing replication loops.
        messageRepository.save(message);
        metrics.incrementMessagesProcessed();

        logService.logEvent(EventSeverity.INFO, "REPLICATION",
                String.format("Accepted replicated message %s (origin=%s) from peer %s",
                        message.getMessageId(), message.getOriginServerId(), sourceServerId));

        return OperationResult.ok("Message replicated and persisted");
    }

    @Override
    public OperationResult<Void> replicateBatch(List<ChatMessageDTO> messages, String sourceServerId) throws RemoteException {
        if (messages == null || messages.isEmpty()) {
            return OperationResult.ok("Empty batch accepted");
        }

        int count = 0;
        for (ChatMessageDTO msg : messages) {
            if (msg != null && msg.getMessageId() != null) {
                if (!messageRepository.existsById(msg.getMessageId())) {
                    messageRepository.save(msg);
                    count++;
                }
            }
        }

        metrics.incrementMessagesProcessed();
        logService.logEvent(EventSeverity.INFO, "REPLICATION",
                String.format("Accepted replicated batch (%d new messages) from peer %s", count, sourceServerId));

        return OperationResult.ok("Batch of " + count + " messages persisted");
    }

    @Override
    public List<ChatMessageDTO> getMessagesAfter(String originServerId, long afterSequence, int limit) throws RemoteException {
        int boundedLimit = Math.min(Math.max(limit, 1), 500);
        return messageRepository.findByOriginAfterSequence(originServerId, afterSequence, boundedLimit);
    }

    @Override
    public ServerStatusDTO getPeerStatus(String queryingServerId) throws RemoteException {
        ServerStatus dbStatus = databaseManager.isConnected() ? ServerStatus.ONLINE : ServerStatus.DEGRADED;
        return new ServerStatusDTO(
                config.getServerId(),
                "node",
                config.getAdvertisedHost(),
                "0.0.0.0",
                config.getAdvertisedHost(),
                config.getRmiRegistryPort(),
                config.getRmiServicePort(),
                ServerStatus.ONLINE,
                ServerStatus.ONLINE,
                dbStatus,
                metrics.getUptimeSeconds(),
                metrics.getStartedAt(),
                0,
                0,
                null,
                messageRepository.getPersistenceMode()
        );
    }
}
