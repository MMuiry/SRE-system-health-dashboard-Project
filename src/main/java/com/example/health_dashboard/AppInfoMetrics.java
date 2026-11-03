package com.example.health_dashboard;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.prometheus.metrics.core.metrics.Info;

@Component
public class AppInfoMetrics {

    public AppInfoMetrics(PrometheusMeterRegistry registry,
                          @Value("${app.version}") String version,
                          @Value("${app.environment}") String environment) {
        Info info = Info.builder()
                .name("app_info")
                .help("Running version and environment (value is always 1)")
                .labelNames("version", "environment")
                .register(registry.getPrometheusRegistry());
        info.setLabelValues(version, environment);
    }
}