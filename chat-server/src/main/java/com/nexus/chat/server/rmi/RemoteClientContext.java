package com.nexus.chat.server.rmi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;

/**
 * Utility for inspecting the incoming RMI call context to extract the caller's network IP.
 */
public final class RemoteClientContext {
    private static final Logger log = LoggerFactory.getLogger(RemoteClientContext.class);

    private RemoteClientContext() {
    }

    /**
     * Extracts the client host address from the current RMI thread execution context.
     *
     * @return Client IP address or hostname, or "UNKNOWN" if call is not an active RMI dispatch
     */
    public static String getObservedClientHost() {
        try {
            return RemoteServer.getClientHost();
        } catch (ServerNotActiveException e) {
            log.debug("RemoteServer.getClientHost() called outside active RMI dispatch context");
            return "127.0.0.1";
        } catch (Exception e) {
            log.warn("Unexpected exception querying client host: {}", e.getMessage());
            return "UNKNOWN";
        }
    }
}
