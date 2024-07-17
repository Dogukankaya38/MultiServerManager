package org.example.dto;

import com.jcraft.jsch.Channel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.DataOutputStream;
import java.io.InputStream;

@Data
@AllArgsConstructor
public class TelnetStream {

    private DataOutputStream outputStream;

    private InputStream inputStream;

    private Channel channel;
    private Boolean isConnect;

}
