package toy.pki.ca.domain.certificate;

import java.time.Instant;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import toy.pki.ca.domain.profile.ProfileId;
import toy.pki.kms.domain.key.KeyId;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class ManagedCertificate {
    private final CertificateId id;
    private final CertificateSerialNumber serialNumber;
    private String alias;
    private String description;

    private final ProfileId profileId;
    private final KeyId subjectKeyId;
    private final CertificateId issuerCertificateId;

    private final CertificateSubject subject;
    private final CertificateSubject issuer;
    private final Set<SubjectAlternativeName> subjectAlternativeNames;

    private final CertificateValidity validity;
    private final Instant createdAt = Instant.now();

    private CertificateSignatureAlgorithm signatureAlgorithm;

    private final byte[] encodedCertificate;
    private CertificateStatus status = CertificateStatus.ACTIVE;

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
