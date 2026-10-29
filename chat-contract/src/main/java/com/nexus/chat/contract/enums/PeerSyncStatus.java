package com.nexus.chat.contract.enums;

import java.io.Serializable;

/**
 * State of bidirectional peer synchronization.
 */
public enum PeerSyncStatus implements Serializable {
    SYNCED,
    IN_PROGRESS,
    FAILED,
    UNKNOWN
}
