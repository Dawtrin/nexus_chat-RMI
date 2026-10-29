package com.nexus.chat.server;

import com.nexus.chat.contract.enums.EventSeverity;
import com.nexus.chat.server.config.ServerConfig;
import com.nexus.chat.server.database.DatabaseManager;
import com.nexus.chat.server.monitoring.ServerMetrics;
import com.nexus.chat.server.monitoring.SystemLogService;
import com.nexus.chat.server.repository.ChatRoomRepository;
import com.nexus.chat.server.repository.ClientSessionRepository;
import com.nexus.chat.server.repository.JdbcChatRoomRepository;
import com.nexus.chat.server.repository.JdbcClientSessionRepository;
import com.nexus.chat.server.repository.JdbcMessageRepository;
import com.nexus.chat.server.repository.JdbcOutboxRepository;
import com.nexus.chat.server.repository.MessageRepository;
import com.nexus.chat.server.repository.OutboxRepository;
import com.nexus.chat.server.rmi.RmiRegistryManager;
import com.nexus.chat.server.service.ChatServiceImpl;
import com.nexus.chat.server.service.ClientSessionManager;
import com.nexus.chat.server.service.MonitoringServiceImpl;
import com.nexus.chat.server.service.PeerReplicationManager;
import com.nexus.chat.server.service.ReplicationServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main application bootstrap for NEXUS Distributed Chat Server.
 */
public class ServerApplication {
    private static final Logger log = LoggerFactory.getLogger(ServerApplication.class);

    public static void main(String[] args) {
        printBanner();

        try {
            ServerConfig config = new ServerConfig(args);
            ServerMetrics metrics = new ServerMetrics();
            SystemLogService logService = new SystemLogService(config.getServerId());

            logService.logEvent(EventSeverity.INFO, "BOOTSTRAP",
                    String.format("Starting server node '%s'...", config.getServerId()));

            // Database setup
            DatabaseManager databaseManager = new DatabaseManager(config.getDatabaseConfig());

            // Repositories
            MessageRepository messageRepository = new JdbcMessageRepository(databaseManager);
            ChatRoomRepository chatRoomRepository = new JdbcChatRoomRepository(databaseManager);
            ClientSessionRepository sessionRepository = new JdbcClientSessionRepository(databaseManager);
            OutboxRepository outboxRepository = new JdbcOutboxRepository(databaseManager);

            // Services
            ClientSessionManager sessionManager = new ClientSessionManager(config.getServerId(), sessionRepository, logService);
            PeerReplicationManager replicationManager = new PeerReplicationManager(
                    config, outboxRepository, messageRepository, metrics, logService
            );

            ChatServiceImpl chatService = new ChatServiceImpl(
                    config, messageRepository, chatRoomRepository, sessionManager,
                    replicationManager, metrics, logService
            );

            ReplicationServiceImpl replicationService = new ReplicationServiceImpl(
                    config, messageRepository, databaseManager, metrics, logService
            );

            MonitoringServiceImpl monitoringService = new MonitoringServiceImpl(
                    config, databaseManager, messageRepository, sessionManager,
                    replicationManager, metrics, logService
            );

            // RMI Registry and Service Export
            RmiRegistryManager rmiManager = new RmiRegistryManager(config);
            rmiManager.start();

            rmiManager.exportAndBind(RmiRegistryManager.CHAT_SERVICE_NAME, chatService, config.getRmiServicePort());
            rmiManager.exportAndBind(RmiRegistryManager.REPLICATION_SERVICE_NAME, replicationService, config.getRmiServicePort());
            rmiManager.exportAndBind(RmiRegistryManager.MONITORING_SERVICE_NAME, monitoringService, config.getRmiServicePort());

            // Register Shutdown Hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("Shutdown signal received, shutting down gracefully...");
                try {
                    sessionManager.close();
                    replicationManager.close();
                    rmiManager.close();
                    databaseManager.close();
                    log.info("Server node '{}' stopped successfully.", config.getServerId());
                } catch (Exception e) {
                    log.error("Error during graceful shutdown: {}", e.getMessage(), e);
                }
            }, "nexus-shutdown-thread"));

            logService.logEvent(EventSeverity.INFO, "BOOTSTRAP",
                    String.format("Server node '%s' is ONLINE and ready on %s (Registry:%d, Service:%d)",
                            config.getServerId(), config.getAdvertisedHost(),
                            config.getRmiRegistryPort(), config.getRmiServicePort()));

            log.info("=================================================================");
            log.info(" NEXUS SERVER '{}' STARTED SUCCESSFULLY", config.getServerId().toUpperCase());
            log.info(" Advertised Host:  {}", config.getAdvertisedHost());
            log.info(" RMI Registry:     {}", config.getRmiRegistryPort());
            log.info(" RMI Service:      {}", config.getRmiServicePort());
            log.info(" Configured Peer:  {} ({}:{})", config.getPeerId(), config.getPeerHost(), config.getPeerRegistryPort());
            log.info(" Persistence Mode: {}", messageRepository.getPersistenceMode());
            log.info("=================================================================");

        } catch (Exception e) {
            log.error("FATAL: Failed to start NEXUS Chat Server: {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    private static void printBanner() {
        System.out.println("""
                 _   _ _______  ___   _ ____    ____  _____ ______     _______ ____ \s
                | \\ | | ____\\ \\/ / | | / ___|  / ___|| ____|  _ \\ \\   / / ____|  _ \\\s
                |  \\| |  _|  \\  /| | | \\___ \\  \\___ \\|  _| | |_) \\ \\ / /|  _| | |_) |
                | |\\  | |___ /  \\| |_| |___) |  ___) | |___|  _ < \\ V / | |___|  _ <\s
                |_| \\_|_____/_/\\_\\\\___/|____/  |____/|_____|_| \\_\\ \\_/  |_____|_| \\_\\
                   Distributed Chat System with Java RMI Bidirectional Replication
                """);
    }
}
