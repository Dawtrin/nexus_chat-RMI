package com.nexus.chat.contract.dto;

import com.nexus.chat.contract.enums.MessageType;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Immutable chat message data transfer object crossing RMI boundaries.
 */
public class ChatMessageDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String messageId;
    private final String roomId;
    private final String senderId;
    private final String senderName;
    private final String content;
    private final Instant createdAt;
    private final String originServerId;
    private final long originSequence;
    private final MessageType messageType;

    public ChatMessageDTO(String messageId,
                          String roomId,
                          String senderId,
                          String senderName,
                          String content,
                          Instant createdAt,
                          String originServerId,
                          long originSequence,
                          MessageType messageType) {
        this.messageId = Objects.requireNonNull(messageId, "messageId cannot be null");
        this.roomId = Objects.requireNonNull(roomId, "roomId cannot be null");
        this.senderId = Objects.requireNonNull(senderId, "senderId cannot be null");
        this.senderName = Objects.requireNonNull(senderName, "senderName cannot be null");
        this.content = Objects.requireNonNull(content, "content cannot be null");
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.originServerId = Objects.requireNonNull(originServerId, "originServerId cannot be null");
        this.originSequence = originSequence;
        this.messageType = messageType != null ? messageType : MessageType.CHAT;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getOriginServerId() {
        return originServerId;
    }

    public long getOriginSequence() {
        return originSequence;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatMessageDTO that = (ChatMessageDTO) o;
        return Objects.equals(messageId, that.messageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId);
    }

    @Override
    public String toString() {
        return "ChatMessageDTO{" +
                "messageId='" + messageId + '\'' +
                ", roomId='" + roomId + '\'' +
                ", senderName='" + senderName + '\'' +
                ", createdAt=" + createdAt +
                ", originServerId='" + originServerId + '\'' +
                ", originSequence=" + originSequence +
                ", messageType=" + messageType +
                '}';
    }
}
