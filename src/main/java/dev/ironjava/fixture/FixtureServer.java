package dev.ironjava.fixture;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/** Small HTTP service that serves one HTML health page. */
public final class FixtureServer implements AutoCloseable {

    private final HttpServer server;
    private final ExecutorService executor;

    public FixtureServer(RuntimeConfig config, NoticeBoard board, TokenCatalog catalog) throws IOException {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(board, "board");
        Objects.requireNonNull(catalog, "catalog");
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", config.port()), 0);
        this.executor = Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "fixture-http");
            thread.setDaemon(true);
            return thread;
        });
        this.server.setExecutor(executor);
        this.server.createContext("/", exchange -> handle(exchange, config, board, catalog));
    }

    public void start() {
        server.start();
    }

    public int port() {
        return server.getAddress().getPort();
    }

    @Override
    public void close() {
        server.stop(0);
        executor.shutdownNow();
    }

    public static void main(String[] args) throws Exception {
        String yaml = readResource("/fixture-config.yml");
        RuntimeConfig config = RuntimeConfig.parse(yaml);
        NoticeBoard board = new NoticeBoard();
        TokenCatalog catalog = TokenCatalog.of(Map.of("service", config.serviceName(), "route", "health"));
        FixtureServer server = new FixtureServer(config, board, catalog);
        server.start();
        System.out.println("ironjava-fixture listening on 127.0.0.1:" + server.port());
        Thread.currentThread().join();
    }

    private static void handle(
            HttpExchange exchange,
            RuntimeConfig config,
            NoticeBoard board,
            TokenCatalog catalog) throws IOException {
        try {
            if (!"GET".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            if (!"/health".equals(exchange.getRequestURI().getPath())) {
                write(exchange, 404, "text/plain; charset=UTF-8", "not found");
                return;
            }
            write(exchange, 200, "text/html; charset=UTF-8", board.healthHtml(config, catalog));
        } finally {
            exchange.close();
        }
    }

    private static void write(HttpExchange exchange, int status, String contentType, String body)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static String readResource(String name) throws IOException {
        try (InputStream in = FixtureServer.class.getResourceAsStream(name)) {
            if (in == null) {
                throw new IllegalStateException("missing resource " + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
