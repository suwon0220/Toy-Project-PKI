package toy.pki.ca.dto.profile;

import lombok.Data;
import toy.pki.ca.domain.extension.KeyUsage;
import toy.pki.ca.domain.profile.AllowedCaIds;
import toy.pki.ca.domain.profile.AllowedKeyAlgorithm;
import toy.pki.kms.domain.algorithm.KeyAlgorithmPreset;

import java.util.EnumSet;
import java.util.Set;

@Data
public class ProfileForm {
    private ProfileIdForm profileIdForm;
    private CertTypeForm certTypeForm;
    private AllowedCaIds allowedCaIds;
    private ValidityForm validityForm = new ValidityForm(0, 0);
    private AllowedKeyAlgorithm allowedKeyAlgorithm = new AllowedKeyAlgorithm(EnumSet.allOf(KeyAlgorithmPreset.class));
    private KeyUsageForm keyUsageForm = new KeyUsageForm(false, false, EnumSet.allOf(KeyUsage.class));
    private BasicConstraintsForm basicConstraintsForm = new BasicConstraintsForm(false);
    private ExtendedKeyUsageForm extendedKeyUsageForm; // TODO
    private AuthorityKeyIdentifierForm authorityKeyIdentifierForm;
    private AuthorityInformationAccessForm authorityInformationAccessForm;
}
