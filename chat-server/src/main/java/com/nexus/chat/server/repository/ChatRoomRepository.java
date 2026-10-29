package com.nexus.chat.server.repository;

import com.nexus.chat.contract.dto.ChatRoomDTO;

import java.util.List;
import java.util.Optional;

/**
 * Persistence contract for chat room catalog.
 */
public interface ChatRoomRepository {
    List<ChatRoomDTO> findAll();
    Optional<ChatRoomDTO> findById(String roomId);
    void save(ChatRoomDTO room);
}
