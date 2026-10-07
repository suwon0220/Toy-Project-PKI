package toy.pki.ca.web.profile;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import toy.pki.ca.domain.policy.DnPolicy;
import toy.pki.ca.domain.profile.CertificateProfile;
import toy.pki.ca.domain.profile.CertificateType;
import toy.pki.ca.domain.profile.ExtendedKeyUsageOid;
import toy.pki.ca.domain.profile.KeyUsage;
import toy.pki.ca.domain.profile.SanType;
import toy.pki.ca.domain.profile.SubjectKeySpec;

@Data
@AllArgsConstructor
public class UpdateProfileForm {

    @NotBlank private String alias;
    private String description;

    @NotNull
    @Min(1)
    private Integer defaultValidityDays;

    @NotNull
    @Min(1)
    private Integer maxValidityDays;

    @NotEmpty private Set<SubjectKeySpec> keyAlgorithms;

    private boolean sanRequired;
    private Set<SanType> allowedSanTypes;

    // Subject Policy
    private DnPolicy dnPolicy;

    private CertificateType certificateType;
    private Integer pathLenConstraint;

    @NotEmpty private Set<KeyUsage> keyUsages;

    private Set<String> extendedKeyUsageOids;

    public static UpdateProfileForm from(CertificateProfile profile) {
        return new UpdateProfileForm(
            profile.getAlias(),
            profile.getDescription(),
            profile.getDefaultValidityDays(),
            profile.getMaxValidityDays(),
            Arrays.stream(SubjectKeySpec.values())
                  .filter(subjectKeySpec -> profile.getSubjectKeyPolicy().allows(subjectKeySpec))
                  .collect(Collectors.toCollection(LinkedHashSet::new)),
            profile.getSanPolicy().required(),
            new LinkedHashSet<>(profile.getSanPolicy().allowedTypes()),
            profile.getDnPolicy(),
            profile.getCertificateType(),
            profile.getPathLenConstraint(),
            new LinkedHashSet<>(profile.getKeyUsages()),
            profile.getExtendedKeyUsages().stream()
                   .map(ExtendedKeyUsageOid::value)
                   .collect(Collectors.toCollection(LinkedHashSet::new))

        );
    }
}
