package com.nexus.chat.server.repository;

import com.nexus.chat.contract.dto.ChatMessageDTO;
import com.nexus.chat.server.model.OutboxItem;

import java.util.List;

/**
 * Persistence contract for the replication outbox queue.
 */
public interface OutboxRepository {

    /**
     * Enqueues a message for peer replication.
     *
     * @param message Message to replicate
     * @param targetPeerId Target peer server identifier
     */
    void enqueue(ChatMessageDTO message, String targetPeerId);

    /**
     * Retrieves pending or retryable outbox items for the target peer.
     *
     * @param targetPeerId Target peer server ID
     * @param limit Maximum batch size
     * @return List of pending outbox items
     */
    List<OutboxItem> findPending(String targetPeerId, int limit);

    /**
     * Marks an outbox item as successfully delivered.
     *
     * @param outboxId Outbox item ID
     */
    void markDelivered(long outboxId);

    /**
     * Marks a batch of outbox items as delivered.
     *
     * @param outboxIds List of outbox item IDs
     */
    void markBatchDelivered(List<Long> outboxIds);

    /**
     * Marks an outbox item as failed and increments its retry counter.
     *
     * @param outboxId Outbox item ID
     * @param error Error message or reason
     */
    void markFailed(long outboxId, String error);

    /**
     * Returns the count of pending replication items for the target peer.
     *
     * @param targetPeerId Target peer ID
     * @return Number of pending items
     */
    int countPending(String targetPeerId);
}
