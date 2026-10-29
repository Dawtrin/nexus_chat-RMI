package com.nexus.chat.server.service;

import com.nexus.chat.contract.dto.ChatMessageDTO;
import com.nexus.chat.contract.enums.MessageType;
import com.nexus.chat.server.config.ServerConfig;
import com.nexus.chat.server.database.DatabaseManager;
import com.nexus.chat.server.monitoring.ServerMetrics;
import com.nexus.chat.server.monitoring.SystemLogService;
import com.nexus.chat.server.repository.JdbcMessageRepository;
import com.nexus.chat.server.repository.JdbcOutboxRepository;
import com.nexus.chat.server.repository.MessageRepository;
import com.nexus.chat.server.repository.OutboxRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReplicationOutboxTest {

    private ServerConfig config;
    private OutboxRepository outboxRepository;
    private MessageRepository messageRepository;
    private PeerReplicationManager replicationManager;

    @BeforeEach
    void setUp() {
        config = new ServerConfig(new String[]{
                "--server.id=server-a",
                "--server.peer.id=server-b",
                "--server.peer.host=127.0.0.1",
                "--server.peer.registry.port=54321", // non-existent port to simulate offline peer
                "--database.enabled=false"
        });

        ServerMetrics metrics = new ServerMetrics();
        SystemLogService logService = new SystemLogService("server-a");

        DatabaseManager dbManager = new DatabaseManager(config.getDatabaseConfig());
        outboxRepository = new JdbcOutboxRepository(dbManager);
        messageRepository = new JdbcMessageRepository(dbManager);

        replicationManager = new PeerReplicationManager(
                config, outboxRepository, messageRepository, metrics, logService
        );
    }

    @AfterEach
    void tearDown() {
        if (replicationManager != null) {
            replicationManager.close();
        }
    }

    @Test
    void testMessageQueuedInDurableOutboxWhenPeerOffline() {
        ChatMessageDTO localMsg = new ChatMessageDTO(
                UUID.randomUUID().toString(),
                "general",
                "usr-1",
                "Alice",
                "Hello when peer is offline!",
                Instant.now(),
                "server-a", // originated on server-a
                1L,
                MessageType.CHAT
        );

        // Submit for replication
        replicationManager.submitReplication(localMsg);

        // Verify outbox queued the message
        int pending = replicationManager.getPendingOutboxCount();
        assertThat(pending).isEqualTo(1);
    }

    @Test
    void testLoopPreventionDoesNotQueueForeignMessages() {
        ChatMessageDTO foreignMsg = new ChatMessageDTO(
                UUID.randomUUID().toString(),
                "general",
                "usr-2",
                "Bob",
                "Hello from peer server b!",
                Instant.now(),
                "server-b", // originated on foreign peer
                5L,
                MessageType.CHAT
        );

        // Submit for replication
        replicationManager.submitReplication(foreignMsg);

        // Loop prevention rule: foreign message must NOT be enqueued back to outbox!
        int pending = replicationManager.getPendingOutboxCount();
        assertThat(pending).isEqualTo(0);
    }
}
