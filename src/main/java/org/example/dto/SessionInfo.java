package org.example.dto;

import com.jcraft.jsch.Session;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;

import java.util.UUID;

@Data
@AllArgsConstructor
public class SessionInfo {
    private Session session;
    @Required
    private String ip;
    private int telnetPort;
    private TelnetStream telnetStream;
    private UUID uuid;
    private Boolean isConnect;
    @Required
    private String command;
    private String waitFor;

}
