package dev.ironjava.fixture;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.text.lookup.StringLookup;
import org.apache.commons.text.lookup.StringLookupFactory;
import org.junit.jupiter.api.Test;

/**
 * Locks the Commons Text 1.9 default lookup set. Version 1.10.0 no longer
 * registers dns, url, or script by default, so this test fails after that upgrade.
 * The lookups are not invoked.
 */
class LegacyDefaultLookupsTest {

    @Test
    void defaultLookupsIncludeDnsUrlAndScript() {
        Map<String, StringLookup> lookups = new HashMap<>();
        StringLookupFactory.INSTANCE.addDefaultStringLookups(lookups);

        assertTrue(lookups.containsKey(StringLookupFactory.KEY_DNS), "expected dns in the default lookup set");
        assertTrue(lookups.containsKey(StringLookupFactory.KEY_URL), "expected url in the default lookup set");
        assertTrue(lookups.containsKey(StringLookupFactory.KEY_SCRIPT), "expected script in the default lookup set");
    }
}
