package com.example.health_dashboard.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class FaultInjectionController {
    private static final Logger log = LoggerFactory.getLogger(FaultInjectionController.class);
    private final boolean enabled;

    public FaultInjectionController(@Value("${app.fault-injection.enabled}") boolean enabled) {
        this.enabled = enabled;
    }

    @GetMapping("/simulate-error")
    public ResponseEntity<Map<String, Object>> simulateError() {
        if (!enabled) {
            return ResponseEntity.status(404).body(Map.of("error", "Not Found"));
        }
        log.error("Simulated failure triggered on /simulate-error");
        return ResponseEntity.status(500).body(Map.of("error", "Simulated Failure"));
    }

}
