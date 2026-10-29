package com.nexus.chat.contract.dto;

import com.nexus.chat.contract.enums.EventSeverity;
import com.nexus.chat.contract.enums.MessageType;
import com.nexus.chat.contract.enums.ServerStatus;
import com.nexus.chat.contract.enums.SessionStatus;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DtoSerializationTest {

    @SuppressWarnings("unchecked")
    private <T> T roundTrip(T original) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(original);
        }
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            return (T) ois.readObject();
        }
    }

    @Test
    void testChatMessageDTOSerialization() throws Exception {
        ChatMessageDTO original = new ChatMessageDTO(
                UUID.randomUUID().toString(),
                "general",
                "usr-1",
                "Alice",
                "Hello distributed world!",
                Instant.now(),
                "server-a",
                101L,
                MessageType.CHAT,
                42L
        );

        ChatMessageDTO deserialized = roundTrip(original);
        assertThat(deserialized).isEqualTo(original);
        assertThat(deserialized.getContent()).isEqualTo("Hello distributed world!");
        assertThat(deserialized.getOriginSequence()).isEqualTo(101L);
        assertThat(deserialized.getLocalSequence()).isEqualTo(42L);
    }

    @Test
    void testClientSessionDTOSerialization() throws Exception {
        ClientSessionDTO original = new ClientSessionDTO(
                "sess-abc",
                "Bob",
                "client-inst-123",
                "192.168.1.50",
                54321,
                Instant.now(),
                Instant.now(),
                SessionStatus.ACTIVE,
                "server-a"
        );

        ClientSessionDTO deserialized = roundTrip(original);
        assertThat(deserialized).isEqualTo(original);
        assertThat(deserialized.getRemoteIp()).isEqualTo("192.168.1.50");
        assertThat(deserialized.getStatus()).isEqualTo(SessionStatus.ACTIVE);
    }

    @Test
    void testServerStatusDTOSerialization() throws Exception {
        ServerStatusDTO original = new ServerStatusDTO(
                "server-a",
                "node-a.local",
                "192.168.1.101",
                "0.0.0.0",
                "192.168.1.101",
                1099,
                1100,
                ServerStatus.ONLINE,
                ServerStatus.ONLINE,
                ServerStatus.ONLINE,
                3600L,
                Instant.now(),
                5,
                3,
                Instant.now(),
                "MYSQL_DURABLE"
        );

        ServerStatusDTO deserialized = roundTrip(original);
        assertThat(deserialized.getServerId()).isEqualTo("server-a");
        assertThat(deserialized.getServerStatus()).isEqualTo(ServerStatus.ONLINE);
        assertThat(deserialized.getActiveSessionCount()).isEqualTo(5);
        assertThat(deserialized.getPendingReplicationCount()).isEqualTo(3);
        assertThat(deserialized.getPersistenceMode()).isEqualTo("MYSQL_DURABLE");
    }

    @Test
    void testOperationResultSerialization() throws Exception {
        ChatRoomDTO room = new ChatRoomDTO("general", "General Chat", "Main room", Instant.now());
        OperationResult<ChatRoomDTO> result = OperationResult.success("Room retrieved", room);

        OperationResult<ChatRoomDTO> deserialized = roundTrip(result);
        assertThat(deserialized.isSuccess()).isTrue();
        assertThat(deserialized.getMessage()).isEqualTo("Room retrieved");
        assertThat(deserialized.getData()).isEqualTo(room);
    }

    @Test
    void testServerEventDTOSerialization() throws Exception {
        ServerEventDTO original = new ServerEventDTO(
                UUID.randomUUID().toString(),
                Instant.now(),
                EventSeverity.INFO,
                "RMI",
                "Registry bound to port 1099",
                "server-a"
        );

        ServerEventDTO deserialized = roundTrip(original);
        assertThat(deserialized.getSourceComponent()).isEqualTo("RMI");
        assertThat(deserialized.getSeverity()).isEqualTo(EventSeverity.INFO);
    }
}
