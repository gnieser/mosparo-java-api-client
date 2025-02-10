package io.mosparo.client;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Value;

/**
 * Verification request of a form submission.
 *
 * @see <a href="https://documentation.mosparo.io/docs/api/verification#request">mosparo API documentation</a>
 */
@Value
public class VerificationRequest {

    String submitToken;
    String validationSignature;
    String formSignature;
    Map<String, Object> formData;

    // The verification signature is not sent with the request, it serves to verify the result
    @JsonIgnore
    String verificationSignature;
}