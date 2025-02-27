package io.mosparo.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

@WireMockTest
class MosparoDefaultClientTest {

    private static String mosparoUrl;

    @BeforeAll
    static void prepare(WireMockRuntimeInfo wmRuntimeInfo) {
        mosparoUrl = wmRuntimeInfo.getHttpBaseUrl();
    }

    @Test
    void testVerifySubmissionIsValid() throws IOException, MosparoException {
        Map<String, Object> formData = Map.of(
                "lastname", "Example",
                "firstname", "John",
                "_mosparo_submitToken", "submitToken",
                "_mosparo_validationToken", "validationToken");

        stubFor(post("/api/v1/verification/verify")
                .willReturn(okJson("""
                        {
                          "valid": true,
                          "verificationSignature": "ec196315e575f7f5b250f4e95d88cd45442b17ef4ff36141ed090d2814e3d8a3",
                          "verifiedFields": {
                            "firstname": "valid",
                            "lastname": "valid"
                          },
                          "issues": []
                        }""")));

        MosparoClient client = new MosparoDefaultClient(mosparoUrl, "publicKey", "privateKey");
        VerificationResult result = client.verifySubmission(formData, Set.of("firstname", "lastname"));

        // Verify the request sent by the client
        verify(1, postRequestedFor(urlEqualTo("/api/v1/verification/verify"))
                .withRequestBody(equalToJson("""
                        {
                            "submitToken": "submitToken",
                            "validationSignature": "a20d6b36f87420850ef281ee977a554df2a749ed1892944e8ed22a1bc9c882a1",
                            "formSignature": "cca193d4d38890d7aa0c27bc590da0a2a98ffc304dc19e947b5da8d4498f015c",
                            "formData": {
                                "firstname":"a8cfcd74832004951b4408cdb0a5dbcd8c7e52d43f7fe244bf720582e05241da",
                                "lastname":"d029f87e3d80f8fd9b1be67c7426b4cc1ff47b4a9d0a8461c826a59d8c5eb6cd"
                            }
                        }
                        """)));

        // Verify the response processed by the client
        assertTrue(result.isValid());
        assertEquals(2, result.getVerifiedFields().size());
        assertEquals("valid", result.getVerifiedFields().get("firstname"));
        assertEquals("valid", result.getVerifiedFields().get("lastname"));

        assertFalse(result.hasIssues());
        assertFalse(result.hasError());
        assertNull(result.getErrorMessage());
    }

    @Test
    void testVerifySubmissionNullRequiredFields() {
        MosparoClient client = new MosparoDefaultClient(mosparoUrl, "publicKey", "privateKey");
        MosparoException thrown = assertThrows(MosparoException.class,
                () -> client.verifySubmission(Collections.emptyMap(), null));
        assertEquals("Required fields must be non null and non empty", thrown.getMessage());
    }

    @Test
    void testVerifySubmissionEmptyRequiredFields() {
        MosparoClient client = new MosparoDefaultClient(mosparoUrl, "publicKey", "privateKey");
        MosparoException thrown = assertThrows(MosparoException.class,
                () -> client.verifySubmission(Collections.emptyMap(), Collections.emptySet()));
        assertEquals("Required fields must be non null and non empty", thrown.getMessage());
    }

    @Test
    void testVerifySubmissionWithoutTokens() {
        Map<String, Object> formData = Map.of(
                "firstname", "John",
                "lastname", "Example",
                "_mosparo_validationToken", "validationToken");

        MosparoClient client = new MosparoDefaultClient(mosparoUrl, "publicKey", "privateKey");
        MosparoException thrown = assertThrows(MosparoException.class,
                () -> client.verifySubmission(formData, Set.of("firstname", "lastname")));

        assertEquals("_mosparo_submitToken must be provided.", thrown.getMessage());
    }

    @Test
    void testVerifySubmissionWithoutValidationTokens() {
        Map<String, Object> formData = Map.of(
                "firstname", "John",
                "lastname", "Example",
                "_mosparo_submitToken", "submitToken");

        MosparoClient client = new MosparoDefaultClient(mosparoUrl, "publicKey", "privateKey");
        MosparoException thrown = assertThrows(MosparoException.class,
                () -> client.verifySubmission(formData, Set.of("firstname", "lastname")));

        assertEquals("_mosparo_validationToken must be provided.", thrown.getMessage());
    }

