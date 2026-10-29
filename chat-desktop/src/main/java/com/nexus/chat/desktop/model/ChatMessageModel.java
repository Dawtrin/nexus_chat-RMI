package com.nexus.chat.desktop.model;

import com.nexus.chat.contract.dto.ChatMessageDTO;
import com.nexus.chat.contract.enums.MessageType;
import javafx.beans.property.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Observable JavaFX model representing a message in the chat timeline.
 */
public class ChatMessageModel {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final StringProperty messageId = new SimpleStringProperty();
    private final StringProperty roomId = new SimpleStringProperty();
    private final StringProperty senderId = new SimpleStringProperty();
    private final StringProperty senderName = new SimpleStringProperty();
    private final StringProperty content = new SimpleStringProperty();
    private final ObjectProperty<Instant> createdAt = new SimpleObjectProperty<>();
    private final StringProperty originServerId = new SimpleStringProperty();
    private final LongProperty originSequence = new SimpleLongProperty();
    private final ObjectProperty<MessageType> messageType = new SimpleObjectProperty<>();
    private final BooleanProperty ownMessage = new SimpleBooleanProperty();

    public ChatMessageModel(ChatMessageDTO dto, boolean isOwnMessage) {
        this.messageId.set(dto.getMessageId());
        this.roomId.set(dto.getRoomId());
        this.senderId.set(dto.getSenderId());
        this.senderName.set(dto.getSenderName());
        this.content.set(dto.getContent());
        this.createdAt.set(dto.getCreatedAt());
        this.originServerId.set(dto.getOriginServerId());
        this.originSequence.set(dto.getOriginSequence());
        this.messageType.set(dto.getMessageType());
        this.ownMessage.set(isOwnMessage);
    }

    public String getFormattedTime() {
        return createdAt.get() != null ? TIME_FORMATTER.format(createdAt.get()) : "";
    }

    public String getMessageId() { return messageId.get(); }
    public StringProperty messageIdProperty() { return messageId; }

    public String getRoomId() { return roomId.get(); }
    public StringProperty roomIdProperty() { return roomId; }

    public String getSenderId() { return senderId.get(); }
    public StringProperty senderIdProperty() { return senderId; }

    public String getSenderName() { return senderName.get(); }
    public StringProperty senderNameProperty() { return senderName; }

    public String getContent() { return content.get(); }
    public StringProperty contentProperty() { return content; }

    public Instant getCreatedAt() { return createdAt.get(); }
    public ObjectProperty<Instant> createdAtProperty() { return createdAt; }

    public String getOriginServerId() { return originServerId.get(); }
    public StringProperty originServerIdProperty() { return originServerId; }

    public long getOriginSequence() { return originSequence.get(); }
    public LongProperty originSequenceProperty() { return originSequence; }

    public MessageType getMessageType() { return messageType.get(); }
    public ObjectProperty<MessageType> messageTypeProperty() { return messageType; }

    public boolean isOwnMessage() { return ownMessage.get(); }
    public BooleanProperty ownMessageProperty() { return ownMessage; }
}
