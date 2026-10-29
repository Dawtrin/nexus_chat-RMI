package com.nexus.chat.server.service;

import com.nexus.chat.contract.dto.ClientSessionDTO;
import com.nexus.chat.contract.enums.EventSeverity;
import com.nexus.chat.contract.enums.SessionStatus;
import com.nexus.chat.server.monitoring.SystemLogService;
import com.nexus.chat.server.repository.ClientSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages client session lifecycle, heartbeats, and liveness reaping.
 */
public class ClientSessionManager implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(ClientSessionManager.class);

    private static final long STALE_TIMEOUT_SECONDS = 30;
    private static final long DISCONNECT_TIMEOUT_SECONDS = 60;

    private final String serverId;
    private final ClientSessionRepository sessionRepository;
    private final SystemLogService logService;
    private final ScheduledExecutorService reaperExecutor;

    public ClientSessionManager(String serverId, ClientSessionRepository sessionRepository, SystemLogService logService) {
        this.serverId = serverId;
        this.sessionRepository = sessionRepository;
        this.logService = logService;
        this.reaperExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "nexus-session-reaper");
            t.setDaemon(true);
            return t;
        });

        startReaper();
    }

    private void startReaper() {
        reaperExecutor.scheduleAtFixedRate(this::reapInactiveSessions, 10, 5, TimeUnit.SECONDS);
    }

    private void reapInactiveSessions() {
        try {
            Instant now = Instant.now();
            List<ClientSessionDTO> all = sessionRepository.findAll();

            for (ClientSessionDTO session : all) {
                if (session.getStatus() == SessionStatus.DISCONNECTED) {
                    continue;
                }

                long inactiveSeconds = Duration.between(session.getLastSeenAt(), now).getSeconds();
                if (inactiveSeconds > DISCONNECT_TIMEOUT_SECONDS) {
                    sessionRepository.updateStatus(session.getSessionId(), SessionStatus.DISCONNECTED);
                    logService.logEvent(EventSeverity.WARN, "SESSION",
                            String.format("Session %s (%s) marked DISCONNECTED after %ds inactivity",
                                    session.getSessionId(), session.getUsername(), inactiveSeconds));
                } else if (inactiveSeconds > STALE_TIMEOUT_SECONDS && session.getStatus() == SessionStatus.ACTIVE) {
                    sessionRepository.updateStatus(session.getSessionId(), SessionStatus.STALE);
                    logService.logEvent(EventSeverity.WARN, "SESSION",
                            String.format("Session %s (%s) marked STALE after %ds inactivity",
                                    session.getSessionId(), session.getUsername(), inactiveSeconds));
                }
            }
        } catch (Exception e) {
            log.error("Error in session reaper: {}", e.getMessage());
        }
    }

    public ClientSessionDTO registerSession(String username, String clientInstanceId, String remoteIp, int remotePort) {
        String sessionId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        ClientSessionDTO session = new ClientSessionDTO(
                sessionId,
                username,
                clientInstanceId,
                remoteIp,
                remotePort,
                now,
                now,
                SessionStatus.ACTIVE,
                serverId
        );

        sessionRepository.save(session);
        logService.logEvent(EventSeverity.INFO, "SESSION",
                String.format("Client connected: user='%s', session=%s, remoteIp=%s", username, sessionId, remoteIp));
        return session;
    }

    public boolean recordHeartbeat(String sessionId) {
        Optional<ClientSessionDTO> existing = sessionRepository.findById(sessionId);
        if (existing.isPresent()) {
            sessionRepository.updateLastSeen(sessionId, Instant.now());
            return true;
        }
        return false;
    }

    public void disconnect(String sessionId) {
        Optional<ClientSessionDTO> existing = sessionRepository.findById(sessionId);
        if (existing.isPresent()) {
            sessionRepository.updateStatus(sessionId, SessionStatus.DISCONNECTED);
            logService.logEvent(EventSeverity.INFO, "SESSION",
                    String.format("Client explicitly disconnected: user='%s', session=%s",
                            existing.get().getUsername(), sessionId));
        }
    }

    public Optional<ClientSessionDTO> getValidSession(String sessionId) {
        return sessionRepository.findById(sessionId)
                .filter(s -> s.getStatus() == SessionStatus.ACTIVE || s.getStatus() == SessionStatus.STALE);
    }

    public List<ClientSessionDTO> getAllSessions() {
        return sessionRepository.findAll();
    }

    public int getActiveSessionCount() {
        return sessionRepository.findActiveSessions().size();
    }

    @Override
    public void close() {
        reaperExecutor.shutdownNow();
    }
}
