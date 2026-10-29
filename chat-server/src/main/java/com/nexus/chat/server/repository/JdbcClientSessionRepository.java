package com.nexus.chat.server.repository;

import com.nexus.chat.contract.dto.ClientSessionDTO;
import com.nexus.chat.contract.enums.SessionStatus;
import com.nexus.chat.server.database.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repository that persists client sessions to MySQL while maintaining an in-memory
 * concurrent cache for instant lookup and heartbeat tracking.
 */
public class JdbcClientSessionRepository implements ClientSessionRepository {
    private static final Logger log = LoggerFactory.getLogger(JdbcClientSessionRepository.class);

    private final DatabaseManager databaseManager;
    private final Map<String, ClientSessionDTO> memorySessions = new ConcurrentHashMap<>();

    public JdbcClientSessionRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public void save(ClientSessionDTO session) {
        memorySessions.put(session.getSessionId(), session);

        if (databaseManager.isConnected()) {
            String sql = "INSERT INTO client_sessions " +
                    "(session_id, username, client_instance_id, remote_ip, remote_port, connected_at, last_seen_at, status, connected_server_id) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE last_seen_at = VALUES(last_seen_at), status = VALUES(status)";

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, session.getSessionId());
                ps.setString(2, session.getUsername());
                ps.setString(3, session.getClientInstanceId());
                ps.setString(4, session.getRemoteIp());
                ps.setInt(5, session.getRemotePort());
                ps.setTimestamp(6, Timestamp.from(session.getConnectedAt()));
                ps.setTimestamp(7, Timestamp.from(session.getLastSeenAt()));
                ps.setString(8, session.getStatus().name());
                ps.setString(9, session.getConnectedServerId());

                ps.executeUpdate();
            } catch (SQLException e) {
                log.warn("Failed saving session to SQL database: {}", e.getMessage());
            }
        }
    }

    @Override
    public Optional<ClientSessionDTO> findById(String sessionId) {
        return Optional.ofNullable(memorySessions.get(sessionId));
    }

    @Override
    public List<ClientSessionDTO> findAll() {
        return new ArrayList<>(memorySessions.values());
    }

    @Override
    public List<ClientSessionDTO> findActiveSessions() {
        return memorySessions.values().stream()
                .filter(s -> s.getStatus() == SessionStatus.ACTIVE || s.getStatus() == SessionStatus.STALE)
                .toList();
    }

    @Override
    public void updateLastSeen(String sessionId, Instant now) {
        memorySessions.computeIfPresent(sessionId, (id, old) -> {
            ClientSessionDTO updated = new ClientSessionDTO(
                    old.getSessionId(),
                    old.getUsername(),
                    old.getClientInstanceId(),
                    old.getRemoteIp(),
                    old.getRemotePort(),
                    old.getConnectedAt(),
                    now,
                    SessionStatus.ACTIVE,
                    old.getConnectedServerId()
            );

            if (databaseManager.isConnected()) {
                String sql = "UPDATE client_sessions SET last_seen_at = ?, status = 'ACTIVE' WHERE session_id = ?";
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setTimestamp(1, Timestamp.from(now));
                    ps.setString(2, sessionId);
                    ps.executeUpdate();
                } catch (SQLException e) {
                    log.debug("Failed updating session last_seen in SQL: {}", e.getMessage());
                }
            }

            return updated;
        });
    }

    @Override
    public void updateStatus(String sessionId, SessionStatus status) {
        memorySessions.computeIfPresent(sessionId, (id, old) -> {
            ClientSessionDTO updated = new ClientSessionDTO(
                    old.getSessionId(),
                    old.getUsername(),
                    old.getClientInstanceId(),
                    old.getRemoteIp(),
                    old.getRemotePort(),
                    old.getConnectedAt(),
                    old.getLastSeenAt(),
                    status,
                    old.getConnectedServerId()
            );

            if (databaseManager.isConnected()) {
                String sql = "UPDATE client_sessions SET status = ? WHERE session_id = ?";
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, status.name());
                    ps.setString(2, sessionId);
                    ps.executeUpdate();
                } catch (SQLException e) {
                    log.debug("Failed updating session status in SQL: {}", e.getMessage());
                }
            }

            return updated;
        });
    }

    @Override
    public void remove(String sessionId) {
        memorySessions.remove(sessionId);
        if (databaseManager.isConnected()) {
            String sql = "DELETE FROM client_sessions WHERE session_id = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, sessionId);
                ps.executeUpdate();
            } catch (SQLException e) {
                log.warn("Failed deleting session from SQL: {}", e.getMessage());
            }
        }
    }
}
