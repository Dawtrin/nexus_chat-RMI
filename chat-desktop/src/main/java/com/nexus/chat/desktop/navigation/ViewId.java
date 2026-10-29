package com.nexus.chat.desktop.navigation;

/**
 * Identifiers for view navigation.
 */
public enum ViewId {
    LOGIN("/com/nexus/chat/desktop/fxml/login-view.fxml", "NEXUS CHAT — Sign In & Connect"),
    CHAT("/com/nexus/chat/desktop/fxml/chat-view.fxml", "NEXUS CHAT — Distributed Messenger"),
    ADMIN("/com/nexus/chat/desktop/fxml/admin-view.fxml", "NEXUS CONTROL — Cluster Monitoring Dashboard"),
    SETTINGS("/com/nexus/chat/desktop/fxml/settings-view.fxml", "NEXUS CHAT — Connection Preferences");

    private final String fxmlPath;
    private final String title;

    ViewId(String fxmlPath, String title) {
        this.fxmlPath = fxmlPath;
        this.title = title;
    }

    public String getFxmlPath() {
        return fxmlPath;
    }

    public String getTitle() {
        return title;
    }
}
