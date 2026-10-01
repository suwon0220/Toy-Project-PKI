package toy.pki;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.springframework.stereotype.Component;
import toy.pki.ca.domain.certificate.CertType;
import toy.pki.ca.domain.certificate.DistinguishedName;
import toy.pki.ca.domain.extension.AuthorityInformationAccessProfile;
import toy.pki.ca.domain.extension.AuthorityKeyIdentifierProfile;
import toy.pki.ca.domain.extension.BasicConstraintsProfile;
import toy.pki.ca.domain.extension.ExtendedKeyUsageProfile;
import toy.pki.ca.domain.profile.AllowedKeyAlgorithm;
import toy.pki.ca.domain.profile.Profile;
import toy.pki.ca.domain.profile.ProfileRepository;
import toy.pki.ca.domain.profile.ProfileStatus;
import toy.pki.kms.domain.algorithm.KeyAlgorithmPreset;

@Component
@RequiredArgsConstructor
public class TestDataInit {

    private final ProfileRepository profileRepository;


    @PostConstruct
    public void init() {
        DistinguishedName subjectDn = new DistinguishedName();
        subjectDn.setCountry("KR");
        subjectDn.setOrganization("Toy PKI");
        subjectDn.setCommonName("Test Root CA");

        profileRepository.save(
            new Profile(
                null,
                ProfileStatus.ACTIVE,
                "Test Root CA",
                "Test profile for a self-signed Root CA certificate",
                CertType.ROOT_CA,
                3650,
                3650,
                new AllowedKeyAlgorithm(Arrays.stream(KeyAlgorithmPreset.values()).collect(Collectors.toSet())),
                new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign),
                subjectDn,
                new BasicConstraintsProfile(true, 2),
                new ExtendedKeyUsageProfile(false, new HashSet<>()),
                new AuthorityKeyIdentifierProfile(false),
                new AuthorityInformationAccessProfile(false, false, false)
            )
        );
    }
}
