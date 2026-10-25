package dev.ironjava.fixture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class TokenCatalogTest {

    @Test
    void joinsPairsInInsertionOrder() {
        Map<String, String> tokens = new LinkedHashMap<>();
        tokens.put("service", "fixture");
        tokens.put("route", "health");
        TokenCatalog catalog = TokenCatalog.of(tokens);

        assertEquals("service=fixture, route=health", catalog.joinPairs());
        assertEquals(tokens, catalog.tokens());
    }

    @Test
    void emptyCatalogJoinsToEmptyString() {
        assertEquals("", TokenCatalog.of(Map.of()).joinPairs());
    }

    @Test
    void rejectsNullSourceMap() {
        assertThrows(NullPointerException.class, () -> TokenCatalog.of(null));
    }
}
