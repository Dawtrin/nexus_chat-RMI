package com.nexus.chat.desktop;

import com.nexus.chat.desktop.navigation.ViewId;
import com.nexus.chat.desktop.navigation.ViewRouter;
import com.nexus.chat.desktop.rmi.RmiClientManager;
import com.nexus.chat.desktop.service.AdminMonitoringService;
import com.nexus.chat.desktop.service.ChatClientService;
import com.nexus.chat.desktop.util.FxAsync;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main JavaFX application entrypoint for NEXUS CHAT.
 */
public class DesktopApplication extends Application {
    private static final Logger log = LoggerFactory.getLogger(DesktopApplication.class);

    private RmiClientManager rmiManager;
    private ChatClientService chatService;
    private AdminMonitoringService adminService;
    private ViewRouter router;

    @Override
    public void start(Stage primaryStage) {
        log.info("Starting NEXUS CHAT JavaFX Desktop Client...");

        this.rmiManager = new RmiClientManager();
        this.chatService = new ChatClientService(rmiManager);
        this.adminService = new AdminMonitoringService();
        this.router = new ViewRouter(primaryStage, chatService, adminService);

        primaryStage.setMinWidth(960);
        primaryStage.setMinHeight(640);

        primaryStage.setOnCloseRequest(event -> {
            log.info("Window close requested. Shutting down client resources...");
            try {
                chatService.close();
                FxAsync.shutdown();
            } catch (Exception e) {
                log.warn("Error during client shutdown: {}", e.getMessage());
            }
            Platform.exit();
            System.exit(0);
        });

        // Launch into Login view
        router.navigateTo(ViewId.LOGIN);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
