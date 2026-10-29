package com.nexus.chat.server.repository;

import com.nexus.chat.contract.dto.ChatMessageDTO;
import com.nexus.chat.contract.enums.MessageType;
import com.nexus.chat.server.database.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * JDBC implementation of MessageRepository with resilient in-memory fallback
 * when MySQL/MariaDB is not connected.
 */
public class JdbcMessageRepository implements MessageRepository {
    private static final Logger log = LoggerFactory.getLogger(JdbcMessageRepository.class);

    private final DatabaseManager databaseManager;

    // Resilient in-memory storage (used when DB is offline and for instant lookup)
    private final Map<String, ChatMessageDTO> memoryStore = new ConcurrentHashMap<>();
    private final List<ChatMessageDTO> chronologicalList = new CopyOnWriteArrayList<>();
    private final Map<String, AtomicLong> sequenceCounters = new ConcurrentHashMap<>();

    public JdbcMessageRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public void save(ChatMessageDTO message) {
        // Update local memory store idempotently
        if (memoryStore.putIfAbsent(message.getMessageId(), message) == null) {
            chronologicalList.add(message);
        }

        // Update sequence counter
        sequenceCounters.computeIfAbsent(message.getOriginServerId(), k -> new AtomicLong(0))
                .updateAndGet(curr -> Math.max(curr, message.getOriginSequence()));

        // If database is available, persist via JDBC prepared statement
        if (databaseManager.isConnected()) {
            String sql = "INSERT INTO chat_messages " +
                    "(message_id, room_id, sender_id, sender_name, content, created_at, origin_server_id, origin_sequence, message_type) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE message_id = message_id";

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

                ps.executeUpdate();
                log.debug("Persisted message {} to database", message.getMessageId());
            } catch (SQLException e) {
                log.error("Failed persisting message to database, kept in memory fallback: {}", e.getMessage());
            }
        }
    }

    @Override
    public boolean existsById(String messageId) {
        if (memoryStore.containsKey(messageId)) {
            return true;
        }

        if (databaseManager.isConnected()) {
            String sql = "SELECT 1 FROM chat_messages WHERE message_id = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, messageId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            } catch (SQLException e) {
                log.error("Error checking message existence: {}", e.getMessage());
            }
        }
        return false;
    }

    @Override
    public List<ChatMessageDTO> findByRoomAfterSequence(String roomId, long afterSequence) {
        // Query from DB if connected
        if (databaseManager.isConnected()) {
            String sql = "SELECT message_id, room_id, sender_id, sender_name, content, created_at, origin_server_id, origin_sequence, message_type " +
                    "FROM chat_messages " +
                    "WHERE room_id = ? AND origin_sequence > ? " +
                    "ORDER BY created_at ASC";

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, roomId);
                ps.setLong(2, afterSequence);

                try (ResultSet rs = ps.executeQuery()) {
                    List<ChatMessageDTO> results = new ArrayList<>();
                    while (rs.next()) {
                        results.add(mapRow(rs));
                    }
                    return results;
                }
            } catch (SQLException e) {
                log.warn("Database query failed, falling back to memory store: {}", e.getMessage());
            }
        }

        // Fallback to in-memory store
        return chronologicalList.stream()
                .filter(m -> m.getRoomId().equals(roomId) && m.getOriginSequence() > afterSequence)
                .sorted(Comparator.comparing(ChatMessageDTO::getCreatedAt))
                .toList();
    }

    @Override
    public List<ChatMessageDTO> findByOriginAfterSequence(String originServerId, long afterSequence, int limit) {
        if (databaseManager.isConnected()) {
            String sql = "SELECT message_id, room_id, sender_id, sender_name, content, created_at, origin_server_id, origin_sequence, message_type " +
                    "FROM chat_messages " +
                    "WHERE origin_server_id = ? AND origin_sequence > ? " +
                    "ORDER BY origin_sequence ASC LIMIT ?";

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, originServerId);
                ps.setLong(2, afterSequence);
                ps.setInt(3, limit);

                try (ResultSet rs = ps.executeQuery()) {
                    List<ChatMessageDTO> results = new ArrayList<>();
                    while (rs.next()) {
                        results.add(mapRow(rs));
                    }
                    return results;
                }
            } catch (SQLException e) {
                log.warn("Database query for origin messages failed: {}", e.getMessage());
            }
        }

        return chronologicalList.stream()
                .filter(m -> m.getOriginServerId().equals(originServerId) && m.getOriginSequence() > afterSequence)
                .sorted(Comparator.comparingLong(ChatMessageDTO::getOriginSequence))
                .limit(limit)
                .toList();
    }

    @Override
    public long getNextSequence(String originServerId) {
        return sequenceCounters.computeIfAbsent(originServerId, k -> new AtomicLong(0))
                .incrementAndGet();
    }

    private ChatMessageDTO mapRow(ResultSet rs) throws SQLException {
        return new ChatMessageDTO(
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
    }
}
