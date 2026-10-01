package toy.pki.ca.web.profile.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import toy.pki.ca.domain.certificate.CertType;
import toy.pki.ca.domain.certificate.DistinguishedName;
import toy.pki.ca.domain.extension.AuthorityInformationAccessProfile;
import toy.pki.ca.domain.extension.AuthorityKeyIdentifierProfile;
import toy.pki.ca.domain.extension.ExtendedKeyUsageRecord;
import toy.pki.ca.domain.extension.KeyUsageBit;
import toy.pki.ca.domain.extension.StandardExtendedKeyUsage;
import toy.pki.kms.domain.algorithm.KeyAlgorithmPreset;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpdateForm {
    @NotNull
    Long id;

    @NotBlank String alias;
    String description;

    @NotNull
    CertType certType;

    @Min(1)
    Integer maxValidDays;

    @Min(1)
    Integer defaultValidDays;

    @NotEmpty
    Set<KeyAlgorithmPreset> keyAlgorithms; // AllowedKeyAlgorithm

    @NotEmpty
    EnumSet<KeyUsageBit> keyUsageBits; // always critical

    boolean criticalExtendedKeyUsage;
    Set<StandardExtendedKeyUsage> extendedKeyUsages;
    List<ExtendedKeyUsageRecord> customExtendedKeyUsages;

    @Min(0)
    private Integer pathLenConstraint;

    @Valid
    DistinguishedName subjectDn;

    AuthorityKeyIdentifierProfile authorityKeyIdentifier;
    AuthorityInformationAccessProfile authorityInformationAccess;
}
