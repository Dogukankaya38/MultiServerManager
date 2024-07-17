package org.example.service;

import com.jcraft.jsch.Session;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.config.SchedulerConfiguration;
import org.example.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;

@Service
public class FTPService {
    private static final Logger logger = LogManager.getLogger(FTPService.class);

    private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(10);
    private Runnable runnable;
    private List<Callable<SessionInfo>> callableList = new ArrayList<>();
    private List<Future<SessionInfo>> futures = new ArrayList<>();

    private final CustomFTPClient customFTPClient;

    @Autowired
    public FTPService(CustomFTPClient customFTPClient) {
        this.customFTPClient = customFTPClient;
    }

    public List<SshResponse> connectSSH(SessionDto sessionDto) throws InterruptedException, ExecutionException {
        addCallableList(sessionDto);
        futures = scheduledExecutorService.invokeAll(callableList);
        initializeTelnet();
        return addSshResponseList();
    }

    public List<SshResponse> addSSH(SessionDto sessionDto) throws InterruptedException, ExecutionException {
        addCallableList(sessionDto);
        List<SshResponse> sshResponse = new ArrayList<>();
        int size = callableList.size();
        for (int i = 0; i < sessionDto.getUserName().size(); i++) {
            Future<SessionInfo> submit = scheduledExecutorService.submit(callableList.get(size - i - 1));
            sshResponse.add(new SshResponse(submit.get().getSession().getHost(),
                    submit.get().getTelnetPort(),
                    submit.get().getIsConnect()));
            futures.add(submit);
        }
        initializeTelnet();
        return sshResponse;
    }

    private void initializeTelnet() {
        if (futures.isEmpty()) return;
        runnable = () -> {
            for (Future<SessionInfo> obj : futures) {
                try {
                    TelnetStream telnetStream = obj.get().getTelnetStream();
                    DataOutputStream outputStream = telnetStream.getOutputStream();
                    InputStream inputStream = telnetStream.getInputStream();
                    if (!obj.get().getSession().isConnected() || !obj.get().getIsConnect()) {
                        System.out.println("connection not open");
                        futures.remove(obj);
                        return;
                    }
                    final String command = obj.get().getCommand();
                    String waitFor = obj.get().getWaitFor();

                    String outString = customFTPClient.runCommand(outputStream, inputStream, command, waitFor);
                    logger.info("Executed command '{}' on remote session. Response: {}", command, outString);

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    public String configureScheduler(SchedulerConfiguration schedulerConfiguration) {
        if (schedulerConfiguration.getDelay() < 20) {
            return "delay cannot be less than 20 seconds";
        }
        scheduledExecutorService.scheduleWithFixedDelay(runnable, schedulerConfiguration.getInitialDelay(), schedulerConfiguration.getDelay(), schedulerConfiguration.getTimeUnit());
        return "Scheduler is up and running...";
    }

    public List<SchedulerInfo> listSchedulers() {
        List<SchedulerInfo> schedulerInfoList = new ArrayList<>();
        futures.forEach(sessionInfoFuture -> {
            try {
                int telnetPort = sessionInfoFuture.get().getTelnetPort();
                String host = sessionInfoFuture.get().getIp();
                schedulerInfoList.add(new SchedulerInfo(host, telnetPort, sessionInfoFuture.get().getUuid(), sessionInfoFuture.get().getSession().isConnected()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return schedulerInfoList;
    }

    public String removeScheduler(SessionRemoveDto sessionDto) {
        StringBuilder message = new StringBuilder();
        for (Future<SessionInfo> sessionInfoFuture : futures) {
            try {
                UUID uuid = sessionInfoFuture.get().getUuid();
                for (String sessionId : sessionDto.getSessionId()) {
                    if (uuid.toString().equals(sessionId)) {
                        message.append("removed : ")
                                .append(sessionInfoFuture.get().getIp())
                                .append(" : ")
                                .append(sessionInfoFuture.get().getTelnetPort())
                                .append("\n");

                        disconnectSession(sessionInfoFuture);
                        futures.remove(sessionInfoFuture);
                        logger.info("Removed session with IP {} and Telnet port {}", sessionInfoFuture.get().getIp(), sessionInfoFuture.get().getTelnetPort());
                        break;
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            if (futures.isEmpty()) {
                break;
            }
        }
        initializeTelnet();
        return message.toString();
    }

    public String stopScheduler() {
        for (Future<SessionInfo> obj : futures) {
            try {
                disconnectSession(obj);
            } catch (Exception e) {
                logger.error("Error while disconnecting", e);
                return e.getMessage();
            }
        }
        if (!scheduledExecutorService.isShutdown()) {
            scheduledExecutorService.shutdown();
        }
        clearSchedulers();
        return "Scheduler stopped \nConnection closed by foreign host.";
    }

    public String clearSchedulers() {
        if (!scheduledExecutorService.isShutdown()) {
            return "This cannot be done while the schedule is running.";
        }
        runnable = null;
        callableList = new ArrayList<>();
        futures = new ArrayList<>();
        return "OK";
    }

    private List<SshResponse> addSshResponseList() {
        List<SshResponse> sshResponse = new ArrayList<>();
        futures.forEach(sessionFuture -> {
            try {
                sshResponse.add(new SshResponse(sessionFuture.get().getSession().getHost(), sessionFuture.get().getTelnetPort(), sessionFuture.get().getIsConnect()));
            } catch (Exception e) {
                logger.error("Error while adding ssh response", e);
                throw new RuntimeException(e);
            }
        });
        return sshResponse;
    }

    private void addCallableList(SessionDto sessionDto) {
        for (int i = 0; i < sessionDto.getUserName().size(); i++) {
            int finalI = i;
            callableList.add(() -> {
                // SSH
                String userName = sessionDto.getUserName().get(finalI);
                String password = sessionDto.getPassword().get(finalI);
                String host = sessionDto.getHost().get(finalI);
                String command = sessionDto.getCommands().get(finalI);
                String waitFor = sessionDto.getWaitFors().get(finalI);

                int port = 22;
                if (sessionDto.getPort() != null) {
                    port = sessionDto.getPort().get(finalI);
                }
                Optional<Session> session = customFTPClient.connectionSession(userName, password, host, port);

                if (session.isEmpty()) {
                    throw new RuntimeException("Failed to establish SSH session for user: " + userName + " on host: " + host);
                }

                // Telnet
                int telnetPort = sessionDto.getTelnetPort().get(finalI);
                TelnetStream telnetStream = customFTPClient.connectionTelnet(session.get(), host, telnetPort);
                boolean connect = telnetStream.getIsConnect();
                return new SessionInfo(session.get(), host, telnetPort, telnetStream, UUID.randomUUID(), connect, command, waitFor);
            });
        }
        if (scheduledExecutorService.isShutdown()) {
            scheduledExecutorService = Executors.newScheduledThreadPool(10);
        }
    }

    private void disconnectSession(Future<SessionInfo> obj) throws InterruptedException, ExecutionException, IOException {
        TelnetStream telnetStream = obj.get().getTelnetStream();
        DataOutputStream outputStream = telnetStream.getOutputStream();
        InputStream inputStream = telnetStream.getInputStream();
        Session session = obj.get().getSession();
        if (!session.isConnected()) return;
        customFTPClient.runCommand(outputStream, inputStream, "q", "closed by foreign host.");
        telnetStream.getOutputStream().close();
        telnetStream.getInputStream().close();
        telnetStream.getChannel().disconnect();
        if (session.isConnected()) {
            session.disconnect();
        }
    }
}


