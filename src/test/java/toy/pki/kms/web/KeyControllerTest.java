package toy.pki.kms.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.HtmlUtils;
import toy.pki.kms.application.port.KeyRepository;
import toy.pki.kms.application.registry.KeyMaterialProviderRegistry;
import toy.pki.kms.application.service.KeyManagementService;
import toy.pki.kms.domain.key.HashAlgorithm;
import toy.pki.kms.domain.key.KeyId;
import toy.pki.kms.domain.key.KeyMaterialRef;
import toy.pki.kms.domain.key.KeyProviderId;
import toy.pki.kms.domain.key.ManagedKey;
import toy.pki.kms.domain.key.SignatureAlgorithm;

@SpringBootTest
@AutoConfigureMockMvc
class KeyControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private KeyRepository keyRepository;
    @Autowired private KeyManagementService keyManagementService;
    @Autowired private KeyMaterialProviderRegistry providerRegistry;

    static Stream<Arguments> signHashCases() {
        return Stream.of(
            Arguments.of(KeyAlgorithmPreset.RSA_2048, SignatureAlgorithm.RSA_PKCS1_V1_5,
                HashAlgorithm.SHA384, "SHA-384", "SHA384withRSA"),
            Arguments.of(KeyAlgorithmPreset.EC_P521, SignatureAlgorithm.ECDSA,
                HashAlgorithm.SHA512, "SHA-512", "SHA512withECDSA"),
            Arguments.of(KeyAlgorithmPreset.ED25519, SignatureAlgorithm.Ed25519,
                null, "SHA-256", "Ed25519"),
            Arguments.of(KeyAlgorithmPreset.ED448, SignatureAlgorithm.Ed448,
                null, "SHA-256", "Ed448"));
    }

    static Stream<Arguments> verifyPrefillCases() {
        return Stream.of(
            Arguments.of(KeyAlgorithmPreset.RSA_2048, SignatureAlgorithm.RSA_PSS, HashAlgorithm.SHA384, "TEXT"),
            Arguments.of(KeyAlgorithmPreset.EC_P256, SignatureAlgorithm.ECDSA, HashAlgorithm.SHA384, "HEX"),
            Arguments.of(KeyAlgorithmPreset.ED25519, SignatureAlgorithm.Ed25519, null, "BASE64"),
            Arguments.of(KeyAlgorithmPreset.ED448, SignatureAlgorithm.Ed448, null, "TEXT"));
    }

    private static String htmlAttribute(String tag, String name) {
        var matcher = Pattern.compile("\\b" + name + "=\"([^\"]*)\"").matcher(tag);
        assertThat(matcher.find()).as("%s attribute in %s", name, tag).isTrue();
        return HtmlUtils.htmlUnescape(matcher.group(1));
    }

    private static void assertVerificationInputs(
        String html, String data, String signature,
        SignatureAlgorithm algorithm, HashAlgorithm hash, String encoding) {
        assertTextarea(html, "data", data);
        assertTextarea(html, "signature", signature);
        assertSelectedOption(html, "signatureAlgorithm", algorithm.name());
        assertSelectedOption(html, "encoding", encoding);
        if (hash != null) {
            assertSelectedOption(html, "hashAlgorithm", hash.name());
        }
    }

    private static void assertTextarea(String html, String id, String expected) {
        var matcher = Pattern.compile("<textarea\\b[^>]*id=\"" + id + "\"[^>]*>([\\s\\S]*?)</textarea>")
                             .matcher(html);
        assertThat(matcher.find()).isTrue();
        assertThat(HtmlUtils.htmlUnescape(matcher.group(1))).isEqualTo(expected);
    }

    private static void assertSelectedOption(String html, String id, String expected) {
        var select = Pattern.compile("<select\\b[^>]*id=\"" + id + "\"[^>]*>([\\s\\S]*?)</select>")
                            .matcher(html);
        assertThat(select.find()).isTrue();
        var selected = Pattern.compile("<option\\b[^>]*selected=\"selected\"[^>]*>").matcher(select.group(1));
        assertThat(selected.find()).isTrue();
        assertThat(htmlAttribute(selected.group(), "value")).isEqualTo(expected);
    }

    static Stream<Arguments> popupCases() {
        return Stream.of(KeyAlgorithmPreset.values()).flatMap(preset ->
            Stream.of("sign", "verify", "info", "delete")
                  .map(operation -> Arguments.of(preset, operation)));
    }

    @ParameterizedTest
    @MethodSource("signHashCases")
    void signsAndRendersMatchingDataHash(
        KeyAlgorithmPreset preset, SignatureAlgorithm algorithm, HashAlgorithm hash,
        String displayHash, String jcaAlgorithm) throws Exception {
        ManagedKey key = keyManagementService.generate("in-memory", preset.toParameters());
        var provider = providerRegistry.get(key.getKeyProviderId());
        try {
            var request = post("/pki/kms/keys/{keyId}/sign", key.getKeyId().value())
                .param("signatureAlgorithm", algorithm.name())
                .param("encoding", "HEX")
                .param("data", "68656c6c6f");
            if (hash != null) {
                request.param("hashAlgorithm", hash.name());
            }
            MvcResult result = mockMvc.perform(request)
                                      .andExpect(status().is3xxRedirection())
                                      .andReturn();

            Map<?, ?> signResult = (Map<?, ?>) result.getFlashMap().get("signResult");
            byte[] original = "hello".getBytes(StandardCharsets.UTF_8);
            String expectedHash = HexFormat.of().formatHex(
                MessageDigest.getInstance(displayHash).digest(original));
            assertThat(signResult.get("dataHashAlgorithm")).isEqualTo(displayHash);
            assertThat(signResult.get("dataHashHex")).isEqualTo(expectedHash);

            Signature verifier = Signature.getInstance(jcaAlgorithm);
            verifier.initVerify(provider.getPublicKey(key.getKeyMaterialRef()).orElseThrow());
            verifier.update(original);
            assertThat(verifier.verify(Base64.getDecoder().decode(
                (String) signResult.get("signatureBase64")))).isTrue();

            mockMvc.perform(get(URI.create(result.getResponse().getRedirectedUrl()))
                       .flashAttrs(result.getFlashMap()))
                   .andExpect(status().isOk())
                   .andExpect(content().string(containsString("데이터 " + displayHash)))
                   .andExpect(content().string(containsString(expectedHash)));
        } finally {
            keyRepository.delete(key.getKeyId());
            provider.delete(key.getKeyMaterialRef());
        }
    }

    @ParameterizedTest
    @MethodSource("verifyPrefillCases")
    void transfersSignedInputsToVerificationAndRetainsThemAfterVerify(
        KeyAlgorithmPreset preset, SignatureAlgorithm algorithm, HashAlgorithm hash,
        String encoding) throws Exception {
        ManagedKey key = keyManagementService.generate("in-memory", preset.toParameters());
        var provider = providerRegistry.get(key.getKeyProviderId());
        try {
            String original = "원본 데이터\n\"<&> '</textarea>";
            byte[] bytes = original.getBytes(StandardCharsets.UTF_8);
            String data = switch (encoding) {
                case "HEX" -> HexFormat.of().formatHex(bytes);
                case "BASE64" -> Base64.getEncoder().encodeToString(bytes);
                default -> original;
            };
            var signRequest = post("/pki/kms/keys/{keyId}/sign", key.getKeyId().value())
                .param("signatureAlgorithm", algorithm.name())
                .param("encoding", encoding)
                .param("data", data);
            if (hash != null) {
                signRequest.param("hashAlgorithm", hash.name());
            }
            MvcResult signed = mockMvc.perform(signRequest)
                                      .andExpect(status().is3xxRedirection()).andReturn();
            String signHtml = mockMvc.perform(get(URI.create(signed.getResponse().getRedirectedUrl()))
                                         .flashAttrs(signed.getFlashMap()))
                                     .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            String signature = (String) ((Map<?, ?>) signed.getFlashMap().get("signResult"))
                .get("signatureBase64");

            var formMatcher = Pattern.compile("<form\\b[^>]*id=\"verify-prefill\"[^>]*>([\\s\\S]*?)</form>")
                                     .matcher(signHtml);
            assertThat(formMatcher.find()).isTrue();
            Map<String, String> inputs = new LinkedHashMap<>();
            var inputMatcher = Pattern.compile("<input\\b[^>]*>").matcher(formMatcher.group(1));
            while (inputMatcher.find()) {
                inputs.put(htmlAttribute(inputMatcher.group(), "name"),
                    htmlAttribute(inputMatcher.group(), "value"));
            }
            assertThat(signHtml).contains("form=\"verify-prefill\"");
            assertThat(inputs).containsEntry("data", data)
                              .containsEntry("signature", signature)
                              .containsEntry("encoding", encoding)
                              .containsEntry("signatureAlgorithm", algorithm.name());
            if (hash == null) {
                assertThat(inputs).doesNotContainKey("hashAlgorithm");
            } else {
                assertThat(inputs).containsEntry("hashAlgorithm", hash.name());
            }

            var prepareRequest = post("/pki/kms/keys/{keyId}/verify/prepare", key.getKeyId().value());
            inputs.forEach(prepareRequest::param);
            MvcResult prepared = mockMvc.perform(prepareRequest)
                                        .andExpect(status().is3xxRedirection()).andReturn();
            assertThat(prepared.getFlashMap().containsKey("verifyResult")).isFalse();
            String verifyHtml = mockMvc.perform(get(URI.create(prepared.getResponse().getRedirectedUrl()))
                                           .flashAttrs(prepared.getFlashMap()))
                                       .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertVerificationInputs(verifyHtml, data, signature, algorithm, hash, encoding);

            var verifyRequest = post("/pki/kms/keys/{keyId}/verify", key.getKeyId().value());
            inputs.forEach(verifyRequest::param);
            MvcResult verified = mockMvc.perform(verifyRequest)
                                        .andExpect(status().is3xxRedirection()).andReturn();
            assertThat(((Map<?, ?>) verified.getFlashMap().get("verifyResult")).get("valid")).isEqualTo(true);
            String resultHtml = mockMvc.perform(get(URI.create(verified.getResponse().getRedirectedUrl()))
                                           .flashAttrs(verified.getFlashMap()))
                                       .andExpect(status().isOk())
                                       .andExpect(content().string(containsString("유효한 서명입니다")))
                                       .andReturn().getResponse().getContentAsString();
            assertVerificationInputs(resultHtml, data, signature, algorithm, hash, encoding);
        } finally {
            keyRepository.delete(key.getKeyId());
            provider.delete(key.getKeyMaterialRef());
        }
    }

    @Test
    void rendersListAndGenerationForm() throws Exception {
        mockMvc.perform(get("/pki/kms"))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("name=\"providerId\"")))
               .andExpect(content().string(containsString("RSA_2048")));
    }

    @ParameterizedTest
    @MethodSource("popupCases")
    void rendersKeyPopup(KeyAlgorithmPreset preset, String operation) throws Exception {
        KeyId keyId = new KeyId("SHA-256:" + UUID.randomUUID());
        keyRepository.save(new ManagedKey(
            keyId, preset.toParameters(), new KeyMaterialRef("test-material"),
            new KeyProviderId("in-memory"), Instant.now()));
        try {
            mockMvc.perform(get("/pki/kms")
                       .param("opKeyId", keyId.value())
                       .param("op", operation))
                   .andExpect(status().isOk())
                   .andExpect(content().string(containsString("role=\"dialog\"")))
                   .andExpect(content().string(containsString(keyId.value())))
                   .andExpect(content().string(containsString("data-k-algorithm=\"" + preset.name() + "\"")))
                   .andExpect(content().string(containsString("· " + preset.name())));
        } finally {
            keyRepository.delete(keyId);
        }
    }

    @Test
    void rendersEmptySearchResults() throws Exception {
        mockMvc.perform(get("/pki/kms").param("keyId", "missing-key"))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("조건에 맞는 키가 없습니다")));
    }

    @Test
    void matchesPartialKeyIdAndCombinesWithAlgorithm() throws Exception {
        String fragment = UUID.randomUUID().toString();
        Instant createdAt = Instant.parse("2026-10-05T10:08:42.802Z");
        ManagedKey rsa = new ManagedKey(
            new KeyId("SHA-256:prefix-" + fragment + "-first"), KeyAlgorithmPreset.RSA_2048.toParameters(),
            new KeyMaterialRef("test-rsa"), new KeyProviderId("in-memory"), createdAt);
        ManagedKey ec = new ManagedKey(
            new KeyId("SHA-256:other-" + fragment + "-second"), KeyAlgorithmPreset.EC_P256.toParameters(),
            new KeyMaterialRef("test-ec"), new KeyProviderId("in-memory"), createdAt);
        ManagedKey unrelated = new ManagedKey(
            new KeyId("SHA-256:" + UUID.randomUUID()), KeyAlgorithmPreset.RSA_2048.toParameters(),
            new KeyMaterialRef("test-other"), new KeyProviderId("in-memory"), createdAt);
        List<ManagedKey> fixtures = List.of(rsa, ec, unrelated);
        fixtures.forEach(keyRepository::save);
        try {
            mockMvc.perform(get("/pki/kms").param("keyword", fragment))
                   .andExpect(status().isOk())
                   .andExpect(model().attribute("keys", containsInAnyOrder(rsa, ec)))
                   .andExpect(content().string(containsString("data-sort-key=\"keyId\"")))
                   .andExpect(content().string(containsString("2026-10-05 19:08:42.802")));

            mockMvc.perform(get("/pki/kms").param("keyId", "  " + fragment.toUpperCase(Locale.ROOT) + "  "))
                   .andExpect(status().isOk())
                   .andExpect(model().attribute("keys", containsInAnyOrder(rsa, ec)));

            mockMvc.perform(get("/pki/kms").param("keyId", fragment).param("keyAlgorithm", "RSA"))
                   .andExpect(status().isOk())
                   .andExpect(model().attribute("keys", contains(rsa)));

            mockMvc.perform(get("/pki/kms").param("keyId", rsa.getKeyId().value()))
                   .andExpect(status().isOk())
                   .andExpect(model().attribute("keys", contains(rsa)));

            mockMvc.perform(get("/pki/kms").param("keyId", "   "))
                   .andExpect(status().isOk())
                   .andExpect(model().attribute("keys", hasItems(rsa, ec, unrelated)));

            mockMvc.perform(get("/pki/kms").param("opKeyId", rsa.getKeyId().value()).param("op", "info"))
                   .andExpect(status().isOk())
                   .andExpect(content().string(containsString("2026-10-05 19:08:42.802")));
        } finally {
            fixtures.forEach(key -> keyRepository.delete(key.getKeyId()));
        }
    }

    @Test
    void searchesAliasOrKeyIdWithOneKeyword() throws Exception {
        String keyword = "find-" + UUID.randomUUID();
        ManagedKey aliasMatch = new ManagedKey(
            new KeyId("SHA-256:" + UUID.randomUUID()), KeyAlgorithmPreset.RSA_2048.toParameters(),
            new KeyMaterialRef("test-alias"), new KeyProviderId("in-memory"), Instant.now());
        aliasMatch.setAlias("ca-" + keyword + "-signer");
        ManagedKey idMatch = new ManagedKey(
            new KeyId("SHA-256:" + keyword), KeyAlgorithmPreset.EC_P256.toParameters(),
            new KeyMaterialRef("test-id"), new KeyProviderId("in-memory"), Instant.now());
        ManagedKey unrelated = new ManagedKey(
            new KeyId("SHA-256:" + UUID.randomUUID()), KeyAlgorithmPreset.RSA_2048.toParameters(),
            new KeyMaterialRef("test-other"), new KeyProviderId("in-memory"), Instant.now());
        unrelated.setAlias("unrelated-ca");
        List<ManagedKey> fixtures = List.of(aliasMatch, idMatch, unrelated);
        fixtures.forEach(keyRepository::save);
        try {
            mockMvc.perform(get("/pki/kms").param("keyword", keyword))
                   .andExpect(status().isOk())
                   .andExpect(model().attribute("keys", containsInAnyOrder(aliasMatch, idMatch)))
                   .andExpect(content().string(containsString("name=\"keyword\"")));

            mockMvc.perform(get("/pki/kms")
                       .param("keyword", "  " + keyword.toUpperCase(Locale.ROOT) + "  ")
                       .param("keyAlgorithm", "RSA"))
                   .andExpect(status().isOk())
                   .andExpect(model().attribute("keys", contains(aliasMatch)));
        } finally {
            fixtures.forEach(key -> keyRepository.delete(key.getKeyId()));
        }
    }

    @Test
    void rendersGenerationValidationErrors() throws Exception {
        mockMvc.perform(post("/pki/kms").param("keyAlgorithmPreset", "RSA_2048"))
               .andExpect(status().isOk())
               .andExpect(model().attributeHasFieldErrors("keyGenerationRequest", "providerId"))
               .andExpect(content().string(containsString("키 Provider를 선택하세요.")));
    }

    @Test
    void createsKeyAndRendersRedirectedList() throws Exception {
        String alias = "tls-issuing-ca-" + UUID.randomUUID();
        mockMvc.perform(post("/pki/kms")
                   .param("providerId", "in-memory")
                   .param("keyAlgorithmPreset", "RSA_2048")
                   .param("alias", "  " + alias + "  "))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/pki/kms"));

        ManagedKey generated = keyRepository.findAll().stream()
                                            .filter(key -> alias.equals(key.getAlias()))
                                            .findFirst().orElseThrow();
        assertThat(generated.getAlias()).isEqualTo(alias);
        assertThat(generated.getKeyGenerationParameters()).isEqualTo(KeyAlgorithmPreset.RSA_2048.toParameters());

        mockMvc.perform(get("/pki/kms").param("keyId", generated.getKeyId().value()))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString(alias)))
               .andExpect(content().string(containsString("data-k-algorithm=\"RSA_2048\"")));

        mockMvc.perform(get("/pki/kms")
                   .param("opKeyId", generated.getKeyId().value()).param("op", "info"))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString(alias)));
    }

    @Test
    void rejectsTooLongAliasBeforeCreatingKey() throws Exception {
        int keyCount = keyRepository.findAll().size();
        String alias = "a".repeat(65);
        mockMvc.perform(post("/pki/kms")
                   .param("providerId", "in-memory")
                   .param("keyAlgorithmPreset", "RSA_2048")
                   .param("alias", alias))
               .andExpect(status().isOk())
               .andExpect(model().attributeHasFieldErrors("keyGenerationRequest", "alias"))
               .andExpect(content().string(containsString("별칭은 최대 64자까지 입력할 수 있습니다.")))
               .andExpect(content().string(containsString("value=\"" + alias + "\"")));

        assertThat(keyRepository.findAll()).hasSize(keyCount);
    }

    @Test
    void escapesAliasInListAndPopup() throws Exception {
        KeyId keyId = new KeyId("SHA-256:" + UUID.randomUUID());
        ManagedKey key = new ManagedKey(
            keyId, KeyAlgorithmPreset.RSA_2048.toParameters(), new KeyMaterialRef("test-material"),
            new KeyProviderId("in-memory"), Instant.now());
        key.setAlias("<script>alert('alias')</script>");
        keyRepository.save(key);
        try {
            mockMvc.perform(get("/pki/kms")
                       .param("opKeyId", keyId.value()).param("op", "info"))
                   .andExpect(status().isOk())
                   .andExpect(content().string(containsString("&lt;script&gt;")))
                   .andExpect(content().string(not(containsString(key.getAlias()))));
        } finally {
            keyRepository.delete(keyId);
        }
    }
}
