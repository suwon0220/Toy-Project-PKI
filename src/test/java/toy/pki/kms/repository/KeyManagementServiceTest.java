package toy.pki.kms.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import toy.pki.kms.application.registry.KeyMaterialProviderRegistry;
import toy.pki.kms.application.service.KeyManagementService;
import toy.pki.kms.domain.key.HashAlgorithm;
import toy.pki.kms.domain.key.ManagedKey;
import toy.pki.kms.domain.key.generation.EcKeyGenerationParameters;
import toy.pki.kms.domain.key.generation.Ed25519KeyGenerationParameters;
import toy.pki.kms.domain.key.generation.Ed448KeyGenerationParameters;
import toy.pki.kms.domain.key.generation.KeyGenerationParameters;
import toy.pki.kms.domain.key.generation.RsaKeyGenerationParameters;
import toy.pki.kms.domain.key.signature.EcdsaSignatureParameters;
import toy.pki.kms.domain.key.signature.Ed25519SignatureParameters;
import toy.pki.kms.domain.key.signature.Ed448SignatureParameters;
import toy.pki.kms.domain.key.signature.RsaPkcs1SignatureParameters;
import toy.pki.kms.domain.key.signature.RsaPssSignatureParameters;
import toy.pki.kms.domain.key.signature.SignatureParameters;

@SpringBootTest
class KeyManagementServiceTest {

    private final String providerId = "in-memory";

    @Autowired private KeyManagementService keyManagementService;

    @Autowired private KeyMaterialProviderRegistry keyMaterialProviderRegistry;

    @Autowired private Provider cryptographicProvider;

    static Stream<KeyGenerationParameters> keyGenerationParameters() {
        return Stream.of(
            new RsaKeyGenerationParameters(2048),
            new RsaKeyGenerationParameters(3072),
            new RsaKeyGenerationParameters(4096),
            new EcKeyGenerationParameters("secp256r1"),
            new EcKeyGenerationParameters("secp384r1"),
            new EcKeyGenerationParameters("secp521r1"),
            new Ed25519KeyGenerationParameters(),
            new Ed448KeyGenerationParameters());
    }

    static Stream<Arguments> signatureCases() {
        return Stream.of(
            Arguments.of(
                new RsaKeyGenerationParameters(2048),
                new RsaPkcs1SignatureParameters(HashAlgorithm.SHA256),
                "SHA256withRSA",
                null),
            Arguments.of(
                new RsaKeyGenerationParameters(2048),
                new RsaPkcs1SignatureParameters(HashAlgorithm.SHA384),
                "SHA384withRSA",
                null),
            Arguments.of(
                new RsaKeyGenerationParameters(2048),
                new RsaPkcs1SignatureParameters(HashAlgorithm.SHA512),
                "SHA512withRSA",
                null),
            Arguments.of(
                new RsaKeyGenerationParameters(2048),
                new RsaPssSignatureParameters(
                    HashAlgorithm.SHA256,
                    HashAlgorithm.SHA256,
                    32),
                "RSASSA-PSS",
                new PSSParameterSpec(
                    "SHA-256",
                    "MGF1",
                    MGF1ParameterSpec.SHA256,
                    32,
                    PSSParameterSpec.TRAILER_FIELD_BC)),
            Arguments.of(
                new RsaKeyGenerationParameters(2048),
                new RsaPssSignatureParameters(
                    HashAlgorithm.SHA384,
                    HashAlgorithm.SHA384,
                    32),
                "RSASSA-PSS",
                new PSSParameterSpec(
                    "SHA-384",
                    "MGF1",
                    MGF1ParameterSpec.SHA384,
                    32,
                    PSSParameterSpec.TRAILER_FIELD_BC)),
            Arguments.of(
                new RsaKeyGenerationParameters(2048),
                new RsaPssSignatureParameters(
                    HashAlgorithm.SHA512,
                    HashAlgorithm.SHA512,
                    32),
                "RSASSA-PSS",
                new PSSParameterSpec(
                    "SHA-512",
                    "MGF1",
                    MGF1ParameterSpec.SHA512,
                    32,
                    PSSParameterSpec.TRAILER_FIELD_BC)),
            Arguments.of(
                new EcKeyGenerationParameters("secp256r1"),
                new EcdsaSignatureParameters(HashAlgorithm.SHA256),
                "SHA256withECDSA",
                null),
            Arguments.of(
                new EcKeyGenerationParameters("secp256r1"),
                new EcdsaSignatureParameters(HashAlgorithm.SHA384),
                "SHA384withECDSA",
                null),
            Arguments.of(
                new EcKeyGenerationParameters("secp256r1"),
                new EcdsaSignatureParameters(HashAlgorithm.SHA512),
                "SHA512withECDSA",
                null),
            Arguments.of(
                new EcKeyGenerationParameters("secp521r1"),
                new EcdsaSignatureParameters(HashAlgorithm.SHA256),
                "SHA256withECDSA",
                null),
            Arguments.of(
                new EcKeyGenerationParameters("secp521r1"),
                new EcdsaSignatureParameters(HashAlgorithm.SHA384),
                "SHA384withECDSA",
                null),
            Arguments.of(
                new EcKeyGenerationParameters("secp521r1"),
                new EcdsaSignatureParameters(HashAlgorithm.SHA512),
                "SHA512withECDSA",
                null),
            Arguments.of(
                new Ed25519KeyGenerationParameters(),
                new Ed25519SignatureParameters(),
                "Ed25519",
                null),
            Arguments.of(
                new Ed448KeyGenerationParameters(),
                new Ed448SignatureParameters(),
                "Ed448",
                null));
    }

