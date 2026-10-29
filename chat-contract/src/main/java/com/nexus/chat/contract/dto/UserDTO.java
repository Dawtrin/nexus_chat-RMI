package com.nexus.chat.contract.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Basic user identity DTO.
 */
public class UserDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String userId;
    private final String username;
    private final String displayName;

    public UserDTO(String userId, String username, String displayName) {
        this.userId = Objects.requireNonNull(userId, "userId cannot be null");
        this.username = Objects.requireNonNull(username, "username cannot be null");
        this.displayName = displayName != null ? displayName : username;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserDTO userDTO = (UserDTO) o;
        return Objects.equals(userId, userDTO.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return "UserDTO{" +
                "userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", displayName='" + displayName + '\'' +
                '}';
    }
}
