package org.example.service;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.dto.TelnetStream;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Optional;

@Service
public class CustomFTPClient {

    private static final Logger logger = LogManager.getLogger(CustomFTPClient.class);

    // Constants
    private static final int CONNECTION_TIMEOUT = 3000;
    private static final int COMMAND_SLEEP_INTERVAL = 35;
    private static final int TELNET_SLEEP_INTERVAL = 20;
    private static final int MAX_EMPTY_COUNT = 100;
    private static final int BUFFER_SIZE = 2000;

    private static final String STRICT_HOST_KEY_CHECKING = "StrictHostKeyChecking";
    private static final String CHANNEL_TYPE_SHELL = "shell";
    private static final String CONNECTION_REFUSED = "Connection refused";
    private static final String ESCAPE_CHARACTER = "Escape character is ";
    private static final String NES_SIP_PROMPT = "NES-SIP>";

    /**
     * Establishes an SSH session.
     *
     * @param username The username for SSH login.
     * @param password The password for SSH login.
     * @param host     The SSH server host.
     * @param port     The SSH server port.
     * @return The established session or an empty optional if the connection fails.
     */
    public Optional<Session> connectionSession(final String username, final String password, final String host, final int port) {
        try {
            final JSch jsch = new JSch();
            final Session session = jsch.getSession(username, host, port);
            session.setConfig(STRICT_HOST_KEY_CHECKING, "no");
            session.setPassword(password);
            session.setTimeout(CONNECTION_TIMEOUT);
            session.connect();
            logger.info("Connection established. Host: {} Port: {}", host, port);
            return Optional.of(session);
        } catch (JSchException e) {
            logger.error("Failed to establish connection to {}:{}. Error: {}", host, port, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Establishes a Telnet connection through the given SSH session.
     *
     * @param session The established SSH session.
     * @param ip      The Telnet server IP.
     * @param port    The Telnet server port.
     * @return The TelnetStream for communication.
     * @throws JSchException, IOException If the connection fails.
     */
    public TelnetStream connectionTelnet(final Session session, final String ip, final int port) throws JSchException, IOException {
        final Channel channel = session.openChannel(CHANNEL_TYPE_SHELL);
        channel.connect(CONNECTION_TIMEOUT);

        final DataOutputStream outputStream = new DataOutputStream(channel.getOutputStream());
        final DataInputStream inputStream = new DataInputStream(channel.getInputStream());
        channel.setInputStream(inputStream, true);

        TelnetStream telnetStream = new TelnetStream(outputStream, inputStream, channel, true);
        boolean isConnect = connectTelnetWithoutUserAndPass(telnetStream, ip, port);
        telnetStream.setIsConnect(isConnect);
        return telnetStream;
    }

    /**
     * Executes a command on the remote server and waits for a specific output.
     *
     * @param outputStream The output stream to send the command.
     * @param inputStream  The input stream to read the response.
     * @param cmd          The command to execute.
     * @param waitFor      The expected response string to wait for.
     * @return The command output.
     */
    public String runCommand(OutputStream outputStream, InputStream inputStream, String cmd, String waitFor) {
        if (StringUtils.isBlank(cmd)) {
            return StringUtils.EMPTY;
        }
        if (StringUtils.isEmpty(waitFor)) {
            waitFor = CONNECTION_REFUSED;
        }
        StringBuilder builder = new StringBuilder();
        try {
            outputStream.write((cmd + "\r\n").getBytes());
            outputStream.flush();
            int emptyCount = 0;
            while (true) {
                String read = read(inputStream);
                Thread.sleep(COMMAND_SLEEP_INTERVAL);
                if (read != null && !read.isEmpty()) {
                    builder.append(read);
                    if (read.contains(waitFor)) {
                        break;
                    }
                    continue;
                }
                if (emptyCount++ > MAX_EMPTY_COUNT) {
                    break;
                }
            }
            logger.info("Command output: {}", builder.toString().trim());
        } catch (IOException | InterruptedException e) {
            logger.error("Failed to execute command '{}'. Error: {}", cmd, e.getMessage(), e);
        }
        return builder.toString();
    }

    /**
     * Reads the input stream and returns the output as a string.
     *
     * @param inputStream The input stream to read from.
     * @return The string read from the input stream.
     */
    public String read(InputStream inputStream) {
        final StringBuilder commandOutput = new StringBuilder();
        byte[] buffer = new byte[BUFFER_SIZE];
        try {
            int availableBytes = inputStream.available();
            int bytesRead = (availableBytes > 0) ? inputStream.read(buffer, 0, Math.min(availableBytes, BUFFER_SIZE)) : 0;
            if (bytesRead > 0) {
                commandOutput.append(new String(buffer, 0, bytesRead));
            }
        } catch (IOException e) {
            logger.error("Error reading from input stream. Error: {}", e.getMessage(), e);
        }
        return commandOutput.toString();
    }

    /**
     * Connects to the Telnet server without user credentials.
     *
     * @param telnetStream The Telnet stream.
     * @param ip           The Telnet server IP.
     * @param port         The Telnet server port.
     * @return True if connected, otherwise false.
     */
    public boolean connectTelnetWithoutUserAndPass(final TelnetStream telnetStream, final String ip, int port) {
        try (OutputStream stdin = telnetStream.getOutputStream()) {
            final String cmd = "telnet " + ip + " " + port;

            stdin.write((cmd + "\r\n").getBytes());
            stdin.flush();
            int emptyCount = 0;
            StringBuilder builder = new StringBuilder();
            boolean result = true;
            while (true) {
                String read = read(telnetStream.getInputStream());
                Thread.sleep(TELNET_SLEEP_INTERVAL);
                if (read != null && !read.isEmpty()) {
                    builder.append(read);
                    if (read.contains(CONNECTION_REFUSED)) {
                        result = false;
                        break;
                    }
                    if (read.contains(ESCAPE_CHARACTER) || read.contains(NES_SIP_PROMPT)) {
                        break;
                    }
                    continue;
                }
                if (emptyCount++ > MAX_EMPTY_COUNT) {
                    break;
                }
            }
            logger.info("Telnet connection output: {}", builder.toString());
            return result;
        } catch (IOException | InterruptedException e) {
            logger.error("Failed to connect to Telnet server at {}:{}. Error: {}", ip, port, e.getMessage(), e);
            return false;
        }
    }
}



