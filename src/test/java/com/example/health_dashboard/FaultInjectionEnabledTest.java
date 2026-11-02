package com.example.health_dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"ENABLE_FAULT_INJECTION=true"})
class FaultInjectionEnabledTest {

    @Value("${local.server.port}")
    private int port;

    @Test
    void simulateErrorReturns500WhenEnabled() throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/simulate-error")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(500, response.statusCode(),
                "/simulate-error should return 500 when ENABLE_FAULT_INJECTION=true");
    }
}