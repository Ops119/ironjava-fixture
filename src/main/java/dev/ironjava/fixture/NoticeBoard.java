package dev.ironjava.fixture;

import java.util.Map;
import java.util.Objects;

import org.apache.commons.text.CaseUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.WordUtils;

/**
 * Formats status copy with Commons Text.
 *
 * <p>Substitution uses a caller-supplied map. This class does not install the
 * default interpolator ({@code StringSubstitutor.createInterpolator()}).
 */
public final class NoticeBoard {

    static final String HEALTH_TEMPLATE = """
            <!DOCTYPE html>
            <html lang="en">
            <head><meta charset="utf-8"><title>${service}</title></head>
            <body>
            <h1>${service} is up</h1>
            <p>Tokens: ${pairs}</p>
            </body>
            </html>
            """;

    public String substitute(String template, Map<String, String> tokens) {
        Objects.requireNonNull(template, "template");
        Objects.requireNonNull(tokens, "tokens");
        return new StringSubstitutor(tokens).replace(template);
    }

    public String escapeHtml(String value) {
        Objects.requireNonNull(value, "value");
        return StringEscapeUtils.escapeHtml4(value);
    }

    public String title(String raw) {
        Objects.requireNonNull(raw, "raw");
        return WordUtils.capitalizeFully(raw);
    }

    public String routeId(String raw) {
        Objects.requireNonNull(raw, "raw");
        return CaseUtils.toCamelCase(raw, false, '-');
    }

    public String healthHtml(RuntimeConfig config, TokenCatalog catalog) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(catalog, "catalog");
        return substitute(HEALTH_TEMPLATE, Map.of(
                "service", escapeHtml(config.serviceName()),
                "pairs", escapeHtml(catalog.joinPairs())));
    }
}
