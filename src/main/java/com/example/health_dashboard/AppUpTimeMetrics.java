package com.example.health_dashboard;

import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class AppUpTimeMetrics {
    private final Instant startedAt =  Instant.now();

    public AppUpTimeMetrics(MeterRegistry registry) {
        Gauge.builder("app_uptime_seconds", this, metrics -> metrics.uptimeSeconds())
                .description("Seconds since Application started")
                .register(registry);
    }

    private double uptimeSeconds() {
        return Duration.between(startedAt, Instant.now()).toMillis()/1000.0;
    }
}
