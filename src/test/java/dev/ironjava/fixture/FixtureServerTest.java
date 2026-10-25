package dev.ironjava.fixture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

class FixtureServerTest {

    @Test
    void servesEscapedHealthPageAndRejectsOtherRequests() throws Exception {
        RuntimeConfig config = RuntimeConfig.parse("""
                serviceName: "a<b>"
                port: 0
                """);
        TokenCatalog catalog = TokenCatalog.of(Map.of("role", "demo"));
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

        try (FixtureServer server = new FixtureServer(config, new NoticeBoard(), catalog)) {
            server.start();
            URI base = URI.create("http://127.0.0.1:" + server.port());

            HttpResponse<String> health = client.send(
                    HttpRequest.newBuilder(base.resolve("/health")).timeout(Duration.ofSeconds(5)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            HttpResponse<String> missing = client.send(
                    HttpRequest.newBuilder(base.resolve("/other")).timeout(Duration.ofSeconds(5)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            HttpResponse<String> posted = client.send(
                    HttpRequest.newBuilder(base.resolve("/health"))
                            .timeout(Duration.ofSeconds(5))
                            .POST(HttpRequest.BodyPublishers.noBody())
                            .build(),
                    HttpResponse.BodyHandlers.ofString());

            assertEquals(200, health.statusCode());
            assertEquals("text/html; charset=UTF-8", health.headers().firstValue("Content-Type").orElseThrow());
            assertTrue(health.body().contains("<h1>a&lt;b&gt; is up</h1>"));
            assertTrue(health.body().contains("role=demo"));
            assertFalse(health.body().contains("a<b>"));

            assertEquals(404, missing.statusCode());
            assertEquals("not found", missing.body());

            assertEquals(405, posted.statusCode());
        }
    }
}
