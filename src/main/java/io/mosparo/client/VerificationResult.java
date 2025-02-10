package io.mosparo.client;

import java.util.List;
import java.util.Map;

import lombok.Value;

/**
 * Represents the result of submission verification.
 *
 * @see <a href="https://documentation.mosparo.io/docs/api/verification#response">mosparo API documentation</a>
 */
@Value
public class VerificationResult {

    public static final String FIELD_NOT_VERIFIED = "not-verified";
    public static final String FIELD_VALID = "valid";
    public static final String FIELD_INVALID = "invalid";

    @Value
    public static class Issue {
        String name;
        String message;

    }

    Boolean valid;
    String verificationSignature;
    Map<String, String> verifiedFields;
    List<Issue> issues;
    Boolean error;
    String errorMessage;

    public boolean isValid() {
        return this.valid != null && this.valid;
    }

    public boolean hasIssues() {
        return this.issues != null && !this.issues.isEmpty();
    }

    public boolean hasError() {
        return this.error != null && this.error;
    }
}