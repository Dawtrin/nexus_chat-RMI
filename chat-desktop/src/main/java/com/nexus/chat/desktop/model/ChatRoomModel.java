package com.nexus.chat.desktop.model;

import com.nexus.chat.contract.dto.ChatRoomDTO;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Observable model representing a chat room in the sidebar.
 */
public class ChatRoomModel {
    private final StringProperty roomId = new SimpleStringProperty();
    private final StringProperty roomName = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final IntegerProperty unreadCount = new SimpleIntegerProperty(0);

    public ChatRoomModel(ChatRoomDTO dto) {
        this.roomId.set(dto.getRoomId());
        this.roomName.set(dto.getRoomName());
        this.description.set(dto.getDescription());
    }

    public String getRoomId() { return roomId.get(); }
    public StringProperty roomIdProperty() { return roomId; }

    public String getRoomName() { return roomName.get(); }
    public StringProperty roomNameProperty() { return roomName; }

    public String getDescription() { return description.get(); }
    public StringProperty descriptionProperty() { return description; }

    public int getUnreadCount() { return unreadCount.get(); }
    public IntegerProperty unreadCountProperty() { return unreadCount; }

    @Override
    public String toString() {
        return "# " + getRoomName();
    }
}
