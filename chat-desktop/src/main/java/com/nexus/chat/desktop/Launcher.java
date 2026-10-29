package com.nexus.chat.desktop;

/**
 * Standard launcher main class to bypass JavaFX runtime module path restrictions
 * when executed as a shaded JAR or standalone application.
 */
public class Launcher {
    public static void main(String[] args) {
        DesktopApplication.main(args);
    }
}
