package toy.pki.kms.service;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import toy.pki.kms.domain.algorithm.DigestAlgorithm;
import toy.pki.kms.domain.algorithm.KeyAlgorithmPreset;
import toy.pki.kms.domain.algorithm.SignatureAlgorithm;
import toy.pki.kms.domain.key.KeyManagementService;
import toy.pki.kms.domain.key.ManagedKey;
import toy.pki.kms.domain.request.KeyGenerationRequest;
import toy.pki.kms.domain.sig.SignatureService;

@SpringBootTest
class JcaServiceTest {

    @Autowired
    private KeyManagementService jcaKeyManagementService;

    @Autowired
    private SignatureService jcaSignatureService;

    static Stream<Arguments> rsaParameters() {
        return Stream.of(
                KeyAlgorithmPreset.RSA_2048,
                KeyAlgorithmPreset.RSA_3072,
                KeyAlgorithmPreset.RSA_4096
        ).flatMap(keyParameter
                        -> Stream.of(
                        DigestAlgorithm.SHA256,
                        DigestAlgorithm.SHA384,
                        DigestAlgorithm.SHA512
                ).map(digestAlgorithm
                                -> Arguments.of(
                                keyParameter,
                                SignatureAlgorithm.DSA,
                                digestAlgorithm
                        )
                )
        );
    }

    static Stream<Arguments> ecdsaParameters() {
        return Stream.of(
                KeyAlgorithmPreset.EC_P256,
                KeyAlgorithmPreset.EC_P384,
                KeyAlgorithmPreset.EC_P521
        ).flatMap(keyParameter
                        -> Stream.of(
                        DigestAlgorithm.SHA256,
                        DigestAlgorithm.SHA384,
                        DigestAlgorithm.SHA512
                ).map(digestAlgorithm
                                -> Arguments.of(
                                keyParameter,
                                SignatureAlgorithm.ECDSA,
                                digestAlgorithm
                        )
                )
        );
    }

    @ParameterizedTest
    @EnumSource(KeyAlgorithmPreset.class)
    void generate(KeyAlgorithmPreset keyAlgorithmPreset) throws Exception {
        KeyGenerationRequest request = new KeyGenerationRequest("", keyAlgorithmPreset);
        ManagedKey generated = jcaKeyManagementService.generate(request);
        assertThat(generated).isNotNull();
        assertThat(generated.getId()).isNotNull();
        assertThat(generated.getKeyAlgorithmPreset()).isNotNull();
        assertThat(generated.getKeyAlgorithmPreset()).isEqualTo(keyAlgorithmPreset);
        assertThat(generated.getKeyPair().getPublic().getAlgorithm()).isEqualTo(keyAlgorithmPreset.getAlgorithm().name());
        assertThat(generated.getKeyPair().getPrivate()).isNotNull();
        assertThat(generated.getKeyPair().getPublic()).isNotNull();
    }

    // @ParameterizedTest(name = "{0} / {1} / {2}")
    // @MethodSources({
    //         @MethodSource("rsaParameters"),
    //         @MethodSource("ecdsaParameters")
    // })
    // void sign_and_verifyRSA(
    //         KeyAlgorithmPreset keyAlgorithmPreset,
    //         SignatureAlgorithm signatureAlgorithm,
    //         DigestAlgorithm digestAlgorithm
    // ) throws Exception {
    //     KeyGenerationRequest request = new KeyGenerationRequest("", keyAlgorithmPreset);
    //     ManagedKey generated = jcaKeyManagementService.generate(request);
    //     SignatureParameter signatureParameter = new SignatureParameter(signatureAlgorithm, digestAlgorithm);
    //     String message = "Hello, World!";
    //     byte[] signature = jcaSignatureService.sign(generated.getKeyPair().getPrivate(), signatureParameter, message.getBytes());
    //     assertThat(signature).isNotNull();
    //     boolean isVerified = jcaSignatureService.verify(generated.getKeyPair().getPublic(), signatureParameter, message.getBytes(), signature);
    //     assertThat(isVerified).isTrue();
    // }
}
