package toy.pki.ca.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import toy.pki.ca.application.model.CreateProfileCommand;
import toy.pki.ca.application.model.UpdateProfileCommand;
import toy.pki.ca.application.port.CertificateProfileRepository;
import toy.pki.ca.domain.profile.CertificateProfile;
import toy.pki.ca.domain.profile.ProfileId;

@Slf4j
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
            request.sanPolicy(),
            request.subjectOrganization(),
            request.subjectOrganizationalUnit(),
            request.subjectLocality(),
            request.subjectState(),
            request.subjectCountry(),
            request.ca(),
            request.pathLenConstraint(),
            request.keyUsages(),
            request.extendedKeyUsages());
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
            profile.setAlias(request.alias());
            profile.setDescription(request.description());
            profile.setDefaultValidityDays(request.defaultValidityDays());
            profile.setMaxValidityDays(request.maxValidityDays());
            profile.setSubjectKeyPolicy(request.subjectKeyPolicy());
            profile.setSanPolicy(request.sanPolicy());
            profile.setSubjectOrganization(request.subjectOrganization());
            profile.setSubjectOrganizationalUnit(request.subjectOrganizationalUnit());
            profile.setSubjectLocality(request.subjectLocality());
            profile.setSubjectState(request.subjectState());
            profile.setSubjectCountry(request.subjectCountry());
            profile.setCa(request.ca());
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
        repository.findById(new ProfileId(profileId)).ifPresent(profile -> {
            profile.activate();
        });
    }

    public void deactivate(String profileId) {
        repository.findById(new ProfileId(profileId)).ifPresent(profile -> {
            profile.deactivate();
        });
    }

    public void delete(String profileId) {
        repository.delete(new ProfileId(profileId));
    }

}
