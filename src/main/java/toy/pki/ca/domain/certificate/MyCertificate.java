package toy.pki.ca.domain.certificate;

import java.security.cert.Certificate;
import java.time.Instant;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import lombok.*;
import toy.pki.ca.adapter.persistence.jpa.CertificateDerConverter;
import toy.pki.ca.domain.profile.ProfileId;
import toy.pki.kms.domain.KeyId;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class MyCertificate {

    @EmbeddedId
    @AttributeOverride(name = "id", column = @Column(name = "certificate_id"))
    private CertificateId id;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "profile_id"))
    private ProfileId profileId;

    @Lob
    @Convert(converter = CertificateDerConverter.class)
    @Column(name = "certificate_der")
    private Certificate certificate;

    private String alias;
    private String description;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "subject_key_id"))
    private KeyId subjectKeyId;

    @Embedded
    @AttributeOverride(name = "id", column = @Column(name = "issuer_certificate_id"))
    private CertificateId issuerCertificateId;

    private CertificateStatus status = CertificateStatus.ACTIVE;
    private Instant revokedAt;

    public MyCertificate(CertificateId id, ProfileId profileId, Certificate certificate) {
        this.id = id;
        this.profileId = profileId;
        this.certificate = certificate;
    }

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
