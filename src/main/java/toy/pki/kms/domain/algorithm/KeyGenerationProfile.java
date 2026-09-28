package toy.pki.kms.domain.algorithm;

import java.util.function.Supplier;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import toy.pki.kms.domain.parameter.EcKeyGenerationParameter;
import toy.pki.kms.domain.parameter.KeyGenerationParameter;
import toy.pki.kms.domain.parameter.RsaKeyGenerationParameter;

@Getter
@RequiredArgsConstructor
public enum KeyGenerationProfile {

    RSA_2048(
            "RSA 2048",
            KeyAlgorithm.RSA,
            () -> new RsaKeyGenerationParameter(
                    RsaKeySize.RSA_2048.getBits()
            )
    ),
    RSA_3072(
            "RSA 3072",
            KeyAlgorithm.RSA,
            () -> new RsaKeyGenerationParameter(
                    RsaKeySize.RSA_3072.getBits()
            )
    ),
    RSA_4096(
            "RSA 4096",
            KeyAlgorithm.RSA,
            () -> new RsaKeyGenerationParameter(
                    RsaKeySize.RSA_4096.getBits()
            )
    ),
    EC_P256(
            "P256",
            KeyAlgorithm.EC,
            () -> new EcKeyGenerationParameter(
                    EcCurve.secp256r1
            )
    ),
    EC_P384(
            "P384",
            KeyAlgorithm.EC,
            () -> new EcKeyGenerationParameter(
                    EcCurve.secp384r1
            )
    ),
    EC_P521(
            "P521",
            KeyAlgorithm.EC,
            () -> new EcKeyGenerationParameter(
                    EcCurve.secp521r1
            )
    );

    private final String displayName;

    private final KeyAlgorithm algorithm;

    private final Supplier<KeyGenerationParameter> parameterSupplier;

    public KeyGenerationParameter createParameter() {
        return parameterSupplier.get();
    }
}
