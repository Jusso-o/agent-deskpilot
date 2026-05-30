package com.deskpilot.agent.ui;

import com.deskpilot.agent.AgentApplication;
import com.deskpilot.agent.config.AgentConfig;
import com.deskpilot.agent.core.AgentService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class AgentApp extends Application {

    private TrayIcon trayIcon;

    @Override
    public void start(Stage primaryStage) throws Exception {

        AgentConfig  config       = new AgentConfig();
        AgentService agentService = new AgentService(config);
        AgentApplication.init(config, agentService);

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/login.fxml")
        );
        Parent root = loader.load();

        // Remove barra padrão do Windows
        primaryStage.initStyle(StageStyle.UNDECORATED);

        Scene scene = new Scene(root, 340, 560);
        primaryStage.setTitle("DeskPilot Agent");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

        // Arrastar janela com o mouse no titlebar
        root.setOnMousePressed(e -> {
            root.setUserData(new double[]{e.getSceneX(), e.getSceneY()});
        });
        root.setOnMouseDragged(e -> {
            double[] offset = (double[]) root.getUserData();
            if (offset != null) {
                primaryStage.setX(e.getScreenX() - offset[0]);
                primaryStage.setY(e.getScreenY() - offset[1]);
            }
        });

        setupTray(primaryStage, agentService);

        primaryStage.iconifiedProperty().addListener((obs, wasMin, isMin) -> {
            if (isMin) Platform.runLater(primaryStage::hide);
        });

        primaryStage.setOnCloseRequest(e -> {
            agentService.stop();
            removeTray();
            Platform.exit();
        });
    }

    private void setupTray(Stage stage, AgentService agentService) {
        if (!SystemTray.isSupported()) return;
        try {
            Platform.setImplicitExit(false);

            java.awt.image.BufferedImage img =
                    new java.awt.image.BufferedImage(16, 16,
                            java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g = img.createGraphics();
            g.setColor(new java.awt.Color(0x1D9E75));
            g.fillOval(0, 0, 16, 16);
            g.dispose();

            PopupMenu popup = new PopupMenu();
            MenuItem showItem = new MenuItem("Abrir DeskPilot");
            showItem.addActionListener(e -> Platform.runLater(stage::show));
            MenuItem exitItem = new MenuItem("Sair");
            exitItem.addActionListener(e -> {
                agentService.stop();
                removeTray();
                Platform.exit();
            });
            popup.add(showItem);
            popup.addSeparator();
            popup.add(exitItem);

            trayIcon = new TrayIcon(img, "DeskPilot Agent", popup);
            trayIcon.setImageAutoSize(true);
            trayIcon.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2)
                        Platform.runLater(stage::show);
                }
            });
            SystemTray.getSystemTray().add(trayIcon);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void removeTray() {
        if (trayIcon != null)
            SystemTray.getSystemTray().remove(trayIcon);
    }

    public static void main(String[] args) {
        launch(args);
    }
}