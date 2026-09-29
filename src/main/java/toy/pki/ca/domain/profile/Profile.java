package toy.pki.ca.domain.profile;

import lombok.Data;
import org.bouncycastle.asn1.x509.Validity;
import toy.pki.ca.domain.CertType;
import toy.pki.ca.domain.extension.*;

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
}
