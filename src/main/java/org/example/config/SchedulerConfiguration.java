package org.example.config;

import lombok.Data;

import java.util.concurrent.TimeUnit;

@Data
public class SchedulerConfiguration {
    private int initialDelay;
    private int delay;
    private TimeUnit timeUnit;
}
