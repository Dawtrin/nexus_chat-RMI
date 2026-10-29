package com.nexus.chat.server.repository;

import com.nexus.chat.contract.dto.ChatMessageDTO;
import com.nexus.chat.contract.enums.MessageType;
import com.nexus.chat.server.database.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * JDBC implementation of MessageRepository with transaction support,
 * monotonic arrival sequencing, and resilient in-memory fallback.
 */
public class JdbcMessageRepository implements MessageRepository {
    private static final Logger log = LoggerFactory.getLogger(JdbcMessageRepository.class);

    private final DatabaseManager databaseManager;

    // Resilient in-memory storage (used when DB is offline and for caching)
    private final Map<String, ChatMessageDTO> memoryStore = new ConcurrentHashMap<>();
    private final List<ChatMessageDTO> chronologicalList = new CopyOnWriteArrayList<>();
    private final Map<String, AtomicLong> originSequenceCounters = new ConcurrentHashMap<>();
    private final AtomicLong localSequenceGenerator = new AtomicLong(1);

    public JdbcMessageRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public ChatMessageDTO save(ChatMessageDTO message) {
        long localSeq = message.getLocalSequence() > 0 ? message.getLocalSequence() : localSequenceGenerator.getAndIncrement();

        ChatMessageDTO toStore = new ChatMessageDTO(
                message.getMessageId(),
                message.getRoomId(),
                message.getSenderId(),
                message.getSenderName(),
                message.getContent(),
                message.getCreatedAt(),
                message.getOriginServerId(),
                message.getOriginSequence(),
                message.getMessageType(),
                localSeq
        );

        // Update local memory store idempotently
        if (memoryStore.putIfAbsent(toStore.getMessageId(), toStore) == null) {
            chronologicalList.add(toStore);
        }

        // Update sequence counter for origin server
        originSequenceCounters.computeIfAbsent(toStore.getOriginServerId(), k -> new AtomicLong(0))
                .updateAndGet(curr -> Math.max(curr, toStore.getOriginSequence()));

        // If database is available, persist via JDBC prepared statement with auto-commit handling
        if (databaseManager.isConnected()) {
            String sql = "INSERT INTO chat_messages " +
                    "(message_id, room_id, sender_id, sender_name, content, created_at, origin_server_id, origin_sequence, message_type) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE message_id = message_id";

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                ps.setString(1, toStore.getMessageId());
                ps.setString(2, toStore.getRoomId());
                ps.setString(3, toStore.getSenderId());
                ps.setString(4, toStore.getSenderName());
                ps.setString(5, toStore.getContent());
                ps.setTimestamp(6, Timestamp.from(toStore.getCreatedAt()));
                ps.setString(7, toStore.getOriginServerId());
                ps.setLong(8, toStore.getOriginSequence());
                ps.setString(9, toStore.getMessageType().name());

                ps.executeUpdate();

                // If DB generated a new local_sequence, update
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        long dbLocalSeq = generatedKeys.getLong(1);
                        if (dbLocalSeq > 0) {
                            localSequenceGenerator.updateAndGet(curr -> Math.max(curr, dbLocalSeq + 1));
                        }
                    }
                }
                log.debug("Persisted message {} to MySQL database (durable)", toStore.getMessageId());
            } catch (SQLException e) {
                log.error("Failed persisting message to MySQL, retained in memory fallback: {}", e.getMessage());
            }
        }

        return toStore;
    }

    @Override
    public int saveBatch(List<ChatMessageDTO> messages) {
        if (messages == null || messages.isEmpty()) return 0;

        int inserted = 0;
        for (ChatMessageDTO msg : messages) {
            if (!existsById(msg.getMessageId())) {
                save(msg);
                inserted++;
            }
        }
        return inserted;
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
                log.warn("Error checking message existence in SQL: {}", e.getMessage());
            }
        }
        return false;
    }

    @Override
    public List<ChatMessageDTO> findByRoomAfterSequence(String roomId, long afterLocalSequence) {
        // Query from DB if connected
        if (databaseManager.isConnected()) {
            String sql = "SELECT local_sequence, message_id, room_id, sender_id, sender_name, content, created_at, origin_server_id, origin_sequence, message_type " +
                    "FROM chat_messages " +
                    "WHERE room_id = ? AND local_sequence > ? " +
                    "ORDER BY local_sequence ASC";

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, roomId);
                ps.setLong(2, afterLocalSequence);

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

        // Fallback to in-memory store using monotonic local sequence
        return chronologicalList.stream()
                .filter(m -> m.getRoomId().equals(roomId) && m.getLocalSequence() > afterLocalSequence)
                .sorted(Comparator.comparingLong(ChatMessageDTO::getLocalSequence))
                .toList();
    }

    @Override
    public List<ChatMessageDTO> findByOriginAfterSequence(String originServerId, long afterOriginSequence, int limit) {
        if (databaseManager.isConnected()) {
            String sql = "SELECT local_sequence, message_id, room_id, sender_id, sender_name, content, created_at, origin_server_id, origin_sequence, message_type " +
                    "FROM chat_messages " +
                    "WHERE origin_server_id = ? AND origin_sequence > ? " +
                    "ORDER BY origin_sequence ASC LIMIT ?";

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, originServerId);
                ps.setLong(2, afterOriginSequence);
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
                .filter(m -> m.getOriginServerId().equals(originServerId) && m.getOriginSequence() > afterOriginSequence)
                .sorted(Comparator.comparingLong(ChatMessageDTO::getOriginSequence))
                .limit(limit)
                .toList();
    }

    @Override
    public long getNextSequence(String originServerId) {
        return originSequenceCounters.computeIfAbsent(originServerId, k -> new AtomicLong(0))
                .incrementAndGet();
    }

    @Override
    public String getPersistenceMode() {
        return databaseManager.isConnected() ? "MYSQL_DURABLE" : "IN_MEMORY_FALLBACK";
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
                MessageType.valueOf(rs.getString("message_type")),
                rs.getLong("local_sequence")
        );
    }
}
