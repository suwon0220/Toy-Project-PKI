package toy.pki.ca.domain.profile;

import org.bouncycastle.asn1.x509.Validity;

import lombok.Data;
import toy.pki.ca.domain.CertType;
import toy.pki.ca.domain.extension.AuthorityInformationAccessExtension;
import toy.pki.ca.domain.extension.AuthorityKeyIdentifierExtension;
import toy.pki.ca.domain.extension.BasicConstraintsExtension;
import toy.pki.ca.domain.extension.ExtendedKeyUsageExtension;
import toy.pki.ca.domain.extension.KeyUsage;
import toy.pki.ca.dto.profile.ProfileForm;

@Data
public class Profile {
    private ProfileId profileId;
    private CertType certType;
    private AllowedCaIds allowedCaIds;
    private Validity validity;
    private AllowedKeyAlgorithm allowedKeyAlgorithm;
    private KeyUsage keyUsage;
    private BasicConstraintsExtension basicConstraints;
    private ExtendedKeyUsageExtension extendedKeyUsage; // TODO
    private AuthorityKeyIdentifierExtension authorityKeyIdentifier;
    private AuthorityInformationAccessExtension authorityInformationAccess;

    public Profile(ProfileForm profileForm) {
        throw new UnsupportedOperationException("Unimplemented constructor 'Profile(ProfileForm profileForm)'");
    }

    public void updateFromForm(ProfileForm profileForm) {
        throw new UnsupportedOperationException("Unimplemented method 'updateFromForm'");
    }
}
