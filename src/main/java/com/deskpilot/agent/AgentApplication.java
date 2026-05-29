package com.deskpilot.agent;

import com.deskpilot.agent.config.AgentConfig;
import com.deskpilot.agent.core.AgentService;
import com.deskpilot.agent.ui.AgentApp;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AgentApplication {

    private static AgentService agentService;
    private static AgentConfig  config;

    public static void main(String[] args) {
        AgentApp.main(args);
    }

    public static void init(AgentConfig cfg, AgentService service) {
        config       = cfg;
        agentService = service;
    }

    public static AgentService getAgentService() {
        return agentService;
    }

    public static AgentConfig getConfig() {
        return config;
    }
}