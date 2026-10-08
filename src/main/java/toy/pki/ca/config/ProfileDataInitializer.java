package toy.pki.ca.config;

import jakarta.annotation.PostConstruct;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import toy.pki.ca.application.profile.port.CertificateProfileRepository;
import toy.pki.ca.domain.certificate.CertificateSignatureAlgorithm;
import toy.pki.ca.domain.policy.DnAttributePolicy;
import toy.pki.ca.domain.policy.DnPolicy;
import toy.pki.ca.domain.policy.SanPolicy;
import toy.pki.ca.domain.policy.SubjectKeyPolicy;
import toy.pki.ca.domain.profile.CertificateProfile;
import toy.pki.ca.domain.profile.CertificateType;
import toy.pki.ca.domain.profile.KeyUsage;
import toy.pki.ca.domain.profile.ProfileId;
import toy.pki.ca.domain.profile.ProfileStatus;
import toy.pki.ca.domain.profile.SubjectKeySpec;

@Component
@RequiredArgsConstructor
public class ProfileDataInitializer {

    private final CertificateProfileRepository certificateProfileRepository;

    @PostConstruct
    public void init() {
        // V2G Root CA Profile
        certificateProfileRepository.save(
            new CertificateProfile(
                new ProfileId(UUID.randomUUID().toString()),
                "V2G Root CA",
                "This is the ISO 15118 V2G Root CA profile.",
                ProfileStatus.ACTIVE,
                14600,
                14600,
                new SubjectKeyPolicy(Set.of(SubjectKeySpec.EC_P256)),
                Set.of(CertificateSignatureAlgorithm.ECDSA_WITH_SHA256),
                new SanPolicy(false, Set.of()),
                new DnPolicy(
                    new DnAttributePolicy("V2G"), // Domain Component (DC)
                    new DnAttributePolicy(true), // Common Name (CN)
                    new DnAttributePolicy(false), // Organizational Unit (OU)
                    new DnAttributePolicy(true), // Organization (O)
                    new DnAttributePolicy(false) // Country (C)
                ),
                CertificateType.ROOT_CA,
                null,
                Set.of(KeyUsage.KEY_CERT_SIGN, KeyUsage.CRL_SIGN),
                Set.of()));

        // CPO Sub CA 1 Profile
        certificateProfileRepository.save(
            new CertificateProfile(
                new ProfileId(UUID.randomUUID().toString()),
                "CPO Sub CA 1",
                "This is the ISO 15118 CPO Sub CA 1 profile.",
                ProfileStatus.ACTIVE,
                1460, // 4 years
                1460, // 4 years
                new SubjectKeyPolicy(Set.of(SubjectKeySpec.EC_P256)),
                Set.of(CertificateSignatureAlgorithm.ECDSA_WITH_SHA256),
                new SanPolicy(false, Set.of()),
                new DnPolicy(
                    new DnAttributePolicy(false), // Domain Component (DC)
                    new DnAttributePolicy(true), // Common Name (CN)
                    new DnAttributePolicy(false), // Organizational Unit (OU)
                    new DnAttributePolicy(true), // Organization (O)
                    new DnAttributePolicy(false) // Country (C)
                ),
                CertificateType.INTERMEDIATE_CA,
                1,
                Set.of(KeyUsage.KEY_CERT_SIGN, KeyUsage.CRL_SIGN),
                Set.of()));

        // CPO Sub CA 2 Profile
        certificateProfileRepository.save(
            new CertificateProfile(
                new ProfileId(UUID.randomUUID().toString()),
                "CPO Sub CA 2",
                "This is the ISO 15118 CPO Sub CA 2 profile.",
                ProfileStatus.ACTIVE,
                730, // 2 years
                730, // 2 years
                new SubjectKeyPolicy(Set.of(SubjectKeySpec.EC_P256)),
                Set.of(CertificateSignatureAlgorithm.ECDSA_WITH_SHA256),
                new SanPolicy(false, Set.of()),
                new DnPolicy(
                    new DnAttributePolicy(false), // Domain Component (DC)
                    new DnAttributePolicy(true), // Common Name (CN)
                    new DnAttributePolicy(false), // Organizational Unit (OU)
                    new DnAttributePolicy(true), // Organization (O)
                    new DnAttributePolicy(false) // Country (C)
                ),
                CertificateType.INTERMEDIATE_CA,
                0,
                Set.of(KeyUsage.KEY_CERT_SIGN, KeyUsage.CRL_SIGN),
                Set.of()));

        // SECC Cert Profile
        certificateProfileRepository.save(
            new CertificateProfile(
                new ProfileId(UUID.randomUUID().toString()),
                "SECC Cert",
                "This is the ISO 15118 SECC Cert profile.",
                ProfileStatus.ACTIVE,
                90, // 3 months
                90, // 3 months
                new SubjectKeyPolicy(Set.of(SubjectKeySpec.EC_P256)),
                Set.of(CertificateSignatureAlgorithm.ECDSA_WITH_SHA256),
                new SanPolicy(false, Set.of()),
                new DnPolicy(
                    new DnAttributePolicy("CPO"), // Domain Component (DC)
                    new DnAttributePolicy(true), // Common Name (CN)
                    new DnAttributePolicy(false), // Organizational Unit (OU)
                    new DnAttributePolicy(true), // Organization (O)
                    new DnAttributePolicy(false) // Country (C)
                ),
                CertificateType.END_ENTITY,
                null,
                Set.of(KeyUsage.DATA_ENCIPHERMENT),
                Set.of()));
    }
}
