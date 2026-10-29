package com.nexus.chat.server.service;

import com.nexus.chat.contract.dto.ChatMessageDTO;
import com.nexus.chat.contract.dto.OperationResult;
import com.nexus.chat.contract.dto.ServerStatusDTO;
import com.nexus.chat.contract.enums.EventSeverity;
import com.nexus.chat.contract.enums.ServerStatus;
import com.nexus.chat.contract.remote.ReplicationRemote;
import com.nexus.chat.server.config.ServerConfig;
import com.nexus.chat.server.monitoring.ServerMetrics;
import com.nexus.chat.server.monitoring.SystemLogService;
import com.nexus.chat.server.rmi.RmiRegistryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages peer connection, loop-free message replication dispatch, and peer health monitoring.
 */
public class PeerReplicationManager implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(PeerReplicationManager.class);

    private final ServerConfig config;
    private final ServerMetrics metrics;
    private final SystemLogService logService;
    private final ExecutorService replicationExecutor;

    private final AtomicReference<ServerStatus> peerStatus = new AtomicReference<>(ServerStatus.UNKNOWN);
    private Instant lastSuccessfulReplication = null;

    public PeerReplicationManager(ServerConfig config, ServerMetrics metrics, SystemLogService logService) {
        this.config = config;
        this.metrics = metrics;
        this.logService = logService;
        this.replicationExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "nexus-peer-replication");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Submits a locally originated message for asynchronous replication to the configured peer.
     * Enforces strict loop prevention: only messages created on this node are replicated to peer.
     *
     * @param message Chat message
     */
    public void submitReplication(ChatMessageDTO message) {
        // Loop prevention rule: never replicate a message that originated on a different server!
        if (!message.getOriginServerId().equals(config.getServerId())) {
            log.debug("Skipping replication for foreign message {} (origin={})",
                    message.getMessageId(), message.getOriginServerId());
            return;
        }

        replicationExecutor.submit(() -> doReplicate(message));
    }

    private void doReplicate(ChatMessageDTO message) {
        try {
            ReplicationRemote peerService = getPeerService();
            OperationResult<Void> result = peerService.replicateMessage(message, config.getServerId());

            if (result.isSuccess()) {
                peerStatus.set(ServerStatus.ONLINE);
                lastSuccessfulReplication = Instant.now();
                metrics.incrementMessagesReplicated();
                logService.logEvent(EventSeverity.INFO, "REPLICATION",
                        String.format("Replicated message %s to peer %s successfully",
                                message.getMessageId(), config.getPeerId()));
            } else {
                log.warn("Peer replication rejected for message {}: {}", message.getMessageId(), result.getMessage());
                metrics.incrementReplicationErrors();
            }
        } catch (Exception e) {
            peerStatus.set(ServerStatus.UNREACHABLE);
            metrics.incrementReplicationErrors();
            logService.logEvent(EventSeverity.WARN, "REPLICATION",
                    String.format("Failed replicating message %s to peer %s (%s:%d): %s",
                            message.getMessageId(), config.getPeerId(), config.getPeerHost(),
                            config.getPeerRegistryPort(), e.getMessage()));
        }
    }

    public ServerStatus checkPeerHealth() {
        try {
            ReplicationRemote peerService = getPeerService();
            ServerStatusDTO status = peerService.getPeerStatus(config.getServerId());
            if (status != null) {
                peerStatus.set(ServerStatus.ONLINE);
                return ServerStatus.ONLINE;
            }
        } catch (Exception e) {
            peerStatus.set(ServerStatus.UNREACHABLE);
        }
        return peerStatus.get();
    }

    private ReplicationRemote getPeerService() throws Exception {
        Registry registry = LocateRegistry.getRegistry(config.getPeerHost(), config.getPeerRegistryPort());
        return (ReplicationRemote) registry.lookup(RmiRegistryManager.REPLICATION_SERVICE_NAME);
    }

    public ServerStatus getPeerStatus() {
        return peerStatus.get();
    }

    public Instant getLastSuccessfulReplication() {
        return lastSuccessfulReplication;
    }

    @Override
    public void close() {
        replicationExecutor.shutdownNow();
    }
}
