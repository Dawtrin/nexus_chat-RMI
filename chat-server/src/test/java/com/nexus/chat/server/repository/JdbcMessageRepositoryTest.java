package com.nexus.chat.server.repository;

import com.nexus.chat.contract.dto.ChatMessageDTO;
import com.nexus.chat.contract.enums.MessageType;
import com.nexus.chat.server.config.ServerConfig;
import com.nexus.chat.server.database.DatabaseManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcMessageRepositoryTest {

    private JdbcMessageRepository repository;

    @BeforeEach
    void setUp() {
        ServerConfig config = new ServerConfig(new String[]{"--database.enabled=false"});
        DatabaseManager dbManager = new DatabaseManager(config.getDatabaseConfig());
        repository = new JdbcMessageRepository(dbManager);
    }

    @Test
    void testMonotonicArrivalSequenceAcrossMultipleOrigins() {
        // Message 1 from server-a with origin sequence 100
        ChatMessageDTO msg1 = new ChatMessageDTO(
                UUID.randomUUID().toString(), "general", "u1", "Alice",
                "Msg 1", Instant.now(), "server-a", 100L, MessageType.CHAT
        );
        ChatMessageDTO saved1 = repository.save(msg1);

        // Message 2 from server-b with origin sequence 5 (arrived later)
        ChatMessageDTO msg2 = new ChatMessageDTO(
                UUID.randomUUID().toString(), "general", "u2", "Bob",
                "Msg 2", Instant.now(), "server-b", 5L, MessageType.CHAT
        );
        ChatMessageDTO saved2 = repository.save(msg2);

        // Local sequences must be strictly monotonic
        assertThat(saved1.getLocalSequence()).isPositive();
        assertThat(saved2.getLocalSequence()).isGreaterThan(saved1.getLocalSequence());

        // Client polling after sequence 0 should receive both
        List<ChatMessageDTO> all = repository.findByRoomAfterSequence("general", 0);
        assertThat(all).hasSize(2);
        assertThat(all.get(0).getMessageId()).isEqualTo(msg1.getMessageId());
        assertThat(all.get(1).getMessageId()).isEqualTo(msg2.getMessageId());

        // Client polling after saved1.getLocalSequence() must receive msg2 even though msg2's originSeq (5) < msg1's originSeq (100)!
        List<ChatMessageDTO> afterFirst = repository.findByRoomAfterSequence("general", saved1.getLocalSequence());
        assertThat(afterFirst).hasSize(1);
        assertThat(afterFirst.get(0).getMessageId()).isEqualTo(msg2.getMessageId());
    }

    @Test
    void testPersistenceModeReporting() {
        assertThat(repository.getPersistenceMode()).isEqualTo("IN_MEMORY_FALLBACK");
    }
}
