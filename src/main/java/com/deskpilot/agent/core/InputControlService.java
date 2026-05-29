package com.deskpilot.agent.core;

import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

@Slf4j
public class InputControlService {

    private final Robot robot;

    public InputControlService() throws AWTException {
        this.robot = new Robot();
        this.robot.setAutoDelay(0);
        this.robot.setAutoWaitForIdle(false);
    }

    // ─── Mouse ───────────────────────────────────────────────────────────────────

    public void mouseMove(int x, int y) {
        try {
            robot.mouseMove(x, y);
        } catch (Exception e) {
            log.error("Erro ao mover mouse: {}", e.getMessage());
        }
    }

    public void mouseClick(int x, int y, int button) {
        try {
            int mask = getMask(button);
            robot.mouseMove(x, y);
            robot.mousePress(mask);
            robot.mouseRelease(mask);
        } catch (Exception e) {
            log.error("Erro ao clicar mouse: {}", e.getMessage());
        }
    }

    public void mousePress(int x, int y, int button) {
        try {
            robot.mouseMove(x, y);
            robot.mousePress(getMask(button));
        } catch (Exception e) {
            log.error("Erro ao pressionar mouse: {}", e.getMessage());
        }
    }

    public void mouseRelease(int button) {
        try {
            robot.mouseRelease(getMask(button));
        } catch (Exception e) {
            log.error("Erro ao soltar mouse: {}", e.getMessage());
        }
    }

    public void mouseScroll(int amount) {
        try {
            robot.mouseWheel(amount);
        } catch (Exception e) {
            log.error("Erro ao rolar mouse: {}", e.getMessage());
        }
    }

    // ─── Teclado ─────────────────────────────────────────────────────────────────

    public void keyPress(int keyCode) {
        try {
            robot.keyPress(keyCode);
        } catch (Exception e) {
            log.error("Erro ao pressionar tecla: {}", e.getMessage());
        }
    }

    public void keyRelease(int keyCode) {
        try {
            robot.keyRelease(keyCode);
        } catch (Exception e) {
            log.error("Erro ao soltar tecla: {}", e.getMessage());
        }
    }

    public void keyClick(int keyCode) {
        try {
            robot.keyPress(keyCode);
            robot.keyRelease(keyCode);
        } catch (Exception e) {
            log.error("Erro ao clicar tecla: {}", e.getMessage());
        }
    }

    public void typeText(String text) {
        try {
            for (char c : text.toCharArray()) {
                typeChar(c);
            }
        } catch (Exception e) {
            log.error("Erro ao digitar texto: {}", e.getMessage());
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private int getMask(int button) {
        return switch (button) {
            case 1 -> InputEvent.BUTTON1_DOWN_MASK;  // esquerdo
            case 2 -> InputEvent.BUTTON2_DOWN_MASK;  // meio
            case 3 -> InputEvent.BUTTON3_DOWN_MASK;  // direito
            default -> InputEvent.BUTTON1_DOWN_MASK;
        };
    }

    private void typeChar(char c) {
        try {
            if (Character.isLetter(c)) {
                int keyCode = KeyEvent.getExtendedKeyCodeForChar(c);
                if (Character.isUpperCase(c)) {
                    robot.keyPress(KeyEvent.VK_SHIFT);
                    robot.keyPress(keyCode);
                    robot.keyRelease(keyCode);
                    robot.keyRelease(KeyEvent.VK_SHIFT);
                } else {
                    robot.keyPress(keyCode);
                    robot.keyRelease(keyCode);
                }
            } else if (Character.isDigit(c)) {
                int keyCode = KeyEvent.getExtendedKeyCodeForChar(c);
                robot.keyPress(keyCode);
                robot.keyRelease(keyCode);
            } else {
                // Caracteres especiais via clipboard
                Toolkit.getDefaultToolkit()
                        .getSystemClipboard()
                        .setContents(new java.awt.datatransfer.StringSelection(String.valueOf(c)), null);
                robot.keyPress(KeyEvent.VK_CONTROL);
                robot.keyPress(KeyEvent.VK_V);
                robot.keyRelease(KeyEvent.VK_V);
                robot.keyRelease(KeyEvent.VK_CONTROL);
            }
        } catch (Exception e) {
            log.error("Erro ao digitar caractere '{}': {}", c, e.getMessage());
        }
    }

    // ─── Informações da tela ─────────────────────────────────────────────────────

    public Dimension getScreenSize() {
        return Toolkit.getDefaultToolkit().getScreenSize();
    }
}