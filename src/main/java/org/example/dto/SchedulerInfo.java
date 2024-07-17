package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;

import java.util.UUID;

@Data
@AllArgsConstructor
public class SchedulerInfo {
    @Required
    private String host;
    private int telnetPort;
    private UUID id;
    private Boolean connectionStatus;
}
