package com.nexus.chat.server.service;

import com.nexus.chat.contract.dto.ChatMessageDTO;
import com.nexus.chat.contract.dto.ChatRoomDTO;
import com.nexus.chat.contract.dto.ClientSessionDTO;
import com.nexus.chat.contract.dto.OperationResult;
import com.nexus.chat.server.config.ServerConfig;
import com.nexus.chat.server.database.DatabaseManager;
import com.nexus.chat.server.monitoring.ServerMetrics;
import com.nexus.chat.server.monitoring.SystemLogService;
import com.nexus.chat.server.repository.ChatRoomRepository;
import com.nexus.chat.server.repository.ClientSessionRepository;
import com.nexus.chat.server.repository.InMemoryClientSessionRepository;
import com.nexus.chat.server.repository.JdbcChatRoomRepository;
import com.nexus.chat.server.repository.JdbcMessageRepository;
import com.nexus.chat.server.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChatServiceTest {

    private ChatServiceImpl chatService;
    private ReplicationServiceImpl replicationService;

    @BeforeEach
    void setUp() {
        ServerConfig config = new ServerConfig(new String[]{"--database.enabled=false"});
        ServerMetrics metrics = new ServerMetrics();
        SystemLogService logService = new SystemLogService("test-server");

        DatabaseManager dbManager = new DatabaseManager(config.getDatabaseConfig());
        MessageRepository messageRepo = new JdbcMessageRepository(dbManager);
        ChatRoomRepository roomRepo = new JdbcChatRoomRepository(dbManager);
        ClientSessionRepository sessionRepo = new InMemoryClientSessionRepository();

        ClientSessionManager sessionManager = new ClientSessionManager(config.getServerId(), sessionRepo, logService);
        PeerReplicationManager replicationManager = new PeerReplicationManager(config, metrics, logService);

        chatService = new ChatServiceImpl(config, messageRepo, roomRepo, sessionManager, replicationManager, metrics, logService);
        replicationService = new ReplicationServiceImpl(config, messageRepo, dbManager, metrics, logService);
    }

    @Test
    void testRegisterAndSendChat() throws Exception {
        OperationResult<ClientSessionDTO> regResult = chatService.registerClient("Alice", "inst-1");
        assertThat(regResult.isSuccess()).isTrue();
        ClientSessionDTO session = regResult.getData();
        assertThat(session.getUsername()).isEqualTo("Alice");

        OperationResult<Void> joinResult = chatService.joinRoom(session.getSessionId(), "general");
        assertThat(joinResult.isSuccess()).isTrue();

        OperationResult<ChatMessageDTO> sendResult = chatService.sendMessage(
                session.getSessionId(), "general", "Hello from Alice!"
        );
        assertThat(sendResult.isSuccess()).isTrue();
        assertThat(sendResult.getData().getContent()).isEqualTo("Hello from Alice!");

        List<ChatMessageDTO> messages = chatService.getMessages(session.getSessionId(), "general", 0);
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getContent()).isEqualTo("Hello from Alice!");
    }

    @Test
    void testReplicationIdempotency() throws Exception {
        OperationResult<ClientSessionDTO> reg = chatService.registerClient("Bob", "inst-2");
        OperationResult<ChatMessageDTO> sent = chatService.sendMessage(reg.getData().getSessionId(), "general", "Replicated message");

        ChatMessageDTO msg = sent.getData();

        // Simulate peer replicating this message to this node
        OperationResult<Void> rep1 = replicationService.replicateMessage(msg, "peer-b");
        assertThat(rep1.isSuccess()).isTrue();

        // Second time must be ignored as duplicate without error
        OperationResult<Void> rep2 = replicationService.replicateMessage(msg, "peer-b");
        assertThat(rep2.isSuccess()).isTrue();
        assertThat(rep2.getMessage()).containsIgnoringCase("duplicate");
    }

    @Test
    void testEmptyMessageRejected() throws Exception {
        OperationResult<ClientSessionDTO> reg = chatService.registerClient("Charlie", "inst-3");
        OperationResult<ChatMessageDTO> sendResult = chatService.sendMessage(
                reg.getData().getSessionId(), "general", "   "
        );
        assertThat(sendResult.isSuccess()).isFalse();
        assertThat(sendResult.getErrorCode()).isEqualTo("EMPTY_MESSAGE");
    }
}
