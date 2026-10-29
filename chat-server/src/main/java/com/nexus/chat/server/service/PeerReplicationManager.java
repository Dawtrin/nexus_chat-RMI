package com.nexus.chat.server.service;

import com.nexus.chat.contract.dto.ChatMessageDTO;
import com.nexus.chat.contract.dto.OperationResult;
import com.nexus.chat.contract.dto.ServerStatusDTO;
import com.nexus.chat.contract.enums.EventSeverity;
import com.nexus.chat.contract.enums.PeerSyncStatus;
import com.nexus.chat.contract.enums.ServerStatus;
import com.nexus.chat.contract.remote.ReplicationRemote;
import com.nexus.chat.server.config.ServerConfig;
import com.nexus.chat.server.model.OutboxItem;
import com.nexus.chat.server.monitoring.ServerMetrics;
import com.nexus.chat.server.monitoring.SystemLogService;
import com.nexus.chat.server.repository.MessageRepository;
import com.nexus.chat.server.repository.OutboxRepository;
import com.nexus.chat.server.rmi.RmiRegistryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages durable replication outbox dispatch, reconnection catch-up sync,
 * loop prevention, and peer health monitoring.
 */
public class PeerReplicationManager implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(PeerReplicationManager.class);

    private final ServerConfig config;
    private final OutboxRepository outboxRepository;
    private final MessageRepository messageRepository;
    private final ServerMetrics metrics;
    private final SystemLogService logService;

    private final ScheduledExecutorService outboxExecutor;
    private final AtomicReference<ServerStatus> peerStatus = new AtomicReference<>(ServerStatus.UNKNOWN);
    private final AtomicReference<PeerSyncStatus> peerSyncStatus = new AtomicReference<>(PeerSyncStatus.UNKNOWN);
    private final AtomicReference<Instant> lastSuccessfulReplication = new AtomicReference<>(null);
    private final AtomicLong lastAcknowledgedPeerOriginSeq = new AtomicLong(0);
    private final AtomicBoolean isDraining = new AtomicBoolean(false);

    public PeerReplicationManager(ServerConfig config,
                                  OutboxRepository outboxRepository,
                                  MessageRepository messageRepository,
                                  ServerMetrics metrics,
                                  SystemLogService logService) {
        this.config = config;
        this.outboxRepository = outboxRepository;
        this.messageRepository = messageRepository;
        this.metrics = metrics;
        this.logService = logService;

        this.outboxExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "nexus-outbox-worker");
            t.setDaemon(true);
            return t;
        });

        // Run outbox draining and health check every 1 second
        outboxExecutor.scheduleWithFixedDelay(this::processOutboxAndHealth, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * Submits a message for replication. Enforces strict loop prevention:
     * only messages originating on THIS server are enqueued for peer delivery.
     */
    public void submitReplication(ChatMessageDTO message) {
        if (!message.getOriginServerId().equals(config.getServerId())) {
            log.debug("Skipping replication for foreign message {} (origin={})",
                    message.getMessageId(), message.getOriginServerId());
            return;
        }

        // Durable queuing into outbox
        outboxRepository.enqueue(message, config.getPeerId());
        log.debug("Enqueued message {} into outbox for peer {}", message.getMessageId(), config.getPeerId());

        // Trigger immediate drain asynchronously
        outboxExecutor.submit(this::processOutboxAndHealth);
    }

    private void processOutboxAndHealth() {
        if (!isDraining.compareAndSet(false, true)) {
            return;
        }

        try {
            ReplicationRemote peerService;
            try {
                peerService = getPeerService();
                ServerStatusDTO remotePeerStatus = peerService.getPeerStatus(config.getServerId());
                if (remotePeerStatus != null) {
                    boolean wasUnreachable = peerStatus.get() != ServerStatus.ONLINE;
                    peerStatus.set(ServerStatus.ONLINE);

                    // If recovering from a partition/disconnect, perform catch-up backfill
                    if (wasUnreachable) {
                        performCatchUpSync(peerService);
                    }
                }
            } catch (Exception e) {
                peerStatus.set(ServerStatus.UNREACHABLE);
                peerSyncStatus.set(PeerSyncStatus.FAILED);
                return;
            }

            // Drain pending outbox items
            List<OutboxItem> pending = outboxRepository.findPending(config.getPeerId(), 50);
            if (pending.isEmpty()) {
                if (peerStatus.get() == ServerStatus.ONLINE) {
                    peerSyncStatus.set(PeerSyncStatus.SYNCED);
                }
                return;
            }

            peerSyncStatus.set(PeerSyncStatus.IN_PROGRESS);
            List<ChatMessageDTO> batch = new ArrayList<>(pending.size());
            List<Long> outboxIds = new ArrayList<>(pending.size());

            for (OutboxItem item : pending) {
                batch.add(item.getMessage());
                outboxIds.add(item.getOutboxId());
            }

            try {
                OperationResult<Void> result = peerService.replicateBatch(batch, config.getServerId());
                if (result.isSuccess()) {
                    outboxRepository.markBatchDelivered(outboxIds);
                    lastSuccessfulReplication.set(Instant.now());
                    metrics.incrementMessagesReplicated();
                    peerSyncStatus.set(PeerSyncStatus.SYNCED);

                    logService.logEvent(EventSeverity.INFO, "REPLICATION",
                            String.format("Delivered replication batch (%d messages) to peer %s",
                                    batch.size(), config.getPeerId()));
                } else {
                    for (Long id : outboxIds) {
                        outboxRepository.markFailed(id, result.getMessage());
                    }
                    metrics.incrementReplicationErrors();
                    peerSyncStatus.set(PeerSyncStatus.FAILED);
                }
            } catch (Exception e) {
                peerStatus.set(ServerStatus.UNREACHABLE);
                peerSyncStatus.set(PeerSyncStatus.FAILED);
                for (Long id : outboxIds) {
                    outboxRepository.markFailed(id, e.getMessage());
                }
                metrics.incrementReplicationErrors();
            }

        } catch (Exception e) {
            log.warn("Error in outbox processing: {}", e.getMessage());
        } finally {
            isDraining.set(false);
        }
    }

    /**
     * Reconciles missing messages from the peer after network reconnection.
     */
    private void performCatchUpSync(ReplicationRemote peerService) {
        logService.logEvent(EventSeverity.INFO, "REPLICATION",
                String.format("Peer %s reconnected. Initiating catch-up backfill...", config.getPeerId()));

        try {
            long lastSeq = lastAcknowledgedPeerOriginSeq.get();
            List<ChatMessageDTO> peerMissedMessages = peerService.getMessagesAfter(config.getPeerId(), lastSeq, 100);

            if (!peerMissedMessages.isEmpty()) {
                int saved = messageRepository.saveBatch(peerMissedMessages);
                long maxSeq = peerMissedMessages.stream()
                        .mapToLong(ChatMessageDTO::getOriginSequence)
                        .max()
                        .orElse(lastSeq);
                lastAcknowledgedPeerOriginSeq.set(maxSeq);

                logService.logEvent(EventSeverity.INFO, "REPLICATION",
                        String.format("Catch-up completed: backfilled %d messages from peer %s", saved, config.getPeerId()));
            }
        } catch (Exception e) {
            log.warn("Error during catch-up backfill from peer {}: {}", config.getPeerId(), e.getMessage());
        }
    }

    public ServerStatus checkPeerHealth() {
        processOutboxAndHealth();
        return peerStatus.get();
    }

    private ReplicationRemote getPeerService() throws Exception {
        Registry registry = LocateRegistry.getRegistry(config.getPeerHost(), config.getPeerRegistryPort());
        return (ReplicationRemote) registry.lookup(RmiRegistryManager.REPLICATION_SERVICE_NAME);
    }

    public ServerStatus getPeerStatus() {
        return peerStatus.get();
    }

    public PeerSyncStatus getPeerSyncStatus() {
        return peerSyncStatus.get();
    }

    public Instant getLastSuccessfulReplication() {
        return lastSuccessfulReplication.get();
    }

    public int getPendingOutboxCount() {
        return outboxRepository.countPending(config.getPeerId());
    }

    @Override
    public void close() {
        outboxExecutor.shutdownNow();
    }
}
