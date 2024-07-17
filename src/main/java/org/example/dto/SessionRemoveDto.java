package org.example.dto;

import lombok.Data;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;

import java.util.List;

@Data
public class SessionRemoveDto {
    @Required
    private List<String> sessionId;
}