    @ParameterizedTest(name = "[{index}] in-memory / {0}")
    @MethodSource("keyGenerationParameters")
    void testInMemoryGenerate(KeyGenerationParameters parameters) throws Exception {
        // Given

        // When
        ManagedKey managedKey = keyManagementService.generate(providerId, parameters);

        // Then
        assertThat(managedKey).isNotNull();
        assertThat(managedKey.getKeyId()).isNotNull();
        assertThat(managedKey.getKeyAlgorithm()).isEqualTo(parameters.algorithm());
        assertThat(managedKey.getKeyGenerationParameters()).isEqualTo(parameters);
        assertThat(managedKey.getKeyMaterialRef()).isNotNull();
        assertThat(managedKey.getKeyProviderId().value()).isEqualTo(providerId);
        assertThat(managedKey.getCreatedAt()).isNotNull();
    }

    @ParameterizedTest(name = "[{index}] in-memory / {2}")
    @MethodSource("signatureCases")
    void testInMemorySign(
        KeyGenerationParameters keyParameters,
        SignatureParameters signatureParameters,
        String jcaAlgorithm,
        AlgorithmParameterSpec verificationParameters) throws Exception {
        // Given
        ManagedKey managedKey = keyManagementService.generate(
            "in-memory",
            keyParameters);
        byte[] data = "hello".getBytes(StandardCharsets.UTF_8);

        PublicKey publicKey = keyMaterialProviderRegistry
            .get(managedKey.getKeyProviderId())
            .getPublicKey(managedKey.getKeyMaterialRef())
            .orElseThrow();

        // When
        byte[] signed = keyManagementService.sign(
            managedKey.getKeyId(),
            signatureParameters,
            data);

        // Then: 원본 데이터 검증 성공
        assertThat(signed).isNotEmpty();

        Signature verifier = Signature.getInstance(
            jcaAlgorithm,
            cryptographicProvider);
        if (verificationParameters != null) {
            verifier.setParameter(verificationParameters);
        }
        verifier.initVerify(publicKey);
        verifier.update(data);

        assertThat(verifier.verify(signed)).isTrue();

        // 변조된 데이터 검증 실패
        verifier.initVerify(publicKey);
        verifier.update("tampered".getBytes(StandardCharsets.UTF_8));

        assertThat(verifier.verify(signed)).isFalse();
    }

}
