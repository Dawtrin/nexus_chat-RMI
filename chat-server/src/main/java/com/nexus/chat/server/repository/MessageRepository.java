package com.nexus.chat.server.repository;

import com.nexus.chat.contract.dto.ChatMessageDTO;

import java.util.List;

/**
 * Persistence contract for chat message storage and sequence recovery.
 */
public interface MessageRepository {

    /**
     * Persists a message. Must be idempotent (ignore duplicates on messageId or origin sequence).
     *
     * @param message Chat message to store
     */
    void save(ChatMessageDTO message);

    /**
     * Checks if a message with the given unique ID already exists.
     *
     * @param messageId Unique message UUID
     * @return true if exists
     */
    boolean existsById(String messageId);

    /**
     * Retrieves messages for a room with an origin sequence greater than afterSequence.
     *
     * @param roomId Target room
     * @param afterSequence Sequence cutoff
     * @return List of messages in chronological order
     */
    List<ChatMessageDTO> findByRoomAfterSequence(String roomId, long afterSequence);

    /**
     * Retrieves messages originating from a specific server for backfill synchronization.
     *
     * @param originServerId Origin server ID
     * @param afterSequence Sequence cutoff
     * @param limit Maximum messages to return
     * @return List of messages
     */
    List<ChatMessageDTO> findByOriginAfterSequence(String originServerId, long afterSequence, int limit);

    /**
     * Atomically allocates the next sequential number for messages originating on this server.
     *
     * @param originServerId Origin server identifier
     * @return Next sequence number
     */
    long getNextSequence(String originServerId);
}
