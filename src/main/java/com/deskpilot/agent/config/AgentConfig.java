package com.deskpilot.agent.config;

import lombok.Data;

import java.util.prefs.Preferences;

@Data
public class AgentConfig {

    private static final String PREF_RELAY_URL  = "relay_url";
    private static final String PREF_USERNAME   = "username";
    private static final String PREF_TOKEN      = "token";
    private static final String PREF_FPS        = "fps";
    private static final String PREF_QUALITY    = "quality";

    private static final String DEFAULT_RELAY_URL = "ws://localhost:8080/relay";
    private static final int    DEFAULT_FPS        = 24;
    private static final float  DEFAULT_QUALITY    = 0.6f;

    private final Preferences prefs;

    private String relayUrl;
    private String username;
    private String token;
    private int    fps;
    private float  quality;

    public AgentConfig() {
        this.prefs = Preferences.userNodeForPackage(AgentConfig.class);
        load();
    }

    // ─── Carregar configurações salvas ───────────────────────────────────────────

    public void load() {
        this.relayUrl = prefs.get(PREF_RELAY_URL, DEFAULT_RELAY_URL);
        this.username = prefs.get(PREF_USERNAME, "");
        this.token    = prefs.get(PREF_TOKEN, "");
        this.fps      = prefs.getInt(PREF_FPS, DEFAULT_FPS);
        this.quality  = prefs.getFloat(PREF_QUALITY, DEFAULT_QUALITY);
    }

    // ─── Salvar configurações ────────────────────────────────────────────────────

    public void save() {
        prefs.put(PREF_RELAY_URL, relayUrl);
        prefs.put(PREF_USERNAME, username);
        prefs.put(PREF_TOKEN, token);
        prefs.putInt(PREF_FPS, fps);
        prefs.putFloat(PREF_QUALITY, quality);
    }

    // ─── Limpar token (logout) ───────────────────────────────────────────────────

    public void clearToken() {
        this.token = "";
        prefs.remove(PREF_TOKEN);
    }

    public boolean hasToken() {
        return token != null && !token.isBlank();
    }

    public boolean hasCredentials() {
        return username != null && !username.isBlank();
    }
}