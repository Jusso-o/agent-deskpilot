package com.deskpilot.agent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RelayMessage {

    private CommandType type;
    private String      sessionId;
    private String      sender;
    private String      payload;
    private long        timestamp;
}