package toy.pki.ca.domain.certificate;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExtKeyUsage {
    SERVER_AUTH("1.3.6.1.5.5.7.3.1"),
    CLIENT_AUTH("1.3.6.1.5.5.7.3.2"),
    CODE_SIGNING("1.3.6.1.5.5.7.3.3"),
    EMAIL_PROTECTION("1.3.6.1.5.5.7.3.4"),
    TIME_STAMPING("1.3.6.1.5.5.7.3.8"),
    OCSP_SIGNING("1.3.6.1.5.5.7.3.9");

    private final String oid;
}
