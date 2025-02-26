package io.mosparo.client;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class RequestHelper {

    public static final String MOSPARO_SUBMIT_TOKEN_KEY = "_mosparo_submitToken";
    public static final String MOSPARO_VALIDATION_TOKEN_KEY = "_mosparo_validationToken";

    private final String publicKey;
    private final Mac privateMac;
    private final ObjectMapper mapper;

    /**
     * Creates a RequestHelper configured with the public key and private key of a mosparo project.
     *
     * @param publicKey the public key
     * @param privateKey the private key
     * @throws IllegalArgumentException if the given {@code privateKey} is inappropriate for
     *         initializing this {@link Mac} using {@code HmacSHA256} algorithm.
     */
    public RequestHelper(String publicKey, String privateKey) {
        try {
            this.publicKey = publicKey;

            // Prepare the private key MAC
            String macAlgorithm = "HmacSHA256";
            SecretKeySpec key = new SecretKeySpec(privateKey.getBytes(StandardCharsets.UTF_8), macAlgorithm);
            this.privateMac = Mac.getInstance(macAlgorithm);
            this.privateMac.init(key);

            // Instantiate an ObjectMapper because we need to ensure consistent JSON string for signature
            this.mapper = JsonMapper.builder()
                    .addModule(new JavaTimeModule())
                    .build();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            // Every implementation of the Java platform is required to support HmacSHA256
            // Only reason for hitting here is an illegal privateKey as argument
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Builds a {@link VerificationRequest}
     * <ul>
     * <li>Extracts the submit and validation token from the form data.</li>
     * <li>Cleans the form data
     * <li>Generates the hashes of the data</li>
     * <li>Generates the form data signature</li>
     * <li>Generates the validation signature</li>
     * <li>Generates the verification signature</li>
     * </ul>
     *
     * @param rawFormData the form data
     * @return the verification request to send to mosparo server
     * @throws MosparoException if the verification request cannot be built
     */
    public VerificationRequest buildVerificationRequest(Map<String, Object> rawFormData) throws MosparoException {
        String submitToken = extractSubmitToken(rawFormData);
        String validationToken = extractValidationToken(rawFormData);

        SortedMap<String, Object> newFormData = this.prepareAndHashFormData(rawFormData);
        String formSignature = this.generateFormDataSignature(newFormData);
        String validationSignature = this.createHmacHash(validationToken);
        String verificationSignature = this.createHmacHash(validationSignature + formSignature);

        return new VerificationRequest(submitToken, validationSignature, formSignature, newFormData, verificationSignature);
    }

    public String extractSubmitToken(Map<String, Object> rawFormData) throws MosparoException {
        return extractStringValue(rawFormData, MOSPARO_SUBMIT_TOKEN_KEY);
    }

    public String extractValidationToken(Map<String, Object> rawFormData) throws MosparoException {
        return extractStringValue(rawFormData, MOSPARO_VALIDATION_TOKEN_KEY);
    }

    private String extractStringValue(Map<String, Object> rawFormData, String key)
            throws MosparoException {
        if (rawFormData.containsKey(key)) {
            if (rawFormData.get(key) instanceof String value) {
                return value;
            } else {
                throw new MosparoException(key + " must be a String value.");
            }
        } else {
            throw new MosparoException(key + " must be provided.");
        }
    }

    /**
     * Hashes the given data using the private key and generates a hexadecimal String representation
     *
     * @param data the data to get the hash of
     * @return hexadecimal String representation of the hashed data
     */
    public String createHmacHash(String data) {
        return Hex.encodeHexString(privateMac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Prepares the form data.
     * Removes the mosparo fields, calculates hashes of values, sorts map by keys natural order
     *
     * @param rawFormData the form data
     * @return a new {@link SortedMap} containing the hashes of the form data
     */
    public SortedMap<String, Object> prepareAndHashFormData(Map<String, Object> rawFormData) {
        return rawFormData.entrySet().stream()
                .filter(entry ->
                // Discard mosparo fields
                !entry.getKey().equals(MOSPARO_SUBMIT_TOKEN_KEY) && !entry.getKey().equals(MOSPARO_VALIDATION_TOKEN_KEY))
                .collect(toSortedMap(
                        Map.Entry::getKey,
                        entry -> prepareAndHash(entry.getValue())));
    }

    /**
     * Returns a Collector that accumulates elements into a SortedMap, by keys natural order,
     * whose keys and values are the result of applying the provided mapping functions to the input elements.
     *
     * @param <T> the type of the input elements
     * @param <K> the output type of the key mapping function
     * @param <U> the output type of the value mapping function
     * @param keyMapper a mapping function to produce keys
     * @param valueMapper a mapping function to produce values
     * @return a Collector which collects elements into a SortedMap, by keys natural order,
     *         whose keys and values are the result of applying mapping functions to the input elements
     */
    private <T, K, U> Collector<T, ?, SortedMap<K, U>> toSortedMap(Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends U> valueMapper) {
        return Collectors.toMap(keyMapper, valueMapper, (key, value) -> key, TreeMap::new);
    }

    /**
     * Hashes an Object
     * <ul>
     * <li>Treats null as an empty String.</li>
     * <li>Normalize String EOL.</li>
     * <li>For {@link List} and {@link Map}, recursively prepareAndHash their values</li>
     * <li>Otherwise, will get the String value of the Object.</li>
     * </ul>
     *
     * @param value object to generate hashes for
     * @return hashed object
     */
    protected Object prepareAndHash(Object value) {
        if (value == null) {
            return DigestUtils.sha256Hex("");

        } else if (value instanceof String string) {
            String preparedValue = string.replace("\r\n", "\n");
            return DigestUtils.sha256Hex(preparedValue);

        } else if (value instanceof List<?> list) {
            return list.stream().map(this::prepareAndHash).toList();

        } else if (value instanceof Map) {
            // Assume keys are String (because it is supposed to come from JSON)
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            return map.entrySet().stream().collect(toSortedMap(
                    Map.Entry::getKey,
                    entry -> prepareAndHash(entry.getValue())));

        } else {
            return prepareAndHash(String.valueOf(value));
        }
    }

    public String generateFormDataSignature(SortedMap<String, Object> formData) throws MosparoException {
        return createHmacHash(toJson(formData));
    }

    protected String generateAuthHeaderValue(String apiEndpoint, String body) {
        String requestSignature = createHmacHash(apiEndpoint + body);
        String basicCredentials = String.format("%s:%s", this.publicKey, requestSignature);
        return Base64.encodeBase64String(basicCredentials.getBytes(StandardCharsets.UTF_8));
    }

    protected String toJson(Object object) throws MosparoException {
        try {
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new MosparoException(e);
        }
    }

    protected ObjectMapper getMapper() {
        return this.mapper;
    }
}