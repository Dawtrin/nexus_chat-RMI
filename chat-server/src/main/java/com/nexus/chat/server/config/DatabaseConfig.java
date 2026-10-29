package com.nexus.chat.server.config;

/**
 * Database connection parameters.
 */
public class DatabaseConfig {
    private final boolean enabled;
    private final String host;
    private final int port;
    private final String databaseName;
    private final String username;
    private final String password;
    private final int poolMaxSize;

    public DatabaseConfig(boolean enabled,
                          String host,
                          int port,
                          String databaseName,
                          String username,
                          String password,
                          int poolMaxSize) {
        this.enabled = enabled;
        this.host = host;
        this.port = port;
        this.databaseName = databaseName;
        this.username = username;
        this.password = password;
        this.poolMaxSize = poolMaxSize;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public int getPoolMaxSize() {
        return poolMaxSize;
    }

    public String getJdbcUrl() {
        return String.format(
                "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&connectTimeout=3000&socketTimeout=5000",
                host, port, databaseName
        );
    }
}
