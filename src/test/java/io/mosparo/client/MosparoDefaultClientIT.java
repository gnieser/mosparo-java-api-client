package io.mosparo.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

class MosparoDefaultClientIT extends AbstractMosparoIT {

    @Test
    void testVerifySubmission() throws IOException, MosparoException {
        Map<String, Object> formData = visitForm(Map.of(
                "firstname", "John",
                "lastname", "Example"));

        MosparoClient client = new MosparoDefaultClient(mosparoUrl, projectPublicKey, projectPrivateKey);
        VerificationResult result = client.verifySubmission(formData, Set.of("firstname", "lastname"));

        assertTrue(result.isValid());
        assertEquals(2, result.getVerifiedFields().size());
        assertTrue(result.getVerifiedFields().containsKey("firstname"));
        assertEquals(VerificationResult.FIELD_VALID, result.getVerifiedFields().get("firstname"));
        assertTrue(result.getVerifiedFields().containsKey("lastname"));
        assertEquals(VerificationResult.FIELD_VALID, result.getVerifiedFields().get("lastname"));

        assertFalse(result.hasIssues());
        assertFalse(result.hasError());
        assertNull(result.getErrorMessage());
    }

    @Test
    void testVerifySubmissionInvalidSubmitToken() {
        Map<String, Object> formData = visitForm(Map.of(
                "firstname", "John",
                "lastname", "Example"));

        // Alter the submit token
        formData.put("_mosparo_submitToken", "not-a-valid-token");

        MosparoClient client = new MosparoDefaultClient(mosparoUrl, projectPublicKey, projectPrivateKey);
        MosparoException thrown = assertThrows(MosparoException.class,
                () -> client.verifySubmission(formData, Set.of("firstname", "lastname")));

        assertEquals("Submit token not found or not valid.", thrown.getMessage());
    }

    @Test
    void testVerifySubmissionInvalidValidationToken() {
        Map<String, Object> formData = visitForm(Map.of(
                "firstname", "John",
                "lastname", "Example"));

        // Alter the validation token
        formData.put("_mosparo_validationToken", "not-a-valid-token");

        MosparoClient client = new MosparoDefaultClient(mosparoUrl, projectPublicKey, projectPrivateKey);
        MosparoException thrown = assertThrows(MosparoException.class,
                () -> client.verifySubmission(formData, Set.of("firstname", "lastname")));

        assertEquals("Verification failed.", thrown.getMessage());
    }

    @Test
    void testVerifySubmissionIsNotValid() throws IOException, MosparoException {
        Map<String, Object> formData = visitForm(Map.of(
                "firstname", "John",
                "lastname", "Example"));

        // Alter the data
        formData.put("firstname", "Not Valid");

        MosparoClient client = new MosparoDefaultClient(mosparoUrl, projectPublicKey, projectPrivateKey);
        VerificationResult result = client.verifySubmission(formData, Set.of("firstname", "lastname"));

        assertFalse(result.isValid());
        assertTrue(result.hasIssues());
        assertEquals(2, result.getVerifiedFields().size());
        assertEquals(VerificationResult.FIELD_INVALID, result.getVerifiedFields().get("firstname"));
        assertEquals(VerificationResult.FIELD_VALID, result.getVerifiedFields().get("lastname"));
        assertEquals(1, result.getIssues().size());
        assertEquals("firstname", result.getIssues().get(0).getName());
        assertEquals("Field not valid.", result.getIssues().get(0).getMessage());

        assertTrue(result.hasIssues());
        assertFalse(result.hasError());
        assertNull(result.getErrorMessage());
        assertNotNull(result.getVerificationSignature());
    }

    @Test
    void testStatisticByDate() throws IOException, MosparoException {
        // Ensures there is at least one submission
        Map<String, Object> formData = visitForm(Map.of(
                "firstname", "John",
                "lastname", "Example"));
        MosparoClient client = new MosparoDefaultClient(mosparoUrl, projectPublicKey, projectPrivateKey);
        VerificationResult v = client.verifySubmission(formData, Set.of("firstname", "lastname"));
        assertTrue(v.isValid());

        StatisticResult s = client.statisticByDate(null, null);

        assertTrue(s.getResult());
        assertThat(s.getData().getNumberOfValidSubmissions()).isGreaterThanOrEqualTo(1);
        assertThat(s.getData().getNumberOfSpamSubmissions()).isGreaterThanOrEqualTo(0);

        assertThat(s.getData().getNumbersByDate().size()).isGreaterThanOrEqualTo(1);
        LocalDate now = LocalDate.now();
        assertThat(s.getData().getNumbersByDate().get(now).getNumberOfValidSubmissions()).isGreaterThanOrEqualTo(1);
        assertThat(s.getData().getNumbersByDate().get(now).getNumberOfSpamSubmissions()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void testHealthCheck() throws IOException, MosparoException {
        MosparoClient client = new MosparoDefaultClient(mosparoUrl, projectPublicKey, projectPrivateKey);
        HealthCheckResult result = client.healthCheck();
        assertTrue(result.isHealthy());
    }
}