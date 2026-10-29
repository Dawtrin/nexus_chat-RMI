package com.nexus.chat.contract.enums;

import java.io.Serializable;

/**
 * Message category / delivery type.
 */
public enum MessageType implements Serializable {
    CHAT,
    SYSTEM,
    JOIN,
    LEAVE
}
