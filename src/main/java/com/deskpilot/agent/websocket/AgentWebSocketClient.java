package com.deskpilot.agent.websocket;

import com.deskpilot.agent.core.InputControlService;
import com.deskpilot.agent.core.ScreenCaptureService;
import com.deskpilot.agent.model.CommandType;
import com.deskpilot.agent.model.RelayMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
public class AgentWebSocketClient extends WebSocketClient {

    private final ObjectMapper       objectMapper;
    private final InputControlService inputControl;
    private final ScreenCaptureService screenCapture;
    private final ScheduledExecutorService reconnectScheduler;

    private Consumer<String> onStatusChange;
    private boolean          intentionallyClosed = false;

    public AgentWebSocketClient(
            URI serverUri,
            InputControlService inputControl,
            ScreenCaptureService screenCapture
    ) {
        super(serverUri);
        this.objectMapper       = new ObjectMapper();
        this.inputControl       = inputControl;
        this.screenCapture      = screenCapture;
        this.reconnectScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ws-reconnect");
            t.setDaemon(true);
            return t;
        });
    }

    // ─── Conexão ─────────────────────────────────────────────────────────────────

    @Override
    public void onOpen(ServerHandshake handshake) {
        log.info("Conectado ao relay");
        notifyStatus("Conectado");

        // Inicia o stream de tela
        screenCapture.start(frameBytes -> {
            if (isOpen()) {
                send(ByteBuffer.wrap(frameBytes));
            }
        });

        // Envia PING a cada 30s para manter conexão viva
        reconnectScheduler.scheduleAtFixedRate(() -> {
            if (isOpen()) {
                try {
                    String ping = objectMapper.writeValueAsString(
                            RelayMessage.builder()
                                    .type(CommandType.PING)
                                    .timestamp(System.currentTimeMillis())
                                    .build()
                    );
                    send(ping);
                } catch (Exception e) {
                    log.error("Erro ao enviar PING: {}", e.getMessage());
                }
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    // ─── Mensagens recebidas ─────────────────────────────────────────────────────

    @Override
    public void onMessage(String message) {
        try {
            RelayMessage msg = objectMapper.readValue(message, RelayMessage.class);
            handleCommand(msg);
        } catch (Exception e) {
            log.error("Erro ao processar mensagem: {}", e.getMessage());
        }
    }

    @Override
    public void onMessage(ByteBuffer bytes) {
        // Agente não recebe frames binários
    }

    // ─── Processamento de comandos ───────────────────────────────────────────────

    private void handleCommand(RelayMessage msg) {
        if (msg.getType() == null) return;

        switch (msg.getType()) {
            case MOUSE_MOVE   -> handleMouseMove(msg.getPayload());
            case MOUSE_CLICK  -> handleMouseClick(msg.getPayload());
            case MOUSE_SCROLL -> handleMouseScroll(msg.getPayload());
            case KEY_PRESS    -> handleKeyPress(msg.getPayload());
            case KEY_RELEASE  -> handleKeyRelease(msg.getPayload());
            case TYPE_TEXT    -> handleTypeText(msg.getPayload());
            case SCREEN_START -> screenCapture.start(frameBytes -> {
                if (isOpen()) send(ByteBuffer.wrap(frameBytes));
            });
            case SCREEN_STOP  -> screenCapture.stop();
            case PONG         -> log.debug("PONG recebido");
            case CLIENT_CONNECTED    -> log.info("Cliente conectado");
            case CLIENT_DISCONNECTED -> {
                log.info("Cliente desconectado");
                screenCapture.stop();
            }
            default -> log.debug("Comando ignorado: {}", msg.getType());
        }
    }

    // ─── Handlers de comando ─────────────────────────────────────────────────────

    private void handleMouseMove(String payload) {
        try {
            Map<?, ?> data = objectMapper.readValue(payload, Map.class);
            int x = ((Number) data.get("x")).intValue();
            int y = ((Number) data.get("y")).intValue();
            inputControl.mouseMove(x, y);
        } catch (Exception e) {
            log.error("Erro ao mover mouse: {}", e.getMessage());
        }
    }

    private void handleMouseClick(String payload) {
        try {
            Map<?, ?> data   = objectMapper.readValue(payload, Map.class);
            int x      = ((Number) data.get("x")).intValue();
            int y      = ((Number) data.get("y")).intValue();
            int button = data.containsKey("button")
                    ? ((Number) data.get("button")).intValue() : 1;
            inputControl.mouseClick(x, y, button);
        } catch (Exception e) {
            log.error("Erro ao clicar mouse: {}", e.getMessage());
        }
    }

    private void handleMouseScroll(String payload) {
        try {
            Map<?, ?> data   = objectMapper.readValue(payload, Map.class);
            int amount = ((Number) data.get("amount")).intValue();
            inputControl.mouseScroll(amount);
        } catch (Exception e) {
            log.error("Erro ao rolar mouse: {}", e.getMessage());
        }
    }

    private void handleKeyPress(String payload) {
        try {
            Map<?, ?> data    = objectMapper.readValue(payload, Map.class);
            int keyCode = ((Number) data.get("keyCode")).intValue();
            inputControl.keyPress(keyCode);
        } catch (Exception e) {
            log.error("Erro ao pressionar tecla: {}", e.getMessage());
        }
    }

    private void handleKeyRelease(String payload) {
        try {
            Map<?, ?> data    = objectMapper.readValue(payload, Map.class);
            int keyCode = ((Number) data.get("keyCode")).intValue();
            inputControl.keyRelease(keyCode);
        } catch (Exception e) {
            log.error("Erro ao soltar tecla: {}", e.getMessage());
        }
    }

    private void handleTypeText(String payload) {
        try {
            Map<?, ?> data = objectMapper.readValue(payload, Map.class);
            String text    = (String) data.get("text");
            inputControl.typeText(text);
        } catch (Exception e) {
            log.error("Erro ao digitar texto: {}", e.getMessage());
        }
    }

    // ─── Desconexão e reconexão ──────────────────────────────────────────────────

    @Override
    public void onClose(int code, String reason, boolean remote) {
        log.warn("Desconectado do relay — código: {}, motivo: {}", code, reason);
        screenCapture.stop();
        notifyStatus("Desconectado");

        if (!intentionallyClosed) {
            log.info("Tentando reconectar em 5 segundos...");
            reconnectScheduler.schedule(() -> {
                try {
                    reconnect();
                } catch (Exception e) {
                    log.error("Erro ao reconectar: {}", e.getMessage());
                }
            }, 5, TimeUnit.SECONDS);
        }
    }

    @Override
    public void onError(Exception ex) {
        log.error("Erro no WebSocket: {}", ex.getMessage());
        notifyStatus("Erro: " + ex.getMessage());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    public void closeGracefully() {
        intentionallyClosed = true;
        screenCapture.stop();
        reconnectScheduler.shutdown();
        close();
    }

    public void setOnStatusChange(Consumer<String> listener) {
        this.onStatusChange = listener;
    }

    private void notifyStatus(String status) {
        if (onStatusChange != null) {
            onStatusChange.accept(status);
        }
    }
}