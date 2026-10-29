package com.nexus.chat.desktop.rmi;

import com.nexus.chat.contract.remote.ChatRemote;
import com.nexus.chat.contract.remote.MonitoringRemote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Handles RMI registry lookups and caching of remote service proxies for the desktop client.
 */
public class RmiClientManager {
    private static final Logger log = LoggerFactory.getLogger(RmiClientManager.class);

    public static final String CHAT_SERVICE_NAME = "NexusChatService";
    public static final String MONITORING_SERVICE_NAME = "NexusMonitoringService";

    private String connectedHost;
    private int connectedPort;
    private ChatRemote chatService;
    private MonitoringRemote monitoringService;

    public synchronized void connect(String host, int port) throws Exception {
        log.info("Connecting to RMI Registry at {}:{}...", host, port);
        try {
            Registry registry = LocateRegistry.getRegistry(host, port);

            // Lookup chat service
            this.chatService = (ChatRemote) registry.lookup(CHAT_SERVICE_NAME);

            // Lookup monitoring service (optional for pure chat clients, required for admin)
            try {
                this.monitoringService = (MonitoringRemote) registry.lookup(MONITORING_SERVICE_NAME);
            } catch (Exception e) {
                log.warn("Monitoring service not available on {}:{}: {}", host, port, e.getMessage());
                this.monitoringService = null;
            }

            this.connectedHost = host;
            this.connectedPort = port;
            log.info("Successfully connected to RMI services at {}:{}", host, port);

        } catch (ConnectException e) {
            reset();
            throw new RemoteException("Could not connect to server at " + host + ":" + port + ". Verify host and port.", e);
        } catch (NotBoundException e) {
            reset();
            throw new RemoteException("Service '" + e.getMessage() + "' not bound in RMI registry at " + host + ":" + port, e);
        } catch (Exception e) {
            reset();
            throw e;
        }
    }

    public synchronized void reset() {
        this.chatService = null;
        this.monitoringService = null;
        this.connectedHost = null;
        this.connectedPort = 0;
    }

    public synchronized boolean isConnected() {
        return chatService != null;
    }

    public synchronized ChatRemote getChatService() {
        return chatService;
    }

    public synchronized MonitoringRemote getMonitoringService() {
        return monitoringService;
    }

    public String getConnectedHost() {
        return connectedHost;
    }

    public int getConnectedPort() {
        return connectedPort;
    }
}
