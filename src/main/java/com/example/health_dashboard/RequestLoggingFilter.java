package com.example.health_dashboard;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if ("/metrics".equals(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        MDC.put("method",  request.getMethod());
        MDC.put("endpoint", request.getRequestURI());

        try {
           chain.doFilter(request, response);
        } finally {
            int status = response.getStatus();
            Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            MDC.put("endpoint", pattern != null ? pattern.toString() : "other");
            MDC.put("status", String.valueOf(status));

            if (status >= 500) {
                log.error("Request failed");
            } else if (status >= 400) {
                log.warn("Request rejected");
            } else {
                log.info("Request handled");
            }
            MDC.clear();
        }
    }

}
