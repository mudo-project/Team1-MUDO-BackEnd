package com.academy.mudogroupware.sharedfile.infrastructure.external.google;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import com.sun.net.httpserver.HttpServer;

class GoogleDriveConfigTest {

    private HttpServer server;
    private final AtomicReference<String> requestMethod = new AtomicReference<>();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/files/item-id", exchange -> {
            requestMethod.set(exchange.getRequestMethod());
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void googleDriveRestClientSendsPatchRequest() {
        RestClient client = new GoogleDriveConfig().googleDriveRestClient();

        client.patch()
                .uri("http://localhost:" + server.getAddress().getPort() + "/files/item-id")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"name\":\"변경된 파일명\"}")
                .retrieve()
                .toBodilessEntity();

        assertThat(requestMethod.get()).isEqualTo("PATCH");
    }
}
