package com.nexus.chat.contract.enums;

import java.io.Serializable;

/**
 * Observed operational health status of a server or component.
 */
public enum ServerStatus implements Serializable {
    ONLINE,
    DEGRADED,
    UNREACHABLE,
    UNKNOWN
}
