package toy.pki.ca.application.certificate.model;

import java.time.Instant;
import java.util.Set;
import lombok.NonNull;
import toy.pki.ca.domain.certificate.CertificateId;
import toy.pki.ca.domain.certificate.CertificateSignatureAlgorithm;
import toy.pki.ca.domain.certificate.CertificateSubject;
import toy.pki.ca.domain.certificate.SubjectAlternativeName;
import toy.pki.ca.domain.profile.ProfileId;
import toy.pki.kms.domain.KeyId;

public record IssueCertificateCommand(
    String alias,
    String description,
    @NonNull ProfileId profileId,
    @NonNull KeyId subjectKeyId,
    CertificateId issuerCertificateId,
    CertificateSubject subject,
    Set<SubjectAlternativeName> subjectAlternativeNames,
    Integer validityDays,
    Instant notBefore,
    CertificateSignatureAlgorithm signatureAlgorithm
) {
    public IssueCertificateCommand {
        subjectAlternativeNames = subjectAlternativeNames == null ? Set.of() : Set.copyOf(subjectAlternativeNames);
    }
}
