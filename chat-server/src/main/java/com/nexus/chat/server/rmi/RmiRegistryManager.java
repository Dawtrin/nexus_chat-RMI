package com.nexus.chat.server.rmi;

import com.nexus.chat.server.config.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the RMI Registry lifecycle, advertised hostname configuration, and service exports.
 */
public class RmiRegistryManager implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(RmiRegistryManager.class);

    public static final String CHAT_SERVICE_NAME = "NexusChatService";
    public static final String REPLICATION_SERVICE_NAME = "NexusReplicationService";
    public static final String MONITORING_SERVICE_NAME = "NexusMonitoringService";

    private final ServerConfig config;
    private Registry registry;
    private boolean createdLocally = false;
    private final List<Remote> exportedServices = new ArrayList<>();

    public RmiRegistryManager(ServerConfig config) {
        this.config = config;
    }

    public synchronized void start() throws Exception {
        // Set advertised RMI hostname to guarantee remote clients connect to reachable IP
        String advertisedHost = config.getAdvertisedHost();
        System.setProperty("java.rmi.server.hostname", advertisedHost);
        log.info("Configured java.rmi.server.hostname = {}", advertisedHost);

        int registryPort = config.getRmiRegistryPort();
        try {
            this.registry = LocateRegistry.createRegistry(registryPort);
            this.createdLocally = true;
            log.info("Created local RMI Registry on port {}", registryPort);
        } catch (Exception e) {
            log.info("RMI Registry already active on port {}, connecting to existing registry...", registryPort);
            this.registry = LocateRegistry.getRegistry(registryPort);
            this.createdLocally = false;
        }
    }

    public synchronized <T extends Remote> Remote exportAndBind(String serviceName, T serviceImplementation, int exportPort) throws Exception {
        if (registry == null) {
            throw new IllegalStateException("RMI Registry is not started");
        }

        log.info("Exporting remote service '{}' on port {}...", serviceName, exportPort);
        Remote stub = UnicastRemoteObject.exportObject(serviceImplementation, exportPort);
        registry.rebind(serviceName, stub);
        exportedServices.add(serviceImplementation);
        log.info("Bound service '{}' into RMI Registry", serviceName);
        return stub;
    }

    @Override
    public synchronized void close() {
        log.info("Shutting down RMI services...");
        for (Remote service : exportedServices) {
            try {
                UnicastRemoteObject.unexportObject(service, true);
            } catch (Exception e) {
                log.warn("Error unexporting service: {}", e.getMessage());
            }
        }
        exportedServices.clear();

        if (createdLocally && registry != null) {
            try {
                UnicastRemoteObject.unexportObject(registry, true);
                log.info("Unexported local RMI Registry");
            } catch (Exception e) {
                log.warn("Error unexporting RMI Registry: {}", e.getMessage());
            }
        }
    }
}
