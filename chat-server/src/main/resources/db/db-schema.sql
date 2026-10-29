-- =============================================================================
-- NEXUS Distributed Chat — Database Schema
-- Compatible with MySQL 8.0+ and MariaDB 10.5+
-- =============================================================================

CREATE TABLE IF NOT EXISTS chat_rooms (
    room_id VARCHAR(100) PRIMARY KEY,
    room_name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_messages (
    message_id CHAR(36) PRIMARY KEY,
    room_id VARCHAR(100) NOT NULL,
    sender_id VARCHAR(100) NOT NULL,
    sender_name VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP(3) NOT NULL,
    origin_server_id VARCHAR(100) NOT NULL,
    origin_sequence BIGINT NOT NULL,
    message_type VARCHAR(30) NOT NULL,
    received_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uq_origin_sequence (origin_server_id, origin_sequence),
    INDEX idx_room_created (room_id, created_at),
    CONSTRAINT fk_message_room FOREIGN KEY (room_id) REFERENCES chat_rooms (room_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS client_sessions (
    session_id VARCHAR(100) PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    client_instance_id VARCHAR(100),
    remote_ip VARCHAR(50),
    remote_port INT,
    connected_at TIMESTAMP(3) NOT NULL,
    last_seen_at TIMESTAMP(3) NOT NULL,
    status VARCHAR(30) NOT NULL,
    connected_server_id VARCHAR(100) NOT NULL,
    INDEX idx_session_status (status),
    INDEX idx_session_last_seen (last_seen_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS replication_state (
    peer_server_id VARCHAR(100) PRIMARY KEY,
    last_success_at TIMESTAMP(3),
    last_sync_status VARCHAR(50),
    last_error TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS server_events (
    event_id VARCHAR(100) PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    source_component VARCHAR(100) NOT NULL,
    event_message TEXT NOT NULL,
    server_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_event_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed default chat rooms if table is empty
INSERT IGNORE INTO chat_rooms (room_id, room_name, description) VALUES
('general', 'General Chat', 'Main discussion channel for all cluster members'),
('announcements', 'Announcements', 'System notices and broadcast updates'),
('random', 'Random Talk', 'Casual discussions and off-topic chat');
