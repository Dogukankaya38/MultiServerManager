package org.example.dto;

import lombok.Data;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;

import java.util.List;

@Data
public class SessionDto { // TODO: 8/11/2022  Parameters required for ssh and telnet connection

    private List<String> userName;
    private List<String> password;
    @Required
    private List<String> host;
    private List<Integer> port; // OPTIONEL
    private List<Integer> telnetPort;
    @Required
    private List<String> commands;
    private List<String> waitFors;// OPTIONEL
}
