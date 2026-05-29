package com.deskpilot.agent.core;

import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
public class ScreenCaptureService {

    private static final int    FPS          = 24;
    private static final float  JPEG_QUALITY = 0.6f;

    private final Robot                    robot;
    private final Rectangle                screenBounds;
    private final ScheduledExecutorService scheduler;
    private       ScheduledFuture<?>       captureTask;
    private       Consumer<byte[]>         frameConsumer;

    public ScreenCaptureService() throws AWTException {
        this.robot        = new Robot();
        this.screenBounds = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration()
                .getBounds();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "screen-capture");
            t.setDaemon(true);
            return t;
        });
    }

    // ─── Iniciar stream ──────────────────────────────────────────────────────────

    public void start(Consumer<byte[]> onFrame) {
        if (captureTask != null && !captureTask.isDone()) {
            log.warn("Captura já está em andamento");
            return;
        }
        this.frameConsumer = onFrame;
        long intervalMs = 1000L / FPS;
        captureTask = scheduler.scheduleAtFixedRate(
                this::captureAndSend, 0, intervalMs, TimeUnit.MILLISECONDS
        );
        log.info("Captura de tela iniciada a {} FPS", FPS);
    }

    // ─── Parar stream ────────────────────────────────────────────────────────────

    public void stop() {
        if (captureTask != null) {
            captureTask.cancel(false);
            captureTask = null;
            log.info("Captura de tela parada");
        }
    }

    public boolean isRunning() {
        return captureTask != null && !captureTask.isDone();
    }

    // ─── Captura e compressão ────────────────────────────────────────────────────

    private void captureAndSend() {
        try {
            BufferedImage screenshot = robot.createScreenCapture(screenBounds);
            byte[] jpegBytes = toJpeg(screenshot);
            if (frameConsumer != null) {
                frameConsumer.accept(jpegBytes);
            }
        } catch (Exception e) {
            log.error("Erro ao capturar tela: {}", e.getMessage());
        }
    }

    private byte[] toJpeg(BufferedImage image) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        var writers = ImageIO.getImageWritersByFormatName("jpeg");
        var writer  = writers.next();
        var param   = writer.getDefaultWriteParam();

        param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(JPEG_QUALITY);

        try (var ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            writer.write(null,
                    new javax.imageio.IIOImage(image, null, null),
                    param);
        } finally {
            writer.dispose();
        }

        return baos.toByteArray();
    }

    // ─── Captura única ───────────────────────────────────────────────────────────

    public byte[] captureOnce() throws Exception {
        BufferedImage screenshot = robot.createScreenCapture(screenBounds);
        return toJpeg(screenshot);
    }

    public void shutdown() {
        stop();
        scheduler.shutdown();
    }
}