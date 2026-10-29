package com.nexus.chat.contract.dto;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Chat room representation.
 */
public class ChatRoomDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String roomId;
    private final String roomName;
    private final String description;
    private final Instant createdAt;

    public ChatRoomDTO(String roomId, String roomName, String description, Instant createdAt) {
        this.roomId = Objects.requireNonNull(roomId, "roomId cannot be null");
        this.roomName = Objects.requireNonNull(roomName, "roomName cannot be null");
        this.description = description != null ? description : "";
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public String getRoomId() {
        return roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatRoomDTO that = (ChatRoomDTO) o;
        return Objects.equals(roomId, that.roomId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roomId);
    }

    @Override
    public String toString() {
        return "ChatRoomDTO{" +
                "roomId='" + roomId + '\'' +
                ", roomName='" + roomName + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
