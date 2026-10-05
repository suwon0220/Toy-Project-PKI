package toy.pki.ca.application.model;

import java.util.Set;

import toy.pki.ca.domain.profile.ExtendedKeyUsageOid;
import toy.pki.ca.domain.profile.KeyUsage;
import toy.pki.ca.domain.profile.SanPolicy;
import toy.pki.ca.domain.profile.SubjectKeyPolicy;

public record UpdateProfileCommand(
    String id,
    String alias,
    String description,

    int defaultValidityDays,
    int maxValidityDays,

    SubjectKeyPolicy subjectKeyPolicy,
    SanPolicy sanPolicy,

    String subjectOrganization,
    String subjectOrganizationalUnit,
    String subjectLocality,
    String subjectState,
    String subjectCountry,

    boolean ca,
    Integer pathLenConstraint,

    Set<KeyUsage> keyUsages,
    Set<ExtendedKeyUsageOid> extendedKeyUsages) {
}