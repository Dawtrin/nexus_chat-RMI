package com.nexus.chat.server.repository;

import com.nexus.chat.contract.dto.ClientSessionDTO;
import com.nexus.chat.contract.enums.SessionStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for tracking client session states.
 */
public interface ClientSessionRepository {
    void save(ClientSessionDTO session);
    Optional<ClientSessionDTO> findById(String sessionId);
    List<ClientSessionDTO> findAll();
    List<ClientSessionDTO> findActiveSessions();
    void updateLastSeen(String sessionId, Instant now);
    void updateStatus(String sessionId, SessionStatus status);
    void remove(String sessionId);
}
