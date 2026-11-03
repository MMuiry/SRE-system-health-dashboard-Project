package com.example.health_dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"ENVIRONMENT=junit", "APP_VERSION=9.9.9"})
class MetricsEndpointTest {

    private static final Pattern HEALTH_COUNT = Pattern.compile(
            "http_requests_total\\{endpoint=\"/health\",method=\"GET\",status=\"200\"\\} ([0-9.eE+-]+)");

    @Value("${local.server.port}")
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private double healthCount() throws Exception {
        Matcher m = HEALTH_COUNT.matcher(get("/metrics").body());
        return m.find() ? Double.parseDouble(m.group(1)) : 0;
    }

    @Test
    void metricsEndpointReturnsPrometheusText() throws Exception {
        HttpResponse<String> response = get("/metrics");
        assertEquals(200, response.statusCode(), "/metrics should return HTTP 200");
        assertTrue(response.body().contains("app_info{environment=\"junit\",version=\"9.9.9\"}"),
                () -> "/metrics should include app_info with version and environment, but returned: "
                        + response.body());
    }

    @Test
    void requestCounterIncreasesAfterCallingHealth() throws Exception {
        double before = healthCount();
        get("/health");
        double after = healthCount();
        assertTrue(after > before,
                "http_requests_total for /health should increase (before " + before + ", after " + after + ")");
    }

    @Test
    void uptimeMetricIsExposed() throws Exception {
        HttpResponse<String> response = get("/metrics");
        assertTrue(response.body().contains("app_uptime_seconds"),
                "/metrics should include app_uptime_seconds");
    }
}