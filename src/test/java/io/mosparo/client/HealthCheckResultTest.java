package io.mosparo.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HealthCheckResultTest {

    @Test
    void testHealthy() {
        HealthCheckResult result = new HealthCheckResult("test", true, "amazing", null, 200);
        assertTrue(result.isHealthy());
    }

    @Test
    void testUnhealthy() {
        HealthCheckResult result = new HealthCheckResult("test", false, "feeling bad", "error", 500);
        assertFalse(result.isHealthy());
    }

    @Test
    void testNullIsUnhealthy() {
        HealthCheckResult result = new HealthCheckResult("test", null, "it's null", "error", 500);
        assertFalse(result.isHealthy());
    }
}