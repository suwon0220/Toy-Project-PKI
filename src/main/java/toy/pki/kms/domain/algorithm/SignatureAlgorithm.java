package toy.pki.kms.domain.algorithm;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SignatureAlgorithm {
    RSA("RSA"),
    DSA("DSA"),
    ECDSA("ECDSA"),
    EDDSA("EdDSA");

    private final String jcaName;
}
