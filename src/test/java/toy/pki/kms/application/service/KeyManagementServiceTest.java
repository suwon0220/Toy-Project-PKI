package toy.pki.kms.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.validation.beanvalidation.MethodValidationInterceptor;
import org.springframework.web.client.ResourceAccessException;
import toy.pki.kms.adapter.http.RestKmsClient;
import toy.pki.kms.application.model.KeySearchCriteria;
import toy.pki.kms.domain.HashAlgorithm;
import toy.pki.kms.domain.KeyAlgorithm;
import toy.pki.kms.domain.KeyId;
import toy.pki.kms.domain.ManagedKey;
import toy.pki.kms.domain.SignatureAlgorithm;
import toy.pki.kms.web.KeyAlgorithmPreset;

class KeyManagementServiceTest {

    private static ValidatorFactory validators;
    private final RestKmsClient client = mock(RestKmsClient.class);
    private final KeyId keyId = new KeyId("SHA-256:key-1");
    private KeyManagementService service;

    @BeforeAll
    static void openValidator() {
        validators = Validation.buildDefaultValidatorFactory();
    }

    @AfterAll
    static void closeValidator() {
        validators.close();
    }

    @BeforeEach
    void setUp() {
        // Exercise service method validation without starting an application or HTTP server.
        ProxyFactory proxy = new ProxyFactory(new KeyManagementService(client));
        proxy.addAdvice(new MethodValidationInterceptor(validators.getValidator()));
        service = (KeyManagementService) proxy.getProxy();
    }

    @ParameterizedTest
    @EnumSource(KeyAlgorithmPreset.class)
    void generatesWithTheSelectedPresetAndNormalizedAlias(KeyAlgorithmPreset preset) {
        ManagedKey key = key(keyId.value(), preset, "root-ca");
        when(client.generate(preset, "root-ca")).thenReturn(key);
        assertThat(service.generate(preset.name(), "  root-ca  ")).isSameAs(key);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    void generatesWithoutAliasWhenItIsAbsentOrBlank(String alias) {
        ManagedKey key = key(keyId.value(), KeyAlgorithmPreset.EC_P256, null);
        when(client.generate(KeyAlgorithmPreset.EC_P256, null)).thenReturn(key);
        assertThat(service.generate("EC_P256", alias)).isSameAs(key);
    }

    @Test
    void rejectsInvalidGenerationInputsBeforeCallingKms() {
        assertThatThrownBy(() -> service.generate("EC_P256", "a".repeat(65))).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> service.generate(null, null)).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> service.generate("", null)).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> service.generate("INVALID", null)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(client);
    }

    @Test
    void searchesByAliasOrPartialKeyIdAndCombinesWithAlgorithm() {
        ManagedKey rsa = key("SHA-256:rsa-root", KeyAlgorithmPreset.RSA_2048, "Root CA");
        ManagedKey ec = key("SHA-256:ec-leaf", KeyAlgorithmPreset.EC_P256, "Root CA");
        ManagedKey unnamed = key("SHA-256:unnamed", KeyAlgorithmPreset.ED25519, null);
        when(client.findAll()).thenReturn(List.of(rsa, ec, unnamed));
        assertThat(service.search(new KeySearchCriteria(KeyAlgorithm.RSA, "  ROOT ca  "))).containsExactly(rsa);
        assertThat(service.search(new KeySearchCriteria(null, "EC-LEAF"))).containsExactly(ec);
        assertThat(service.search(new KeySearchCriteria(KeyAlgorithm.RSA, "ec-leaf"))).isEmpty();
        assertThat(service.search(new KeySearchCriteria(null, "  "))).containsExactly(rsa, ec, unnamed);
    }

    @Test
    void passesSigningAndVerificationInputsToKms() {
        byte[] data = {0, 1, (byte) 255};
        byte[] signature = {5, 6, 7};
        when(client.sign(keyId, SignatureAlgorithm.ECDSA, HashAlgorithm.SHA384, data)).thenReturn(signature);
        when(client.verify(keyId, SignatureAlgorithm.ECDSA, HashAlgorithm.SHA384, data, signature)).thenReturn(true);
        assertThat(service.sign(keyId, SignatureAlgorithm.ECDSA, HashAlgorithm.SHA384, data)).isEqualTo(signature);
        assertThat(service.verify(keyId, SignatureAlgorithm.ECDSA, HashAlgorithm.SHA384, data, signature)).isTrue();
        assertThat(service.verify(keyId, SignatureAlgorithm.ECDSA, HashAlgorithm.SHA384, data, new byte[] {8})).isFalse();
    }

    @Test
    void allowsEdDsaWithoutDigestButRejectsMissingRequiredInputs() {
        byte[] data = {1};
        when(client.sign(keyId, SignatureAlgorithm.Ed25519, null, data)).thenReturn(new byte[] {2});
        assertThat(service.sign(keyId, SignatureAlgorithm.Ed25519, null, data)).containsExactly(2);
        assertThatThrownBy(() -> service.sign(null, SignatureAlgorithm.Ed25519, null, data))
            .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> service.sign(keyId, null, null, data)).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> service.sign(keyId, SignatureAlgorithm.Ed25519, null, null))
            .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> service.verify(keyId, SignatureAlgorithm.Ed25519, null, data, null))
            .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void returnsPublicKeyAndDeletionResultFromKms() {
        when(client.getPublicKeyOf(keyId)).thenReturn(new byte[] {0, 1, (byte) 255});
        when(client.delete(keyId)).thenReturn(keyId);
        assertThat(service.getPublicKeyOf(keyId)).containsExactly(0, 1, (byte) 255);
        assertThat(service.delete(keyId)).isEqualTo(keyId);
    }

    @Test
    void propagatesKmsFailureToTheCaller() {
        var failure = new ResourceAccessException("KMS offline");
        when(client.findAll()).thenThrow(failure);
        assertThatThrownBy(service::findAll).isSameAs(failure);
    }

    private ManagedKey key(String id, KeyAlgorithmPreset preset, String alias) {
        ManagedKey key = new ManagedKey(new KeyId(id), preset, Instant.now());
        key.setAlias(alias);
        return key;
    }
}
