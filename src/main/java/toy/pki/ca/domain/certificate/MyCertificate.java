package toy.pki.ca.domain.certificate;

import java.security.cert.Certificate;
import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import toy.pki.ca.domain.profile.ProfileId;
import toy.pki.kms.domain.KeyId;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class MyCertificate {
    private final CertificateId id;
    private final ProfileId profileId;
    private final Certificate certificate;
    private String alias;
    private String description;
    private KeyId subjectKeyId;
    private CertificateId issuerCertificateId;
    private CertificateStatus status = CertificateStatus.ACTIVE;
    private Instant revokedAt;

    public void setAlias(String alias) {
        this.alias = alias == null || alias.isBlank() ? null : alias.strip();
    }

    public void expire() {
        if (status != CertificateStatus.REVOKED) {
            this.status = CertificateStatus.EXPIRED;
        }
    }

    public void suspend() {
        if (status == CertificateStatus.REVOKED) {
            throw new IllegalStateException("A revoked certificate cannot be suspended");
        }
        if (revokedAt == null) {
            revokedAt = Instant.now();
        }
        this.status = CertificateStatus.SUSPENDED;
    }

    public synchronized void revoke() {
        if (revokedAt == null) {
            revokedAt = Instant.now();
        }
        this.status = CertificateStatus.REVOKED;
    }
}
