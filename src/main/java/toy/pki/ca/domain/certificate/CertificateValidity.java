package toy.pki.ca.domain.certificate;

import java.time.Instant;

public record CertificateValidity(
    Instant notBefore,
    Instant notAfter
) {

}
