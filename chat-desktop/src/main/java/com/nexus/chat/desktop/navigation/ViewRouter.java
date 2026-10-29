package com.nexus.chat.desktop.navigation;

import com.nexus.chat.desktop.controller.AdminController;
import com.nexus.chat.desktop.controller.ChatController;
import com.nexus.chat.desktop.controller.LoginController;
import com.nexus.chat.desktop.controller.SettingsController;
import com.nexus.chat.desktop.service.AdminMonitoringService;
import com.nexus.chat.desktop.service.ChatClientService;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;

/**
 * Manages view transitions and controller dependency injection.
 */
public class ViewRouter {
    private static final Logger log = LoggerFactory.getLogger(ViewRouter.class);

    private final Stage stage;
    private final ChatClientService chatService;
    private final AdminMonitoringService adminService;
    private Scene scene;

    public ViewRouter(Stage stage, ChatClientService chatService, AdminMonitoringService adminService) {
        this.stage = stage;
        this.chatService = chatService;
        this.adminService = adminService;
    }

    public void navigateTo(ViewId viewId) {
        try {
            URL fxmlUrl = getClass().getResource(viewId.getFxmlPath());
            if (fxmlUrl == null) {
                throw new IOException("Cannot locate FXML resource: " + viewId.getFxmlPath());
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            loader.setControllerFactory(param -> createController(param));

            Parent root = loader.load();

            if (scene == null) {
                scene = new Scene(root, 1180, 760);
                scene.getStylesheets().add(
                        getClass().getResource("/com/nexus/chat/desktop/css/design-system.css").toExternalForm()
                );
                stage.setScene(scene);
            } else {
                scene.setRoot(root);
            }

            stage.setTitle(viewId.getTitle());
            stage.show();
            log.info("Navigated to {}", viewId);

        } catch (Exception e) {
            log.error("Failed navigating to {}: {}", viewId, e.getMessage(), e);
        }
    }

    private Object createController(Class<?> type) {
        if (type == LoginController.class) {
            return new LoginController(chatService, this);
        } else if (type == ChatController.class) {
            return new ChatController(chatService, this);
        } else if (type == AdminController.class) {
            return new AdminController(chatService, adminService, this);
        } else if (type == SettingsController.class) {
            return new SettingsController(chatService, this);
        }
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Cannot instantiate controller " + type, e);
        }
    }
}
