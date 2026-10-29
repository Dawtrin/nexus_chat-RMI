package com.nexus.chat.server.service;

import com.nexus.chat.contract.dto.ChatMessageDTO;
import com.nexus.chat.contract.dto.OperationResult;
import com.nexus.chat.contract.enums.MessageType;
import com.nexus.chat.server.config.ServerConfig;
import com.nexus.chat.server.database.DatabaseManager;
import com.nexus.chat.server.monitoring.ServerMetrics;
import com.nexus.chat.server.monitoring.SystemLogService;
import com.nexus.chat.server.repository.JdbcMessageRepository;
import com.nexus.chat.server.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CatchUpSyncTest {

    private MessageRepository messageRepository;
    private ReplicationServiceImpl replicationService;

    @BeforeEach
    void setUp() {
        ServerConfig config = new ServerConfig(new String[]{"--database.enabled=false"});
        ServerMetrics metrics = new ServerMetrics();
        SystemLogService logService = new SystemLogService("server-a");

        DatabaseManager dbManager = new DatabaseManager(config.getDatabaseConfig());
        messageRepository = new JdbcMessageRepository(dbManager);

        replicationService = new ReplicationServiceImpl(config, messageRepository, dbManager, metrics, logService);
    }

    @Test
    void testReplicateBatchAndIdempotency() throws Exception {
        List<ChatMessageDTO> batch = new ArrayList<>();
        String msg1Id = UUID.randomUUID().toString();
        String msg2Id = UUID.randomUUID().toString();

        batch.add(new ChatMessageDTO(msg1Id, "general", "u1", "Alice", "Batch 1", Instant.now(), "server-b", 1L, MessageType.CHAT));
        batch.add(new ChatMessageDTO(msg2Id, "general", "u1", "Alice", "Batch 2", Instant.now(), "server-b", 2L, MessageType.CHAT));

        // First replication
        OperationResult<Void> res1 = replicationService.replicateBatch(batch, "server-b");
        assertThat(res1.isSuccess()).isTrue();
        assertThat(messageRepository.existsById(msg1Id)).isTrue();
        assertThat(messageRepository.existsById(msg2Id)).isTrue();

        // Second duplicate replication
        OperationResult<Void> res2 = replicationService.replicateBatch(batch, "server-b");
        assertThat(res2.isSuccess()).isTrue();
        assertThat(res2.getMessage()).contains("Batch of 0 messages persisted");

        // Verify catch-up query
        List<ChatMessageDTO> catchUpList = replicationService.getMessagesAfter("server-b", 0, 10);
        assertThat(catchUpList).hasSize(2);
    }
}
