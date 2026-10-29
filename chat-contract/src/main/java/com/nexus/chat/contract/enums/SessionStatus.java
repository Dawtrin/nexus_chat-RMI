package com.nexus.chat.contract.enums;

import java.io.Serializable;

/**
 * State of a client session on the server.
 */
public enum SessionStatus implements Serializable {
    ACTIVE,
    IDLE,
    STALE,
    DISCONNECTED
}
