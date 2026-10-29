package com.nexus.chat.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * Server configuration manager supporting property files, system properties, and environment variables.
 */
public class ServerConfig {
    private static final Logger log = LoggerFactory.getLogger(ServerConfig.class);

    private final Properties properties = new Properties();

    private final String serverId;
    private final String advertisedHost;
    private final int rmiRegistryPort;
    private final int rmiServicePort;
    private final String adminToken;

    private final String peerId;
    private final String peerHost;
    private final int peerRegistryPort;
    private final int peerServicePort;

    private final DatabaseConfig databaseConfig;

    public ServerConfig(String[] args) {
        loadProperties();
        applyCommandLineArgs(args);

        this.serverId = getPropertyOrEnv("server.id", "NEXUS_SERVER_ID", "server-a");
        this.advertisedHost = getPropertyOrEnv("server.advertised.host", "NEXUS_SERVER_HOST", "127.0.0.1");
        this.rmiRegistryPort = Integer.parseInt(getPropertyOrEnv("server.rmi.registry.port", "NEXUS_RMI_REGISTRY_PORT", "1099"));
        this.rmiServicePort = Integer.parseInt(getPropertyOrEnv("server.rmi.service.port", "NEXUS_RMI_SERVICE_PORT", "1100"));
        this.adminToken = getPropertyOrEnv("server.admin.token", "NEXUS_ADMIN_TOKEN", "nexus-admin-secret");

        this.peerId = getPropertyOrEnv("server.peer.id", "NEXUS_PEER_ID", "server-b");
        this.peerHost = getPropertyOrEnv("server.peer.host", "NEXUS_PEER_HOST", "127.0.0.1");
        this.peerRegistryPort = Integer.parseInt(getPropertyOrEnv("server.peer.registry.port", "NEXUS_PEER_REGISTRY_PORT", "2099"));
        this.peerServicePort = Integer.parseInt(getPropertyOrEnv("server.peer.service.port", "NEXUS_PEER_SERVICE_PORT", "2100"));

        boolean dbEnabled = Boolean.parseBoolean(getPropertyOrEnv("database.enabled", "NEXUS_DB_ENABLED", "false"));
        String dbHost = getPropertyOrEnv("database.host", "NEXUS_DB_HOST", "127.0.0.1");
        int dbPort = Integer.parseInt(getPropertyOrEnv("database.port", "NEXUS_DB_PORT", "3306"));
        String dbName = getPropertyOrEnv("database.name", "NEXUS_DB_NAME", "nexus_chat");
        String dbUser = getPropertyOrEnv("database.username", "DB_USERNAME", "nexus_user");
        String dbPass = getPropertyOrEnv("database.password", "DB_PASSWORD", "");
        int poolMax = Integer.parseInt(getPropertyOrEnv("database.pool.max-size", "NEXUS_DB_POOL_MAX", "10"));

        this.databaseConfig = new DatabaseConfig(dbEnabled, dbHost, dbPort, dbName, dbUser, dbPass, poolMax);

        log.info("ServerConfig initialized: id={}, advertisedHost={}, registryPort={}, servicePort={}, peerId={}",
                serverId, advertisedHost, rmiRegistryPort, rmiServicePort, peerId);
    }

    private void loadProperties() {
        // 1. Try local file in working directory
        File localFile = new File("application.properties");
        if (localFile.exists()) {
            try (InputStream is = new FileInputStream(localFile)) {
                properties.load(is);
                log.info("Loaded configuration from local file: {}", localFile.getAbsolutePath());
                return;
            } catch (Exception e) {
                log.warn("Failed reading local application.properties", e);
            }
        }

        // 2. Try classpath application.properties
        try (InputStream is = getClass().getResourceAsStream("/application.properties")) {
            if (is != null) {
                properties.load(is);
                log.info("Loaded configuration from classpath: /application.properties");
                return;
            }
        } catch (Exception e) {
            log.warn("Failed reading classpath application.properties", e);
        }

        // 3. Fallback to classpath application.properties.example
        try (InputStream is = getClass().getResourceAsStream("/application.properties.example")) {
            if (is != null) {
                properties.load(is);
                log.info("Loaded defaults from classpath: /application.properties.example");
            }
        } catch (Exception e) {
            log.warn("Failed reading classpath application.properties.example", e);
        }
    }

    private void applyCommandLineArgs(String[] args) {
        if (args == null) return;
        for (String arg : args) {
            if (arg.startsWith("--") && arg.contains("=")) {
                String[] parts = arg.substring(2).split("=", 2);
                properties.setProperty(parts[0], parts[1]);
            }
        }
    }

    private String getPropertyOrEnv(String propKey, String envKey, String defaultValue) {
        String val = System.getProperty(propKey);
        if (val != null && !val.trim().isEmpty()) return val.trim();

        val = System.getenv(envKey);
        if (val != null && !val.trim().isEmpty()) return val.trim();

        val = properties.getProperty(propKey);
        if (val != null && !val.trim().isEmpty()) return val.trim();

        return defaultValue;
    }

    public String getServerId() {
        return serverId;
    }

    public String getAdvertisedHost() {
        return advertisedHost;
    }

    public int getRmiRegistryPort() {
        return rmiRegistryPort;
    }

    public int getRmiServicePort() {
        return rmiServicePort;
    }

    public String getAdminToken() {
        return adminToken;
    }

    public String getPeerId() {
        return peerId;
    }

    public String getPeerHost() {
        return peerHost;
    }

    public int getPeerRegistryPort() {
        return peerRegistryPort;
    }

    public int getPeerServicePort() {
        return peerServicePort;
    }

    public DatabaseConfig getDatabaseConfig() {
        return databaseConfig;
    }
}
