package com.nexus.chat.server.repository;

import com.nexus.chat.contract.dto.ChatMessageDTO;

import java.util.List;

/**
 * Persistence contract for chat message storage, sequence allocation, and timeline queries.
 */
public interface MessageRepository {

    /**
     * Persists a message. Must be idempotent (ignore duplicate messageId or origin sequence).
     *
     * @param message Chat message to store
     * @return The persisted message with its assigned localSequence
     */
    ChatMessageDTO save(ChatMessageDTO message);

    /**
     * Persists a batch of messages idempotently in a single transaction if database is connected.
     *
     * @param messages List of messages to store
     * @return Count of newly inserted messages
     */
    int saveBatch(List<ChatMessageDTO> messages);

    /**
     * Checks if a message with the given unique ID already exists.
     *
     * @param messageId Unique message UUID
     * @return true if exists
     */
    boolean existsById(String messageId);

    /**
     * Retrieves messages for a room with a local monotonic sequence greater than afterLocalSequence.
     * Ensures all messages (both local and replicated) appear in strict arrival order for local clients.
     *
     * @param roomId Target room
     * @param afterLocalSequence Local sequence cutoff
     * @return List of messages in arrival order
     */
    List<ChatMessageDTO> findByRoomAfterSequence(String roomId, long afterLocalSequence);

    /**
     * Retrieves messages originating from a specific server for backfill synchronization.
     *
     * @param originServerId Origin server ID
     * @param afterOriginSequence Origin sequence cutoff
     * @param limit Maximum messages to return
     * @return List of messages in origin sequence order
     */
    List<ChatMessageDTO> findByOriginAfterSequence(String originServerId, long afterOriginSequence, int limit);

    /**
     * Atomically allocates the next sequential number for messages originating on this server.
     *
     * @param originServerId Origin server identifier
     * @return Next sequence number
     */
    long getNextSequence(String originServerId);

    /**
     * Returns the active persistence mode ("MYSQL_DURABLE" or "IN_MEMORY_FALLBACK").
     *
     * @return Persistence mode description
     */
    String getPersistenceMode();
}
