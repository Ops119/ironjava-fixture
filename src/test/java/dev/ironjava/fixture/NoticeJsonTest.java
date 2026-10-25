package dev.ironjava.fixture;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

/** Exercises the test-scoped Gson API. This dependency is not on the production classpath. */
class NoticeJsonTest {

    @Test
    void roundTripsAStatusPayload() {
        Gson gson = new Gson();
        StatusPayload payload = new StatusPayload("ironjava-fixture", "healthCheck");

        String json = gson.toJson(payload);
        StatusPayload restored = gson.fromJson(json, StatusPayload.class);

        assertEquals(payload, restored);
        assertEquals("{\"serviceName\":\"ironjava-fixture\",\"routeId\":\"healthCheck\"}", json);
    }

    static final class StatusPayload {
        final String serviceName;
        final String routeId;

        StatusPayload(String serviceName, String routeId) {
            this.serviceName = serviceName;
            this.routeId = routeId;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof StatusPayload payload)) {
                return false;
            }
            return serviceName.equals(payload.serviceName) && routeId.equals(payload.routeId);
        }

        @Override
        public int hashCode() {
            return serviceName.hashCode() * 31 + routeId.hashCode();
        }
    }
}
