package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;

@Data
@AllArgsConstructor
public class SshResponse { // TODO: 18/07/2024 parameters indicating the state of the connection
    @Required
    private String host;
    private int port;
    private boolean connectionStatus;
}
