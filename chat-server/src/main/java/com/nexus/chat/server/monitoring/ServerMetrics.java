package com.nexus.chat.server.monitoring;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks runtime operational metrics such as message counts and server uptime.
 */
public class ServerMetrics {
    private final Instant startedAt = Instant.now();
    private final AtomicLong messagesProcessed = new AtomicLong(0);
    private final AtomicLong messagesReplicated = new AtomicLong(0);
    private final AtomicLong replicationErrors = new AtomicLong(0);

    public Instant getStartedAt() {
        return startedAt;
    }

    public long getUptimeSeconds() {
        return Duration.between(startedAt, Instant.now()).getSeconds();
    }

    public void incrementMessagesProcessed() {
        messagesProcessed.incrementAndGet();
    }

    public long getMessagesProcessed() {
        return messagesProcessed.get();
    }

    public void incrementMessagesReplicated() {
        messagesReplicated.incrementAndGet();
    }

    public long getMessagesReplicated() {
        return messagesReplicated.get();
    }

    public void incrementReplicationErrors() {
        replicationErrors.incrementAndGet();
    }

    public long getReplicationErrors() {
        return replicationErrors.get();
    }
}
