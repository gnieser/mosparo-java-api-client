package io.mosparo.client;

import lombok.Value;

/**
 * Represents the result of mosparo server health check.
 *
 * @see <a href="https://documentation.mosparo.io/docs/api/health#response">mosparo API documentation</a>
 */
@Value
public class HealthCheckResult {

    String service;
    Boolean healthy;
    String databaseStatus;
    String error;

    public boolean isHealthy() {
        return healthy != null && healthy;
    }
}