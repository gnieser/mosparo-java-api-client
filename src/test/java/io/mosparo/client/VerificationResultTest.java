package io.mosparo.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class VerificationResultTest {

    @Test
    void testVerificationResult() {
        Map<String, String> verifiedFields = Map.of("name", VerificationResult.FIELD_VALID);

        VerificationResult vr = new VerificationResult(true, "sign", verifiedFields, null, false, "");

        assertTrue(vr.isValid());
        assertEquals(verifiedFields, vr.getVerifiedFields());
        assertEquals("valid", vr.getVerifiedFields().get("name"));
        assertFalse(vr.hasIssues());
        assertFalse(vr.hasError());
    }

    @Test
    void testVerificationResultInvalid() {
        Map<String, String> verifiedFields = Map.of(
                "name", VerificationResult.FIELD_VALID,
                "street", VerificationResult.FIELD_INVALID,
                "number", VerificationResult.FIELD_NOT_VERIFIED);
        List<VerificationResult.Issue> issues = List.of(
                new VerificationResult.Issue("street", "Missing in form data, verification not possible"));

        VerificationResult vr = new VerificationResult(false, "sign", verifiedFields, issues, null, "");

        assertFalse(vr.isValid());
        assertEquals(verifiedFields, vr.getVerifiedFields());
        assertEquals("valid", vr.getVerifiedFields().get("name"));
        assertEquals("invalid", vr.getVerifiedFields().get("street"));
        assertEquals("not-verified", vr.getVerifiedFields().get("number"));
        assertTrue(vr.hasIssues());
        assertEquals(issues, vr.getIssues());
    }

    @Test
    void testVerificationResultError() {
        VerificationResult vr = new VerificationResult(null, null, null, null, true, "Error");

        assertTrue(vr.hasError());
        assertEquals("Error", vr.getErrorMessage());
    }
}
