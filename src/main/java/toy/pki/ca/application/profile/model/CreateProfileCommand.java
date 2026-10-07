package toy.pki.ca.application.profile.model;

import java.util.Set;

import toy.pki.ca.domain.certificate.CertificateSignatureAlgorithm;
import toy.pki.ca.domain.policy.KeyUsagePolicyEntry;
import toy.pki.ca.domain.profile.CertificateType;
import toy.pki.ca.domain.profile.ExtendedKeyUsageOid;
import toy.pki.ca.domain.profile.KeyUsage;
import toy.pki.ca.domain.policy.SanPolicy;
import toy.pki.ca.domain.policy.DnPolicy;
import toy.pki.ca.domain.policy.SubjectKeyPolicy;

public record CreateProfileCommand(
    String alias,
    String description,

    int defaultValidityDays,
    int maxValidityDays,

    SubjectKeyPolicy subjectKeyPolicy,
    Set<CertificateSignatureAlgorithm> allowedSignatures,
    SanPolicy sanPolicy,

    DnPolicy dnPolicy,

    CertificateType certificateType,
    Integer pathLenConstraint,

    Set<KeyUsage> keyUsages,
    Set<ExtendedKeyUsageOid> extendedKeyUsages) {

}
