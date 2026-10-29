package com.nexus.chat.server.monitoring;

import com.nexus.chat.contract.dto.ServerEventDTO;
import com.nexus.chat.contract.enums.EventSeverity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Captures operational diagnostic events in memory for admin dashboard inspection.
 */
public class SystemLogService {
    private static final Logger log = LoggerFactory.getLogger(SystemLogService.class);
    private static final int MAX_BUFFERED_EVENTS = 500;

    private final String serverId;
    private final List<ServerEventDTO> events = new CopyOnWriteArrayList<>();

    public SystemLogService(String serverId) {
        this.serverId = serverId;
    }

    public void logEvent(EventSeverity severity, String component, String message) {
        ServerEventDTO event = new ServerEventDTO(
                UUID.randomUUID().toString(),
                Instant.now(),
                severity,
                component,
                message,
                serverId
        );

        events.add(event);
        if (events.size() > MAX_BUFFERED_EVENTS) {
            events.remove(0);
        }

        switch (severity) {
            case INFO -> log.info("[{}] {}", component, message);
            case WARN -> log.warn("[{}] {}", component, message);
            case ERROR -> log.error("[{}] {}", component, message);
        }
    }

    public List<ServerEventDTO> getRecentEvents(int limit) {
        List<ServerEventDTO> copy = new ArrayList<>(events);
        Collections.reverse(copy); // Newest first
        if (limit > 0 && copy.size() > limit) {
            return copy.subList(0, limit);
        }
        return copy;
    }
}
