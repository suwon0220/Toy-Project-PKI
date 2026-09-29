package toy.pki.ca.dto.profile;

import java.util.EnumSet;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NonNull;
import toy.pki.ca.domain.CertType;
import toy.pki.ca.domain.extension.KeyUsage;
import toy.pki.ca.domain.profile.AllowedCaIds;
import toy.pki.ca.domain.profile.AllowedKeyAlgorithm;
import toy.pki.ca.domain.profile.Profile;
import toy.pki.kms.domain.algorithm.KeyAlgorithmPreset;

@Data
public class ProfileForm {
    private ProfileIdForm profileIdForm;

    @NotNull private CertType certType;
    private AllowedCaIds allowedCaIds;
    private ValidityForm validityForm = new ValidityForm(0, 0);
    private AllowedKeyAlgorithm allowedKeyAlgorithm = new AllowedKeyAlgorithm(EnumSet.allOf(KeyAlgorithmPreset.class));
    private KeyUsageForm keyUsageForm = new KeyUsageForm(false, false, EnumSet.allOf(KeyUsage.class));
    private BasicConstraintsForm basicConstraintsForm = new BasicConstraintsForm(false);
    private ExtendedKeyUsageForm extendedKeyUsageForm; // TODO
    private AuthorityKeyIdentifierForm authorityKeyIdentifierForm;
    private AuthorityInformationAccessForm authorityInformationAccessForm;

    public @NonNull Profile toProfile() {
        throw new UnsupportedOperationException("Unimplemented method 'toProfile'");
    }
}
