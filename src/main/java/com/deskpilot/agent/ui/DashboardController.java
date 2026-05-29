package com.deskpilot.agent.ui;

import com.deskpilot.agent.AgentApplication;
import com.deskpilot.agent.core.AgentService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    @FXML private Label statusDot;
    @FXML private Label statusText;
    @FXML private Label fpsLabel;
    @FXML private Label qualityLabel;
    @FXML private Label latencyLabel;
    @FXML private Label clientsLabel;

    private AgentService agentService;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        agentService = AgentApplication.getAgentService();

        // Atualiza labels com configurações atuais
        var config = AgentApplication.getConfig();
        fpsLabel.setText(String.valueOf(config.getFps()));
        qualityLabel.setText((int)(config.getQuality() * 100) + "%");

        // Escuta mudanças de status
        agentService.setOnStatusChange(status -> Platform.runLater(() -> {
            updateStatus(status);
            if (status.equals("Desconectado") || status.equals("Parado")) {
                navigateToLogin();
            }
        }));
    }

    @FXML
    private void onDisconnect() {
        agentService.stop();
        AgentApplication.getConfig().clearToken();
        navigateToLogin();
    }

    @FXML
    private void onSettings() {
        // Abre tela de configurações — implementar depois
    }

    public void updateLatency(long ms) {
        Platform.runLater(() -> latencyLabel.setText(ms + "ms"));
    }

    public void updateClients(int count) {
        Platform.runLater(() -> clientsLabel.setText(String.valueOf(count)));
    }

    private void updateStatus(String status) {
        statusText.setText(status);
        if (status.equals("Conectado")) {
            statusDot.getStyleClass().setAll("status-dot-green");
        } else if (status.startsWith("Erro")) {
            statusDot.getStyleClass().setAll("status-dot-red");
        } else {
            statusDot.getStyleClass().setAll("status-dot-yellow");
        }
    }

    private void navigateToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/login.fxml")
            );
            Parent root  = loader.load();
            Stage  stage = (Stage) statusText.getScene().getWindow();
            stage.setScene(new Scene(root, 340, 520));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}