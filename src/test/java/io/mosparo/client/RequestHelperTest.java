package io.mosparo.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

class RequestHelperTest {

    String publicKey = "publicKey";
    String privateKey = "privateKey";

    ObjectMapper mapper = JsonMapper.builder().build();

    @Test
    void testCreateHmacHash() {
        RequestHelper helper = new RequestHelper(publicKey, privateKey);
        String data = "testData";
        assertEquals("0646b5f2e09db205a8b3eb0e7429645561a1b9fdff1fcdb1fed9cd585108d850", helper.createHmacHash(data));
    }

    @Test
    void testPrepareAndHash() {
        RequestHelper helper = new RequestHelper(publicKey, privateKey);
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", helper.prepareAndHash(null));

        assertEquals("153590093b8c278bb7e1fef026d8a59b9ba02701d1e0a66beac0938476f2a812", helper.prepareAndHash("Test Tester"));
        assertEquals("a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3", helper.prepareAndHash(123));
        assertEquals("973dfe463ec85785f5f95af5ba3906eedb2d931c24e69824a89ea65dba4e813b",
                helper.prepareAndHash("test@example.com"));
        assertEquals("8cc62c145cd0c6dc444168eaeb1b61b351f9b1809a579cc9b4c9e9d7213a39ee",
                helper.prepareAndHash("test2@example.com"));

        assertEquals("b5bea41b6c623f7c09f1bf24dcae58ebab3c0cdd90ad966bc43a45b44867e12b", helper.prepareAndHash(true));
        assertEquals("fcbcf165908dd18a9e49f7ff27810176db8e9f63b4352213741664245224f8aa", helper.prepareAndHash(false));

        assertEquals("cc0bdb0377d3ba87046028784e8a4319972a7c9df31c645e80e14e8dd8735b6b", helper.prepareAndHash("Teststreet"));
        assertEquals("f0026e9b4550ae1b057893f70438648e9233301a0ba1a91d5057d225527b0de1",
                helper.prepareAndHash("Teststreet\nTest\nStreet"));
        assertEquals("f0026e9b4550ae1b057893f70438648e9233301a0ba1a91d5057d225527b0de1",
                helper.prepareAndHash("Teststreet\r\nTest\r\nStreet"));
    }

    @Test
    void testPrepareAndHashList() {
        RequestHelper helper = new RequestHelper(publicKey, privateKey);

        List<String> list = List.of(
                "test@example.com",
                "test2@example.com");

        List<String> expected = List.of(
                "973dfe463ec85785f5f95af5ba3906eedb2d931c24e69824a89ea65dba4e813b",
                "8cc62c145cd0c6dc444168eaeb1b61b351f9b1809a579cc9b4c9e9d7213a39ee");

        assertThat(helper.prepareAndHash(list)).isEqualTo(expected);
    }

    @Test
    void testPrepareAndHashMap() {
        RequestHelper helper = new RequestHelper(publicKey, privateKey);

        Map<String, Object> formData = Map.of(
                "street", "Teststreet",
                "number", 123);

        assertThat(helper.prepareAndHashFormData(formData)).containsExactly(
                entry("number", "a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3"),
                entry("street", "cc0bdb0377d3ba87046028784e8a4319972a7c9df31c645e80e14e8dd8735b6b"));
    }

    @Test
    void testPrepareAndHashFormData() throws JsonProcessingException {
        RequestHelper helper = new RequestHelper(publicKey, privateKey);

        Map<String, Object> formData = mapper.readValue("""
                {
                    "_mosparo_submitToken":"submitToken",
                    "_mosparo_validationToken":"validationToken",
                    "name":"Test Tester",
                    "address":{
                        "street":"Teststreet\\r\\nTest\\r\\nStreet",
                        "number":123
                    },
                    "valid":false,
                    "email": [
                        "test@example.com",
                        "test2@example.com"
                    ],
                    "website": null,
                    "data":{
                    }
                }
                """, new TypeReference<>() {
        });

        Map<String, Object> expected = mapper.readValue("""
                {
                    "address":{
                        "number":"a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3",
                        "street":"f0026e9b4550ae1b057893f70438648e9233301a0ba1a91d5057d225527b0de1"
                    },
                    "name":"153590093b8c278bb7e1fef026d8a59b9ba02701d1e0a66beac0938476f2a812",
                    "email": [
                        "973dfe463ec85785f5f95af5ba3906eedb2d931c24e69824a89ea65dba4e813b",
                        "8cc62c145cd0c6dc444168eaeb1b61b351f9b1809a579cc9b4c9e9d7213a39ee"
                    ],
                    "data": {
                    },
                    "valid": "fcbcf165908dd18a9e49f7ff27810176db8e9f63b4352213741664245224f8aa",
                    "website": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
                }
                """, new TypeReference<>() {
        });

        SortedMap<String, Object> actual = helper.prepareAndHashFormData(formData);

        assertThat(actual).usingRecursiveAssertion().isEqualTo(expected);
    }

    @Test
    void testCreateFormDataHmacHash() throws MosparoException, JsonProcessingException {
        RequestHelper helper = new RequestHelper("publicKey", "privateKey");

        SortedMap<String, Object> formData = mapper.readValue("""
                {
                    "address":{
                        "number":123,
                        "street":"Teststreet"
                    },
                    "data":{
                    },
                    "name": "Test Tester",
                    "valid": false
                }
                """, new TypeReference<>() {
        });

        assertEquals("08288d6a1a3e72cf6b2981e15a3a0be52d9606c590165fb95247c25e5570e874",
                helper.generateFormDataSignature(formData));
    }
}