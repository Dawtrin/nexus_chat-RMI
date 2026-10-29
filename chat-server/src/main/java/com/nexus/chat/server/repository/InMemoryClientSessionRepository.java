package com.nexus.chat.server.repository;

import com.nexus.chat.contract.dto.ClientSessionDTO;
import com.nexus.chat.contract.enums.SessionStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-performance concurrent memory repository for tracking client sessions.
 */
public class InMemoryClientSessionRepository implements ClientSessionRepository {
    private final Map<String, ClientSessionDTO> sessions = new ConcurrentHashMap<>();

    @Override
    public void save(ClientSessionDTO session) {
        sessions.put(session.getSessionId(), session);
    }

    @Override
    public Optional<ClientSessionDTO> findById(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    @Override
    public List<ClientSessionDTO> findAll() {
        return new ArrayList<>(sessions.values());
    }

    @Override
    public List<ClientSessionDTO> findActiveSessions() {
        return sessions.values().stream()
                .filter(s -> s.getStatus() == SessionStatus.ACTIVE || s.getStatus() == SessionStatus.IDLE)
                .toList();
    }

    @Override
    public void updateLastSeen(String sessionId, Instant now) {
        sessions.computeIfPresent(sessionId, (id, old) -> new ClientSessionDTO(
                old.getSessionId(),
                old.getUsername(),
                old.getClientInstanceId(),
                old.getRemoteIp(),
                old.getRemotePort(),
                old.getConnectedAt(),
                now,
                SessionStatus.ACTIVE,
                old.getConnectedServerId()
        ));
    }

    @Override
    public void updateStatus(String sessionId, SessionStatus status) {
        sessions.computeIfPresent(sessionId, (id, old) -> new ClientSessionDTO(
                old.getSessionId(),
                old.getUsername(),
                old.getClientInstanceId(),
                old.getRemoteIp(),
                old.getRemotePort(),
                old.getConnectedAt(),
                old.getLastSeenAt(),
                status,
                old.getConnectedServerId()
        ));
    }

    @Override
    public void remove(String sessionId) {
        sessions.remove(sessionId);
    }
}
