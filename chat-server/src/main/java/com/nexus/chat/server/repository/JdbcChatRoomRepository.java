package com.nexus.chat.server.repository;

import com.nexus.chat.contract.dto.ChatRoomDTO;
import com.nexus.chat.server.database.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JDBC implementation of ChatRoomRepository with default seeded rooms.
 */
public class JdbcChatRoomRepository implements ChatRoomRepository {
    private static final Logger log = LoggerFactory.getLogger(JdbcChatRoomRepository.class);

    private final DatabaseManager databaseManager;
    private final Map<String, ChatRoomDTO> memoryRooms = new ConcurrentHashMap<>();

    public JdbcChatRoomRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        seedDefaultRooms();
    }

    private void seedDefaultRooms() {
        save(new ChatRoomDTO("general", "General Chat", "Main discussion channel for all cluster members", Instant.now()));
        save(new ChatRoomDTO("announcements", "Announcements", "System notices and broadcast updates", Instant.now()));
        save(new ChatRoomDTO("random", "Random Talk", "Casual discussions and off-topic chat", Instant.now()));
    }

    @Override
    public List<ChatRoomDTO> findAll() {
        if (databaseManager.isConnected()) {
            String sql = "SELECT room_id, room_name, description, created_at FROM chat_rooms ORDER BY room_name ASC";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                List<ChatRoomDTO> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
                if (!list.isEmpty()) {
                    return list;
                }
            } catch (SQLException e) {
                log.warn("Failed querying chat_rooms from database: {}", e.getMessage());
            }
        }
        return new ArrayList<>(memoryRooms.values());
    }

    @Override
    public Optional<ChatRoomDTO> findById(String roomId) {
        if (databaseManager.isConnected()) {
            String sql = "SELECT room_id, room_name, description, created_at FROM chat_rooms WHERE room_id = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, roomId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapRow(rs));
                    }
                }
            } catch (SQLException e) {
                log.warn("Failed querying chat room {}: {}", roomId, e.getMessage());
            }
        }
        return Optional.ofNullable(memoryRooms.get(roomId));
    }

    @Override
    public void save(ChatRoomDTO room) {
        memoryRooms.put(room.getRoomId(), room);

        if (databaseManager.isConnected()) {
            String sql = "INSERT INTO chat_rooms (room_id, room_name, description, created_at) " +
                    "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE room_name = VALUES(room_name), description = VALUES(description)";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, room.getRoomId());
                ps.setString(2, room.getRoomName());
                ps.setString(3, room.getDescription());
                ps.setTimestamp(4, Timestamp.from(room.getCreatedAt()));
                ps.executeUpdate();
            } catch (SQLException e) {
                log.warn("Failed saving room {} to database: {}", room.getRoomId(), e.getMessage());
            }
        }
    }

    private ChatRoomDTO mapRow(ResultSet rs) throws SQLException {
        return new ChatRoomDTO(
                rs.getString("room_id"),
                rs.getString("room_name"),
                rs.getString("description"),
                rs.getTimestamp("created_at").toInstant()
        );
    }
}
