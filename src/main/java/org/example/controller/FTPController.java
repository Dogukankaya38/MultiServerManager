package org.example.controller;

import org.example.config.SchedulerConfiguration;
import org.example.dto.SchedulerInfo;
import org.example.dto.SessionDto;
import org.example.dto.SessionRemoveDto;
import org.example.dto.SshResponse;
import org.example.service.FTPService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ftp")
public class FTPController {

    private final FTPService ftpService;

    @Autowired
    public FTPController(FTPService ftpService) {
        this.ftpService = ftpService;
    }

    @PostMapping("/connect")
    public List<SshResponse> connectSSH(@RequestBody SessionDto sessionDto) throws Exception {
        return ftpService.connectSSH(sessionDto);
    }

    @PostMapping("/add")
    public List<SshResponse> addSSH(@RequestBody SessionDto sessionDto) throws Exception {
        return ftpService.addSSH(sessionDto);
    }

    @PostMapping("/start")
    public String configureScheduler(@RequestBody SchedulerConfiguration schedulerConfiguration) {
        return ftpService.configureScheduler(schedulerConfiguration);
    }

    @GetMapping("/list")
    public List<SchedulerInfo> listSchedulers() {
        return ftpService.listSchedulers();
    }

    @PostMapping("/remove")
    public String removeScheduler(@RequestBody SessionRemoveDto sessionDto) {
        return ftpService.removeScheduler(sessionDto);
    }

    @GetMapping("/stop")
    public String stopScheduler() {
        return ftpService.stopScheduler();
    }

    @GetMapping("/clear")
    public String clearSchedulers() {
        return ftpService.clearSchedulers();
    }
}
