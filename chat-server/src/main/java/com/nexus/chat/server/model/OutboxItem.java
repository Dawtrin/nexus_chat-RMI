package com.nexus.chat.server.model;

import com.nexus.chat.contract.dto.ChatMessageDTO;

import java.time.Instant;

/**
 * Represents an item in the replication outbox queued for asynchronous delivery to a peer server.
 */
public class OutboxItem {
    private final long outboxId;
    private final ChatMessageDTO message;
    private final String targetPeerId;
    private String status;
    private int retryCount;
    private Instant lastRetryAt;
    private String lastError;
    private final Instant queuedAt;

    public OutboxItem(long outboxId,
                      ChatMessageDTO message,
                      String targetPeerId,
                      String status,
                      int retryCount,
                      Instant lastRetryAt,
                      String lastError,
                      Instant queuedAt) {
        this.outboxId = outboxId;
        this.message = message;
        this.targetPeerId = targetPeerId;
        this.status = status;
        this.retryCount = retryCount;
        this.lastRetryAt = lastRetryAt;
        this.lastError = lastError;
        this.queuedAt = queuedAt != null ? queuedAt : Instant.now();
    }

    public long getOutboxId() {
        return outboxId;
    }

    public ChatMessageDTO getMessage() {
        return message;
    }

    public String getTargetPeerId() {
        return targetPeerId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void incrementRetry() {
        this.retryCount++;
        this.lastRetryAt = Instant.now();
    }

    public Instant getLastRetryAt() {
        return lastRetryAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public Instant getQueuedAt() {
        return queuedAt;
    }
}
