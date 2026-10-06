package toy.pki.ca.web.profile;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Data;
import toy.pki.ca.domain.profile.CertificateProfile;
import toy.pki.ca.domain.profile.ExtendedKeyUsageOid;
import toy.pki.ca.domain.profile.KeyUsage;
import toy.pki.ca.domain.profile.SanType;
import toy.pki.ca.domain.profile.SubjectKeySpec;
import toy.pki.kms.web.KeyAlgorithmPreset;

@Data
public class CreateProfileForm {

    @NotBlank private String alias;
    private String description;

    @NotNull
    @Min(1)
    private Integer defaultValidityDays;

    @NotNull
    @Min(1)
    private Integer maxValidityDays;

    @NotEmpty private Set<SubjectKeySpec> keyAlgorithms = new LinkedHashSet<>();

    private boolean sanRequired;
    private Set<SanType> allowedSanTypes = new LinkedHashSet<>();

    private String subjectOrganization;
    private String subjectOrganizationalUnit;
    private String subjectLocality;
    private String subjectState;
    private String subjectCountry;

    private boolean ca;
    private Integer pathLenConstraint;

    @NotEmpty private Set<KeyUsage> keyUsages = new LinkedHashSet<>();

    private Set<String> extendedKeyUsageOids = new LinkedHashSet<>();

    public static CreateProfileForm from(CertificateProfile profile) {
        CreateProfileForm form = new CreateProfileForm();
        form.setAlias(profile.getAlias());
        form.setDescription(profile.getDescription());
        form.setDefaultValidityDays(profile.getDefaultValidityDays());
        form.setMaxValidityDays(profile.getMaxValidityDays());
        form.setKeyAlgorithms(Arrays.stream(KeyAlgorithmPreset.values())
                                    .filter(preset -> profile.getSubjectKeyPolicy().allows(preset.toParameters()))
                                    .collect(Collectors.toCollection(LinkedHashSet::new)));
        form.setSanRequired(profile.getSanPolicy().required());
        form.setAllowedSanTypes(new LinkedHashSet<>(profile.getSanPolicy().allowedTypes()));
        form.setSubjectOrganization(profile.getSubjectOrganization());
        form.setSubjectOrganizationalUnit(profile.getSubjectOrganizationalUnit());
        form.setSubjectLocality(profile.getSubjectLocality());
        form.setSubjectState(profile.getSubjectState());
        form.setSubjectCountry(profile.getSubjectCountry());
        form.setCa(profile.isCa());
        form.setPathLenConstraint(profile.getPathLenConstraint());
        form.setKeyUsages(new LinkedHashSet<>(profile.getKeyUsages()));
        form.setExtendedKeyUsageOids(profile.getExtendedKeyUsages().stream()
                                            .map(ExtendedKeyUsageOid::value)
                                            .collect(Collectors.toCollection(LinkedHashSet::new)));
        return form;
    }
}
