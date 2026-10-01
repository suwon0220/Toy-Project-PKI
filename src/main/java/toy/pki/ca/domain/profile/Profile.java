package toy.pki.ca.domain.profile;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.bouncycastle.asn1.x509.KeyUsage;

import lombok.Data;
import toy.pki.ca.domain.certificate.CertType;
import toy.pki.ca.domain.certificate.DistinguishedName;
import toy.pki.ca.domain.extension.AuthorityInformationAccessProfile;
import toy.pki.ca.domain.extension.AuthorityKeyIdentifierProfile;
import toy.pki.ca.domain.extension.BasicConstraintsProfile;
import toy.pki.ca.domain.extension.ExtendedKeyUsageProfile;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Profile {
    private Long id;
    private ProfileStatus status;

    @Nullable
    private String alias;

    @Nullable
    private String description;

    @NotEmpty
    private CertType certType;

    @Min(1)
    private Integer maxValidDays;

    @Min(1)
    private Integer defaultValidDays;

    private AllowedKeyAlgorithm allowedKeyAlgorithm;
    private KeyUsage keyUsage;
    private DistinguishedName subjectDn;
    private BasicConstraintsProfile basicConstraints;
    private ExtendedKeyUsageProfile extendedKeyUsage; // TODO
    private AuthorityKeyIdentifierProfile authorityKeyIdentifier;
    private AuthorityInformationAccessProfile authorityInformationAccess;
}
