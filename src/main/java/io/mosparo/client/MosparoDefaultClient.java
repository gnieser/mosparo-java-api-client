package io.mosparo.client;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

public class MosparoDefaultClient implements MosparoClient {

    public static final String VERIFICATION_API_ENDPOINT = "/api/v1/verification/verify";
    public static final String STATISTIC_BY_DATE_API_ENDPOINT = "/api/v1/statistic/by-date";
    public static final String HEALTH_CHECK_API_ENDPOINT = "/api/v1/health/check";

    private final String url;
    private final HttpClient httpClient;
    private final RequestHelper helper;

    /**
     * Creates a mosparo client, using a default {@link HttpClient}
     *
     * @param url Url of the mosparo server
     * @param publicKey the public key of the mosparo project
     * @param privateKey the private key of the mosparo project
     */
    public MosparoDefaultClient(String url, String publicKey, String privateKey) {
        this(url, publicKey, privateKey, HttpClientBuilder.create().build());
    }

    /**
     * Creates a mosparo client, using the provided {@link HttpClient}
     *
     * @param url Url of the mosparo server
     * @param publicKey the public key of the mosparo project
     * @param privateKey the private key of the mosparo project
     * @param httpClient HttpClient the client will use
     */
    public MosparoDefaultClient(String url, String publicKey, String privateKey, HttpClient httpClient) {
        this.httpClient = httpClient;
        this.url = url;
        this.helper = new RequestHelper(publicKey, privateKey);
    }

    @Override
    public VerificationResult verifySubmission(Map<String, Object> formData, Set<String> requiredFields)
            throws IOException, MosparoException {
        if (requiredFields == null || requiredFields.isEmpty()) {
            throw new MosparoException("Required fields must be non null and non empty");
        }

        VerificationRequest request = helper.buildVerificationRequest(formData);
        HttpPost httpRequest = buildVerificationRequestHttpRequest(request);

        VerificationResult result = execute(httpRequest, VerificationResult.class);

        if (result.hasError()) {
            throw new MosparoException(result.getErrorMessage());
        }
        checkSignature(request, result);
        checkRequiredFields(result, requiredFields);

        return result;
    }

    @Override
    public StatisticResult statisticByDate(Integer range, LocalDate startDate) throws IOException, MosparoException {
        HttpGet httpRequest = buildStatisticByDateHttpRequest(range, startDate);

        StatisticResult result = execute(httpRequest, StatisticResult.class);

        if (result.hasError()) {
            throw new MosparoException(result.getErrorMessage());
        }
        return result;
    }

    @Override
    public HealthCheckResult healthCheck() throws IOException, MosparoException {
        HttpGet httpRequest = new HttpGet(url + HEALTH_CHECK_API_ENDPOINT);
        return execute(httpRequest, HealthCheckResult.class);
    }

    protected <T> T execute(HttpUriRequest httpRequest, Class<T> resultType) throws IOException, MosparoException {
        HttpResponse httpResponse = execute(httpRequest);
        try {
            return helper.getMapper().readValue(httpResponse.getEntity().getContent(), resultType);
        } catch (IOException e) {
            throw new MosparoException("Response from API invalid.", e);
        }
    }

    protected HttpResponse execute(HttpUriRequest httpRequest) throws IOException, MosparoException {
        HttpResponse httpResponse = httpClient.execute(httpRequest);
        if (httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            EntityUtils.consumeQuietly(httpResponse.getEntity());
            throw new MosparoException(httpResponse.getStatusLine().getReasonPhrase());
        }
        return httpResponse;
    }

    protected HttpPost buildVerificationRequestHttpRequest(VerificationRequest request) throws MosparoException {
        try {
            String body = helper.toJson(request);

            HttpPost httpRequest = new HttpPost(url + MosparoDefaultClient.VERIFICATION_API_ENDPOINT);
            httpRequest.setEntity(new StringEntity(body));

            setHeaders(httpRequest, MosparoDefaultClient.VERIFICATION_API_ENDPOINT, body);

            return httpRequest;
        } catch (UnsupportedEncodingException e) {
            throw new MosparoException(e);
        }
    }

    protected HttpGet buildStatisticByDateHttpRequest(Integer range, LocalDate startDate) throws MosparoException {
        try {
            URIBuilder uriBuilder = new URIBuilder(url + MosparoDefaultClient.STATISTIC_BY_DATE_API_ENDPOINT);
            if (range != null) {
                uriBuilder.addParameter("range", range > 0 ? String.valueOf(range) : "0");
            }
            if (startDate != null) {
                uriBuilder.addParameter("startDate", startDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
            }

            HttpGet httpRequest = new HttpGet(uriBuilder.build());
            // Generate the request signature authorization header (using a fake empty body for a GET request)
            setHeaders(httpRequest, MosparoDefaultClient.STATISTIC_BY_DATE_API_ENDPOINT, "{}");

            return httpRequest;
        } catch (URISyntaxException e) {
            throw new MosparoException(e);
        }
    }

    protected void setHeaders(HttpUriRequest httpRequest, String apiEndpoint, String body) {
        httpRequest.setHeader(HttpHeaders.ACCEPT, "application/json");
        httpRequest.setHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8");
        String auth = helper.generateAuthHeaderValue(apiEndpoint, body);
        httpRequest.setHeader(HttpHeaders.AUTHORIZATION, auth);
    }

    protected void checkSignature(VerificationRequest request, VerificationResult result) throws MosparoException {
        // Mosparo result has signature only when result is valid
        if (result.isValid() && !request.getVerificationSignature().equals(result.getVerificationSignature())) {
            // This may indicate that an attacker is attempting to falsify actions

            String message = String.format("Verification signatures mismatch. Request '%s' - Response '%s'",
                    request.getVerificationSignature(), result.getVerificationSignature());
            throw new MosparoException(message);
        }
    }

    protected void checkRequiredFields(VerificationResult result, Set<String> requiredFields) throws MosparoException {
        // The user could change a required field in the browser to an ignored field for mosparo and bypass mosparo with it
        // After successful verification, you should ensure all your required fields are verified
        for (String requiredField : requiredFields) {
            if (!result.getVerifiedFields().containsKey(requiredField)) {
                throw new MosparoException(String.format("Required field '%s' not found", requiredField));
            }
        }
    }
}
