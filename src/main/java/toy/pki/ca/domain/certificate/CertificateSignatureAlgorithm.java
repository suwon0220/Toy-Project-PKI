package toy.pki.ca.domain.certificate;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CertificateSignatureAlgorithm {
    RSA_WITH_SHA256("SHA256withRSA"),
    RSA_WITH_SHA384("SHA384withRSA"),
    RSA_WITH_SHA512("SHA512withRSA"),
    ECDSA_WITH_SHA256("ecdsa-with-SHA256"),
    ECDSA_WITH_SHA384("ecdsa-with-SHA384"),
    ECDSA_WITH_SHA512("ecdsa-with-SHA512"),
    ED25519("Ed25519"),
    ED448("Ed448"),
    UNKNOWN("unknown");

    private final String jcaName;
}