    @Test
    void testVerifySubmissionMismatchSignature() {
        Map<String, Object> formData = Map.of(
                "lastname", "Example",
                "firstname", "John",
                "_mosparo_submitToken", "submitToken",
                "_mosparo_validationToken", "validationToken");

        stubFor(post("/api/v1/verification/verify")
                .willReturn(okJson("""
                        {
                          "valid": true,
                          "verificationSignature": "altered-signature",
                          "verifiedFields": {
                            "firstname": "valid",
                            "lastname": "valid"
                          },
                          "issues": []
                        }""")));

        MosparoClient client = new MosparoDefaultClient(mosparoUrl, "publicKey", "privateKey");
        MosparoException thrown = assertThrows(MosparoException.class,
                () -> client.verifySubmission(formData, Set.of("firstname", "lastname")));

        assertEquals("Verification signatures mismatch." +
                " Request 'ec196315e575f7f5b250f4e95d88cd45442b17ef4ff36141ed090d2814e3d8a3'" +
                " - Response 'altered-signature'", thrown.getMessage());
    }

    @Test
    void testVerifySubmissionWithoutRequiredFieldsCheck() throws MosparoException, IOException {
        Map<String, Object> formData = Map.of(
                "lastname", "Example",
                "firstname", "John",
                "_mosparo_submitToken", "submitToken",
                "_mosparo_validationToken", "validationToken");

        stubFor(post("/api/v1/verification/verify")
                .willReturn(okJson("""
                        {
                          "valid": true,
                          "verificationSignature": "ec196315e575f7f5b250f4e95d88cd45442b17ef4ff36141ed090d2814e3d8a3",
                          "verifiedFields": {
                            "firstname": "valid"
                          },
                          "issues": []
                        }""")));

        MosparoClient client = new MosparoDefaultClient(mosparoUrl, "publicKey", "privateKey");

        // No exception is raised even though the lastname was not verified
        VerificationResult result = client.verifySubmission(formData);

        assertTrue(result.isValid());
    }

    @Test
    void testVerifySubmissionMissingRequiredField() {
        Map<String, Object> formData = Map.of(
                "lastname", "Example",
                "firstname", "John",
                "_mosparo_submitToken", "submitToken",
                "_mosparo_validationToken", "validationToken");

        stubFor(post("/api/v1/verification/verify")
                .willReturn(okJson("""
                        {
                          "valid": true,
                          "verificationSignature": "ec196315e575f7f5b250f4e95d88cd45442b17ef4ff36141ed090d2814e3d8a3",
                          "verifiedFields": {
                            "firstname": "valid"
                          },
                          "issues": []
                        }""")));

        MosparoClient client = new MosparoDefaultClient(mosparoUrl, "publicKey", "privateKey");
        MosparoException thrown = assertThrows(MosparoException.class,
                () -> client.verifySubmission(formData, Set.of("firstname", "lastname")));

        assertEquals("Required field 'lastname' not verified", thrown.getMessage());
    }

    @Test
    void testVerifySubmissionFormTokensEmptyResponse() {
        Map<String, Object> formData = Map.of(
                "firstname", "John",
                "lastname", "Example",
                "_mosparo_submitToken", "submitToken",
                "_mosparo_validationToken", "validationToken");

        stubFor(post("/api/v1/verification/verify").willReturn(ok()));

        MosparoClient client = new MosparoDefaultClient(mosparoUrl, "publicKey", "privateKey");
        MosparoException thrown = assertThrows(MosparoException.class,
                () -> client.verifySubmission(formData, Set.of("firstname", "lastname")));

        assertEquals("Response from API invalid.", thrown.getMessage());
    }

