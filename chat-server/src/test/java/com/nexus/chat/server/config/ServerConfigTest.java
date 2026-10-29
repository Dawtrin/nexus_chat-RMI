package com.nexus.chat.server.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ServerConfigTest {

    @Test
    void testDefaultValues() {
        ServerConfig config = new ServerConfig(new String[]{});

        assertThat(config.getServerId()).isEqualTo("server-a");
        assertThat(config.getRmiRegistryPort()).isEqualTo(1099);
        assertThat(config.getRmiServicePort()).isEqualTo(1100);
        assertThat(config.getPeerId()).isEqualTo("server-b");
    }

    @Test
    void testCommandLineOverrides() {
        String[] args = new String[]{
                "--server.id=server-b",
                "--server.rmi.registry.port=2099",
                "--server.peer.id=server-a"
        };

        ServerConfig config = new ServerConfig(args);

        assertThat(config.getServerId()).isEqualTo("server-b");
        assertThat(config.getRmiRegistryPort()).isEqualTo(2099);
        assertThat(config.getPeerId()).isEqualTo("server-a");
    }
}
