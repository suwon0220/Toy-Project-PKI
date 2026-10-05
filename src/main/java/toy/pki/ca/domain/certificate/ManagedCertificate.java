package toy.pki.ca.domain.certificate;

import java.time.Instant;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ManagedCertificate {
    private final String id;
    private final String serialNumber;
    private final String profileId;
    private final String subjectKeyId;
    private final String issuerCertificateId;
    private final String subjectDn;
    private final String issuerDn;
    private CertificateStatus status = CertificateStatus.ACTIVE;
    private final Instant notBefore;
    private final Instant notAfter;
    private final Instant createdAt = Instant.now();
    private final byte[] encodedCertificate;

    public void expire() {
        this.status = CertificateStatus.EXPIRED;
    }

    public void suspend() {
        this.status = CertificateStatus.SUSPENDED;
    }

    public void revoke() {
        this.status = CertificateStatus.REVOKED;
    }
}
