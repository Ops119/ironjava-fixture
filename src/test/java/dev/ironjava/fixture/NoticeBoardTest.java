package dev.ironjava.fixture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

class NoticeBoardTest {

    private final NoticeBoard board = new NoticeBoard();

    @Test
    void substitutesOnlySuppliedTokens() {
        String rendered = board.substitute("Hello ${name}, port ${port}", Map.of("name", "fixture", "port", "8080"));

        assertEquals("Hello fixture, port 8080", rendered);
    }

    @Test
    void leavesUnknownTokensUntouched() {
        String rendered = board.substitute("value=${script:javascript:1+1}", Map.of("name", "fixture"));

        assertEquals("value=${script:javascript:1+1}", rendered);
    }

    @Test
    void escapesHtmlReservedCharacters() {
        assertEquals("a&lt;b&amp;c&gt;", board.escapeHtml("a<b&c>"));
    }

    @Test
    void capitalizesTitleWords() {
        assertEquals("Iron Java Fixture", board.title("iron java fixture"));
    }

    @Test
    void buildsCamelCaseRouteId() {
        assertEquals("healthCheck", board.routeId("health-check"));
    }

    @Test
    void healthPageEscapesServiceNameBeforeSubstitution() {
        RuntimeConfig config = new RuntimeConfig("a<b>", 0);
        TokenCatalog catalog = TokenCatalog.of(Map.of("role", "demo&ops"));

        String html = board.healthHtml(config, catalog);

        assertTrue(html.contains("<h1>a&lt;b&gt; is up</h1>"));
        assertTrue(html.contains("Tokens: role=demo&amp;ops"));
        assertFalse(html.contains("a<b>"));
        assertFalse(html.contains("demo&ops"));
    }
}
