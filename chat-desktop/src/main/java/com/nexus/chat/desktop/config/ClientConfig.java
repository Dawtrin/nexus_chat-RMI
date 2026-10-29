package com.nexus.chat.desktop.config;

/**
 * Client configuration preferences and default connection profiles.
 */
public class ClientConfig {
    public static final String DEFAULT_SERVER_A_HOST = "127.0.0.1";
    public static final int DEFAULT_SERVER_A_PORT = 1099;

    public static final String DEFAULT_SERVER_B_HOST = "127.0.0.1";
    public static final int DEFAULT_SERVER_B_PORT = 2099;

    public static final int DEFAULT_POLL_INTERVAL_MS = 1000;
    public static final int HEARTBEAT_INTERVAL_SECONDS = 10;
}
