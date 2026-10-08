package toy.pki.ca.domain.certificate;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CertificateSignatureAlgorithm {
    RSA_WITH_SHA256("SHA256withRSA"),
    RSA_WITH_SHA384("SHA384withRSA"),
    RSA_WITH_SHA512("SHA512withRSA"),
    ECDSA_WITH_SHA256("SHA256withECDSA"),
    ECDSA_WITH_SHA384("SHA384withECDSA"),
    ECDSA_WITH_SHA512("SHA512withECDSA"),
    ED25519("Ed25519"),
    ED448("Ed448"),
    UNKNOWN("unknown");

    private final String jcaName;
}
