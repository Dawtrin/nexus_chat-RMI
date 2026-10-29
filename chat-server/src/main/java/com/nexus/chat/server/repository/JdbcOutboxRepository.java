package com.nexus.chat.server.repository;

import com.nexus.chat.contract.dto.ChatMessageDTO;
import com.nexus.chat.contract.enums.MessageType;
import com.nexus.chat.server.database.DatabaseManager;
import com.nexus.chat.server.model.OutboxItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * JDBC implementation of OutboxRepository with thread-safe in-memory fallback.
 */
public class JdbcOutboxRepository implements OutboxRepository {
    private static final Logger log = LoggerFactory.getLogger(JdbcOutboxRepository.class);

    private final DatabaseManager databaseManager;

    // Resilient memory queue fallback
    private final List<OutboxItem> memoryQueue = new CopyOnWriteArrayList<>();
    private final AtomicLong memoryIdGen = new AtomicLong(1);

    public JdbcOutboxRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public void enqueue(ChatMessageDTO message, String targetPeerId) {
        // Always record in memory queue for resilient fallback
        OutboxItem memoryItem = new OutboxItem(
                memoryIdGen.getAndIncrement(),
                message,
                targetPeerId,
                "PENDING",
                0,
                null,
                null,
                Instant.now()
        );
        memoryQueue.add(memoryItem);

        if (databaseManager.isConnected()) {
            String sql = "INSERT INTO replication_outbox " +
                    "(message_id, room_id, sender_id, sender_name, content, created_at, origin_server_id, origin_sequence, message_type, target_peer_id, status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING')";

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, message.getMessageId());
                ps.setString(2, message.getRoomId());
                ps.setString(3, message.getSenderId());
                ps.setString(4, message.getSenderName());
                ps.setString(5, message.getContent());
                ps.setTimestamp(6, Timestamp.from(message.getCreatedAt()));
                ps.setString(7, message.getOriginServerId());
                ps.setLong(8, message.getOriginSequence());
                ps.setString(9, message.getMessageType().name());
                ps.setString(10, targetPeerId);

                ps.executeUpdate();
                log.debug("Enqueued message {} into SQL replication_outbox for peer {}", message.getMessageId(), targetPeerId);
            } catch (SQLException e) {
                log.warn("Failed enqueueing to SQL outbox, kept in memory fallback: {}", e.getMessage());
            }
        }
    }

    @Override
    public List<OutboxItem> findPending(String targetPeerId, int limit) {
        if (databaseManager.isConnected()) {
            String sql = "SELECT outbox_id, message_id, room_id, sender_id, sender_name, content, created_at, " +
                    "origin_server_id, origin_sequence, message_type, target_peer_id, status, retry_count, last_retry_at, last_error, queued_at " +
                    "FROM replication_outbox " +
                    "WHERE target_peer_id = ? AND status IN ('PENDING', 'FAILED') AND retry_count < 10 " +
                    "ORDER BY outbox_id ASC LIMIT ?";

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, targetPeerId);
                ps.setInt(2, limit);

                try (ResultSet rs = ps.executeQuery()) {
                    List<OutboxItem> results = new ArrayList<>();
                    while (rs.next()) {
                        results.add(mapOutboxItem(rs));
                    }
                    if (!results.isEmpty()) {
                        return results;
                    }
                }
            } catch (SQLException e) {
                log.warn("Failed querying SQL outbox, falling back to memory queue: {}", e.getMessage());
            }
        }

        // Memory fallback
        return memoryQueue.stream()
                .filter(item -> item.getTargetPeerId().equals(targetPeerId)
                        && ("PENDING".equals(item.getStatus()) || "FAILED".equals(item.getStatus()))
                        && item.getRetryCount() < 10)
                .limit(limit)
                .toList();
    }

    @Override
    public void markDelivered(long outboxId) {
        memoryQueue.removeIf(item -> item.getOutboxId() == outboxId);

        if (databaseManager.isConnected()) {
            String sql = "UPDATE replication_outbox SET status = 'DELIVERED' WHERE outbox_id = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, outboxId);
                ps.executeUpdate();
            } catch (SQLException e) {
                log.warn("Failed marking outbox item {} DELIVERED in SQL: {}", outboxId, e.getMessage());
            }
        }
    }

    @Override
    public void markBatchDelivered(List<Long> outboxIds) {
        if (outboxIds == null || outboxIds.isEmpty()) return;

        memoryQueue.removeIf(item -> outboxIds.contains(item.getOutboxId()));

        if (databaseManager.isConnected()) {
            String sql = "UPDATE replication_outbox SET status = 'DELIVERED' WHERE outbox_id = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                conn.setAutoCommit(false);
                for (Long id : outboxIds) {
                    ps.setLong(1, id);
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                log.warn("Failed marking outbox batch delivered in SQL: {}", e.getMessage());
            }
        }
    }

    @Override
    public void markFailed(long outboxId, String error) {
        for (OutboxItem item : memoryQueue) {
            if (item.getOutboxId() == outboxId) {
                item.setStatus("FAILED");
                item.incrementRetry();
                item.setLastError(error);
                break;
            }
        }

        if (databaseManager.isConnected()) {
            String sql = "UPDATE replication_outbox SET status = 'FAILED', retry_count = retry_count + 1, " +
                    "last_retry_at = CURRENT_TIMESTAMP(3), last_error = ? WHERE outbox_id = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, error != null ? error : "Unknown error");
                ps.setLong(2, outboxId);
                ps.executeUpdate();
            } catch (SQLException e) {
                log.warn("Failed updating failed status in SQL outbox: {}", e.getMessage());
            }
        }
    }

    @Override
    public int countPending(String targetPeerId) {
        if (databaseManager.isConnected()) {
            String sql = "SELECT COUNT(*) FROM replication_outbox WHERE target_peer_id = ? AND status IN ('PENDING', 'FAILED')";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, targetPeerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            } catch (SQLException e) {
                log.warn("Failed counting SQL outbox items: {}", e.getMessage());
            }
        }

        return (int) memoryQueue.stream()
                .filter(item -> item.getTargetPeerId().equals(targetPeerId)
                        && ("PENDING".equals(item.getStatus()) || "FAILED".equals(item.getStatus())))
                .count();
    }

    private OutboxItem mapOutboxItem(ResultSet rs) throws SQLException {
        ChatMessageDTO msg = new ChatMessageDTO(
                rs.getString("message_id"),
                rs.getString("room_id"),
                rs.getString("sender_id"),
                rs.getString("sender_name"),
                rs.getString("content"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getString("origin_server_id"),
                rs.getLong("origin_sequence"),
                MessageType.valueOf(rs.getString("message_type"))
        );

        Timestamp lastRetry = rs.getTimestamp("last_retry_at");
        return new OutboxItem(
                rs.getLong("outbox_id"),
                msg,
                rs.getString("target_peer_id"),
                rs.getString("status"),
                rs.getInt("retry_count"),
                lastRetry != null ? lastRetry.toInstant() : null,
                rs.getString("last_error"),
                rs.getTimestamp("queued_at").toInstant()
        );
    }
}
