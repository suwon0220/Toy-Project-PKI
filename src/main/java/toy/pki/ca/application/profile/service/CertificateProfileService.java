package toy.pki.ca.application.profile.service;

import java.util.List;
import java.util.UUID;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import toy.pki.ca.application.profile.model.CreateProfileCommand;
import toy.pki.ca.application.profile.model.UpdateProfileCommand;
import toy.pki.ca.application.profile.port.CertificateProfileRepository;
import toy.pki.ca.domain.profile.CertificateProfile;
import toy.pki.ca.domain.profile.ProfileId;
import toy.pki.ca.domain.profile.ProfileStatus;

@Slf4j
@Data
@Service
@RequiredArgsConstructor
public class CertificateProfileService {

    private final CertificateProfileRepository repository;

    public ProfileId createDraft(CreateProfileCommand request) {
        CertificateProfile profile = new CertificateProfile(
            new ProfileId(UUID.randomUUID().toString()),
            request.alias(),
            request.description(),
            request.defaultValidityDays(),
            request.maxValidityDays(),
            request.subjectKeyPolicy(),
            request.allowedSignatures(),
            request.sanPolicy(),
            request.dnPolicy(),
            request.certificateType(),
            request.pathLenConstraint(),
            request.keyUsages(), request.extendedKeyUsages());
        repository.save(profile);
        return profile.getId();
    }

    public CertificateProfile findById(String profileId) {
        return repository.findById(new ProfileId(profileId))
                         .orElseThrow(() -> new IllegalArgumentException("Profile not found: " + profileId));
    }

    public List<CertificateProfile> findAll() {
        return repository.findAll();
    }

    public void updateDraft(UpdateProfileCommand request) {
        repository.findById(new ProfileId(request.id())).ifPresent(profile -> {
            if (profile.getStatus() != ProfileStatus.DRAFT) {
                throw new IllegalStateException("Only DRAFT profiles can be updated.");
            }
            profile.setAlias(request.alias());
            profile.setDescription(request.description());
            profile.setDefaultValidityDays(request.defaultValidityDays());
            profile.setMaxValidityDays(request.maxValidityDays());
            profile.setSubjectKeyPolicy(request.subjectKeyPolicy());
            profile.setSanPolicy(request.sanPolicy());
            profile.setDnPolicy(request.dnPolicy());
            profile.setCertificateType(request.certificateType());
            profile.setPathLenConstraint(request.pathLenConstraint());
            profile.setKeyUsages(request.keyUsages());
            profile.setExtendedKeyUsages(request.extendedKeyUsages());
        });
    }

    public void duplicateDraft(String profileId) {
        repository.findById(new ProfileId(profileId)).ifPresent(profile -> {
            CertificateProfile duplicateProfile = profile.duplicate();
            repository.save(duplicateProfile);
        });
    }

    public void activate(String profileId) {
        repository.findById(new ProfileId(profileId)).ifPresent(CertificateProfile::activate);
    }

    public void deactivate(String profileId) {
        repository.findById(new ProfileId(profileId)).ifPresent(CertificateProfile::deactivate);
    }

    public void delete(String profileId) {
        repository.delete(new ProfileId(profileId));
    }

}
