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
import javafx.scene.control.PasswordField;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML private TextField     relayUrlField;
    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label         statusDot;
    @FXML private Label         statusText;
    @FXML private Slider        fpsSlider;
    @FXML private Slider        qualitySlider;
    @FXML private Label         fpsValue;
    @FXML private Label         qualityValue;

    private AgentService agentService;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        agentService = AgentApplication.getAgentService();

        // Carrega configurações salvas
        var config = AgentApplication.getConfig();
        relayUrlField.setText(config.getRelayUrl());
        usernameField.setText(config.getUsername());
        fpsSlider.setValue(config.getFps());
        qualitySlider.setValue(config.getQuality() * 100);

        // Atualiza labels dos sliders em tempo real
        fpsSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int fps = newVal.intValue();
            fpsValue.setText(String.valueOf(fps));
            AgentApplication.getConfig().setFps(fps);
        });

        qualitySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int quality = newVal.intValue();
            qualityValue.setText(quality + "%");
            AgentApplication.getConfig().setQuality(quality / 100f);
        });

        // Escuta mudanças de status do agente
        agentService.setOnStatusChange(status -> Platform.runLater(() -> {
            updateStatus(status);
            if (status.equals("Conectado")) {
                navigateToDashboard();
            }
        }));

        // Se já tem token, tenta conectar
        if (config.hasToken()) {
            updateStatus("Reconectando...");
            agentService.startWithToken();
        }
    }

    @FXML
    private void onConnect() {
        String relayUrl = relayUrlField.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (relayUrl.isEmpty() || username.isEmpty() || password.isEmpty()) {
            updateStatus("Preencha todos os campos");
            return;
        }

        AgentApplication.getConfig().setRelayUrl(relayUrl);
        updateStatus("Conectando...");
        agentService.start(username, password);
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

    private void navigateToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/dashboard.fxml")
            );
            Parent root  = loader.load();
            Stage  stage = (Stage) relayUrlField.getScene().getWindow();
            stage.setScene(new Scene(root, 340, 480));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}