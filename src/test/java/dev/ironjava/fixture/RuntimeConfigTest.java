package dev.ironjava.fixture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class RuntimeConfigTest {

    @Test
    void parsesServiceNameAndPort() {
        RuntimeConfig config = RuntimeConfig.parse("""
                serviceName: fixture
                port: 8080
                """);

        assertEquals("fixture", config.serviceName());
        assertEquals(8080, config.port());
    }

    @Test
    void parsesPackagedConfigResource() throws Exception {
        String yaml;
        try (InputStream in = RuntimeConfigTest.class.getResourceAsStream("/fixture-config.yml")) {
            assertTrue(in != null, "fixture-config.yml is on the classpath");
            yaml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        RuntimeConfig config = RuntimeConfig.parse(yaml);

        assertEquals("ironjava-fixture", config.serviceName());
        assertEquals(0, config.port());
    }

    @Test
    void rejectsDocumentThatIsNotAMapping() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> RuntimeConfig.parse("- just-a-list"));

        assertEquals("config must be a YAML mapping", error.getMessage());
    }

    @Test
    void rejectsMissingServiceName() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> RuntimeConfig.parse("port: 9"));

        assertEquals("serviceName must be a string", error.getMessage());
    }

    @Test
    void rejectsNonNumericPort() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> RuntimeConfig.parse("""
                        serviceName: fixture
                        port: auto
                        """));

        assertEquals("port must be a number", error.getMessage());
    }

    @Test
    void rejectsPortOutsideRange() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> RuntimeConfig.parse("""
                        serviceName: fixture
                        port: 70000
                        """));

        assertEquals("port out of range: 70000", error.getMessage());
    }
}
