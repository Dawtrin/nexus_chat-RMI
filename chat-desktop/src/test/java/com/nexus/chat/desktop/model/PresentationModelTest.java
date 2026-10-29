package com.nexus.chat.desktop.model;

import com.nexus.chat.contract.dto.ChatMessageDTO;
import com.nexus.chat.contract.dto.ChatRoomDTO;
import com.nexus.chat.contract.dto.ClientSessionDTO;
import com.nexus.chat.contract.dto.ServerEventDTO;
import com.nexus.chat.contract.enums.EventSeverity;
import com.nexus.chat.contract.enums.MessageType;
import com.nexus.chat.contract.enums.SessionStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PresentationModelTest {

    @Test
    void testChatMessageModel() {
        ChatMessageDTO dto = new ChatMessageDTO(
                UUID.randomUUID().toString(),
                "general",
                "usr-1",
                "Alice",
                "Test presentation message",
                Instant.now(),
                "server-a",
                1L,
                MessageType.CHAT
        );

        ChatMessageModel model = new ChatMessageModel(dto, true);
        assertThat(model.getMessageId()).isEqualTo(dto.getMessageId());
        assertThat(model.getContent()).isEqualTo("Test presentation message");
        assertThat(model.isOwnMessage()).isTrue();
        assertThat(model.getFormattedTime()).isNotEmpty();
    }

    @Test
    void testChatRoomModel() {
        ChatRoomDTO dto = new ChatRoomDTO("dev", "Developer Channel", "Tech discussion", Instant.now());
        ChatRoomModel model = new ChatRoomModel(dto);

        assertThat(model.getRoomId()).isEqualTo("dev");
        assertThat(model.getRoomName()).isEqualTo("Developer Channel");
        assertThat(model.toString()).isEqualTo("# Developer Channel");
    }

    @Test
    void testSessionViewModel() {
        ClientSessionDTO dto = new ClientSessionDTO(
                "s-1",
                "Bob",
                "client-1",
                "192.168.1.150",
                45000,
                Instant.now(),
                Instant.now(),
                SessionStatus.ACTIVE,
                "server-b"
        );

        SessionViewModel vm = new SessionViewModel(dto);
        assertThat(vm.getUsername()).isEqualTo("Bob");
        assertThat(vm.getRemoteIp()).isEqualTo("192.168.1.150");
        assertThat(vm.getStatus()).isEqualTo(SessionStatus.ACTIVE);
    }

    @Test
    void testEventViewModel() {
        ServerEventDTO dto = new ServerEventDTO(
                UUID.randomUUID().toString(),
                Instant.now(),
                EventSeverity.WARN,
                "REPLICATION",
                "Peer offline",
                "server-a"
        );

        EventViewModel vm = new EventViewModel(dto);
        assertThat(vm.getComponent()).isEqualTo("REPLICATION");
        assertThat(vm.getSeverity()).isEqualTo(EventSeverity.WARN);
        assertThat(vm.getMessage()).isEqualTo("Peer offline");
    }
}
