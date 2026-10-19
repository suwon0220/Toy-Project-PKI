package toy.pki.ca.domain.certificate;

import jakarta.persistence.Embeddable;

@Embeddable
public record CertificateId(
    String id
) {

}