    @Test
    void testVerifySubmissionConnectionError() {
        Map<String, Object> formData = Map.of(
                "firstname", "John",
                "lastname", "Example",
                "_mosparo_submitToken", "submitToken",
                "_mosparo_validationToken", "validationToken");

        stubFor(post("/api/v1/verification/verify")
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        MosparoClient client = new MosparoDefaultClient(mosparoUrl, "publicKey", "privateKey");
        IOException thrown = assertThrows(IOException.class,
                () -> client.verifySubmission(formData, Set.of("firstname", "lastname")));

        assertEquals("Connection reset", thrown.getMessage());
    }

    @Test
    void testVerifySubmissionError() {
        Map<String, Object> formData = Map.of(
                "firstname", "John",
                "lastname", "Example",
                "_mosparo_submitToken", "submitToken",
                "_mosparo_validationToken", "validationToken");

        stubFor(post("/api/v1/verification/verify")
                .willReturn(okJson("""
                        {
                          "error": true,
                          "errorMessage": "Validation failed."
                        }""")));

        MosparoClient client = new MosparoDefaultClient(mosparoUrl, "publicKey", "privateKey");
        MosparoException thrown = assertThrows(MosparoException.class,
                () -> client.verifySubmission(formData, Set.of("firstname", "lastname")));

        assertEquals("Validation failed.", thrown.getMessage());
    }

    @Test
    void testStatisticByDate() throws IOException, MosparoException {
        stubFor(get("/api/v1/statistic/by-date").willReturn(okJson("""
                {
                  "result":true,
                  "data":{
                    "numberOfValidSubmissions":5,
                    "numberOfSpamSubmissions":7,
                    "numbersByDate":{
                      "2022-12-30":{
                        "numberOfValidSubmissions":3,
                        "numberOfSpamSubmissions":4
                      },
                      "2022-12-31":{
                        "numberOfValidSubmissions":2,
                        "numberOfSpamSubmissions":3
                      }
                    }
                  }
                }
                """)));

        MosparoClient client = new MosparoDefaultClient(mosparoUrl, "publicKey", "privateKey");

        StatisticResult result = client.statisticByDate(null, null);

        assertEquals(5, result.getData().getNumberOfValidSubmissions());
        assertEquals(7, result.getData().getNumberOfSpamSubmissions());

        LocalDate date1 = LocalDate.of(2022, 12, 30);
        LocalDate date2 = LocalDate.of(2022, 12, 31);
        assertThat(result.getData().getNumbersByDate()).containsOnlyKeys(date1, date2);
        assertEquals(3, result.getData().getNumbersByDate().get(date1).getNumberOfValidSubmissions());
        assertEquals(4, result.getData().getNumbersByDate().get(date1).getNumberOfSpamSubmissions());
        assertEquals(2, result.getData().getNumbersByDate().get(date2).getNumberOfValidSubmissions());
        assertEquals(3, result.getData().getNumbersByDate().get(date2).getNumberOfSpamSubmissions());

    }

    @Test
    void testStatisticByDateWithRange() throws IOException, MosparoException {
        stubFor(get("/api/v1/statistic/by-date?range=3600").willReturn(okJson("""
                {
                  "result":true,
                  "data":{
                    "numberOfValidSubmissions":5,
                    "numberOfSpamSubmissions":7,
                    "numbersByDate":{
                      "2022-12-30":{
                        "numberOfValidSubmissions":3,
                        "numberOfSpamSubmissions":4
                      },
                      "2022-12-31":{
                        "numberOfValidSubmissions":2,
                        "numberOfSpamSubmissions":3
                      }
                    }
                  }
                }
                """)));

        MosparoClient client = new MosparoDefaultClient(mosparoUrl, "publicKey", "privateKey");

        StatisticResult result = client.statisticByDate(3600, null);

        assertTrue(result.getResult());
    }

    @Test
    void testStatisticByDateWithStartDate() throws IOException, MosparoException {
        stubFor(get("/api/v1/statistic/by-date?startDate=2024-01-01").willReturn(okJson("""
                {
                  "result":true,
                  "data":{
                    "numberOfValidSubmissions":5,
                    "numberOfSpamSubmissions":7,
                    "numbersByDate":{
                      "2022-12-30":{
                        "numberOfValidSubmissions":3,
                        "numberOfSpamSubmissions":4
                      },
                      "2022-12-31":{
                        "numberOfValidSubmissions":2,
                        "numberOfSpamSubmissions":3
                      }
                    }
                  }
                }
                """)));

        MosparoClient client = new MosparoDefaultClient(mosparoUrl, "publicKey", "privateKey");

        LocalDate startDate = LocalDate.of(2024, 1, 1);
        StatisticResult result = client.statisticByDate(null, startDate);

        assertTrue(result.getResult());
    }

    @Test
    void testStatisticByDateReturnsError() {
        stubFor(get("/api/v1/statistic/by-date").willReturn(okJson("""
                {
                  "error": true,
                  "errorMessage": "Request not valid."
                }
                """)));

        MosparoClient client = new MosparoDefaultClient(mosparoUrl, "publicKey", "privateKey");

        MosparoException thrown = assertThrows(MosparoException.class,
                () -> client.statisticByDate(null, null));
        assertEquals("Request not valid.", thrown.getMessage());
    }

    @Test
    void testHealthCheck() throws IOException, MosparoException {
        stubFor(get("/api/v1/health/check").willReturn(okJson("""
                {
                  "service": "mosparo",
                  "healthy": true,
                  "databaseStatus": "connected",
                  "error": null
                }
                """)));

        MosparoClient client = new MosparoDefaultClient(mosparoUrl, "publicKey", "privateKey");
        HealthCheckResult result = client.healthCheck();

        assertTrue(result.isHealthy());
        assertEquals("connected", result.getDatabaseStatus());
        assertEquals("mosparo", result.getService());
    }
}