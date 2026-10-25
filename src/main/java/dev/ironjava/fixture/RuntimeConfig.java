package dev.ironjava.fixture;

import java.util.Map;
import java.util.Objects;

import org.yaml.snakeyaml.Yaml;

/** Loads the service name and listen port from a small YAML document. */
public record RuntimeConfig(String serviceName, int port) {

    public RuntimeConfig {
        if (serviceName == null || serviceName.isBlank()) {
            throw new IllegalArgumentException("serviceName is required");
        }
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("port out of range: " + port);
        }
    }

    public static RuntimeConfig parse(String yamlText) {
        Objects.requireNonNull(yamlText, "yamlText");
        Object loaded = new Yaml().load(yamlText);
        if (!(loaded instanceof Map<?, ?> document)) {
            throw new IllegalArgumentException("config must be a YAML mapping");
        }
        Object name = document.get("serviceName");
        Object portValue = document.get("port");
        if (!(name instanceof String serviceName)) {
            throw new IllegalArgumentException("serviceName must be a string");
        }
        if (!(portValue instanceof Number portNumber)) {
            throw new IllegalArgumentException("port must be a number");
        }
        return new RuntimeConfig(serviceName, portNumber.intValue());
    }
}
