package com.example.health_dashboard;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
public class RequestMetricsFilter  extends OncePerRequestFilter  {
    private final MeterRegistry registry;

    public RequestMetricsFilter(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if ("/metrics".equals(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        long start = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally{
            String endpoint = endpointOf(request);
            String status = String.valueOf(response.getStatus());

            Counter.builder("http_requests")
                    .description("Total HTTP requests")
                    .tag("method", request.getMethod())
                    .tag("endpoint", endpoint)
                    .tag("status", status)
                    .register(registry)
                    .increment();

            Timer.builder("http_request_duration")
                    .description("HTTP request latency")
                    .tag("endpoint", endpoint)
                    .publishPercentileHistogram()
                    .register(registry)
                    .record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        }


    }
    private String endpointOf(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return pattern != null ? pattern.toString() : "other";
    }

}
