package com.nexus.chat.desktop.model;

/**
 * High-level connection state of the desktop client.
 */
public enum ConnectionState {
    DISCONNECTED("Disconnected", "#f87171"),
    CONNECTING("Connecting...", "#f5b84b"),
    CONNECTED("Connected", "#36d399"),
    RECONNECTING("Reconnecting...", "#f5b84b"),
    FAILED("Connection Failed", "#f87171");

    private final String label;
    private final String colorHex;

    ConnectionState(String label, String colorHex) {
        this.label = label;
        this.colorHex = colorHex;
    }

    public String getLabel() {
        return label;
    }

    public String getColorHex() {
        return colorHex;
    }
}
