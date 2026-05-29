package com.deskpilot.agent.core;

import com.deskpilot.agent.config.AgentConfig;
import com.deskpilot.agent.websocket.AgentWebSocketClient;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.Consumer;

@Slf4j
public class AgentService {

    private final AgentConfig          config;
    private AgentWebSocketClient       wsClient;
    private ScreenCaptureService       screenCapture;
    private InputControlService        inputControl;
    private Consumer<String>           onStatusChange;
    private boolean                    running = false;

    public AgentService(AgentConfig config) {
        this.config = config;
    }

    // ─── Iniciar agente ──────────────────────────────────────────────────────────

    public void start(String username, String password) {
        if (running) {
            log.warn("Agente já está rodando");
            return;
        }

        notifyStatus("Autenticando...");

        // 1. Faz login no relay para obter o token
        new Thread(() -> {
            try {
                String token = login(username, password);
                config.setToken(token);
                config.setUsername(username);
                config.save();
                connect();
            } catch (Exception e) {
                log.error("Erro ao autenticar: {}", e.getMessage());
                notifyStatus("Erro: " + e.getMessage());
            }
        }, "agent-start").start();
    }

    public void startWithToken() {
        if (!config.hasToken()) {
            notifyStatus("Token não encontrado — faça login");
            return;
        }
        connect();
    }

    // ─── Conectar ao relay ───────────────────────────────────────────────────────

    private void connect() {
        try {
            notifyStatus("Conectando...");

            screenCapture = new ScreenCaptureService();
            inputControl  = new InputControlService();

            // Monta a URI com token e role=agent
            String wsUrl = config.getRelayUrl()
                    + "?token=" + config.getToken()
                    + "&role=agent";

            wsClient = new AgentWebSocketClient(
                    new URI(wsUrl),
                    inputControl,
                    screenCapture
            );

            wsClient.setOnStatusChange(status -> {
                running = status.equals("Conectado");
                notifyStatus(status);
            });

            wsClient.connect();

        } catch (AWTException e) {
            log.error("Erro ao inicializar Robot: {}", e.getMessage());
            notifyStatus("Erro: Robot API não disponível");
        } catch (Exception e) {
            log.error("Erro ao conectar: {}", e.getMessage());
            notifyStatus("Erro: " + e.getMessage());
        }
    }

    // ─── Parar agente ────────────────────────────────────────────────────────────

    public void stop() {
        if (wsClient != null) {
            wsClient.closeGracefully();
            wsClient = null;
        }
        if (screenCapture != null) {
            screenCapture.shutdown();
            screenCapture = null;
        }
        running = false;
        notifyStatus("Parado");
        log.info("Agente parado");
    }

    // ─── Login HTTP ──────────────────────────────────────────────────────────────

    private String login(String username, String password) throws Exception {
        String relayHttp = config.getRelayUrl()
                .replace("ws://", "http://")
                .replace("wss://", "https://")
                .replace("/relay", "");

        String body = String.format(
                "{\"username\":\"%s\",\"password\":\"%s\"}",
                username, password
        );

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(relayHttp + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(
                request, HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new RuntimeException("Login falhou: " + response.body());
        }

        // Extrai o accessToken do JSON de resposta
        String responseBody = response.body();
        int start = responseBody.indexOf("\"accessToken\":\"") + 15;
        int end   = responseBody.indexOf("\"", start);
        return responseBody.substring(start, end);
    }

    // ─── Status ──────────────────────────────────────────────────────────────────

    public void setOnStatusChange(Consumer<String> listener) {
        this.onStatusChange = listener;
    }

    private void notifyStatus(String status) {
        log.info("Status: {}", status);
        if (onStatusChange != null) {
            onStatusChange.accept(status);
        }
    }

    public boolean isRunning() {
        return running;
    }

    public AgentConfig getConfig() {
        return config;
    }
}