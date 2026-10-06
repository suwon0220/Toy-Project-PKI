package toy.pki.ca.config;

import jakarta.annotation.PostConstruct;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import toy.pki.ca.application.profile.port.CertificateProfileRepository;
import toy.pki.ca.domain.certificate.CertificateStatus;
import toy.pki.ca.domain.profile.CertificateProfile;
import toy.pki.ca.domain.profile.ProfileId;
import toy.pki.ca.domain.profile.ProfileStatus;
import toy.pki.ca.domain.profile.SubjectKeyPolicy;
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
                new SubjectKeyPolicy(
                    SubjectKeySpec.EC_P256;
                ),
                null,
                null,
                null,
                null,
                null,
                null,
                null
            )
        );
    }
}
