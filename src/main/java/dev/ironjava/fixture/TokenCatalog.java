package dev.ironjava.fixture;

import java.util.Map;
import java.util.Objects;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;

/** Immutable token map rendered with Guava. */
public final class TokenCatalog {

    private static final Joiner.MapJoiner PAIR_JOINER =
            Joiner.on(", ").withKeyValueSeparator("=");

    private final ImmutableMap<String, String> tokens;

    private TokenCatalog(ImmutableMap<String, String> tokens) {
        this.tokens = tokens;
    }

    public static TokenCatalog of(Map<String, String> tokens) {
        Objects.requireNonNull(tokens, "tokens");
        return new TokenCatalog(ImmutableMap.copyOf(tokens));
    }

    public ImmutableMap<String, String> tokens() {
        return tokens;
    }

    public String joinPairs() {
        return PAIR_JOINER.join(tokens);
    }
}
