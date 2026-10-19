package toy.pki.ca.application.profile.service;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import toy.pki.ca.application.profile.model.CreateProfileCommand;
import toy.pki.ca.application.profile.model.ProfileSearchCriteria;
import toy.pki.ca.application.profile.model.UpdateProfileCommand;
import toy.pki.ca.application.profile.port.CertificateProfileRepository;
import toy.pki.ca.domain.profile.CertificateProfile;
import toy.pki.ca.domain.profile.ProfileId;
import toy.pki.ca.domain.profile.ProfileStatus;

@Slf4j
@Service
@RequiredArgsConstructor
public class CertificateProfileService {

    private final CertificateProfileRepository repository;
    private final Validator validator;

    public ProfileId createDraft(CreateProfileCommand request) {
        CertificateProfile profile = new CertificateProfile(
            new ProfileId(UUID.randomUUID().toString()),
            request.alias(),
            request.description(),
            ProfileStatus.DRAFT,
            request.defaultValidityDays(),
            request.maxValidityDays(),
            request.subjectKeyPolicy(),
            request.allowedSignatures(),
            request.sanPolicy(),
            request.dnPolicy(),
            request.certificateType(),
            request.pathLenConstraint(),
            request.keyUsages(), request.extendedKeyUsages());
        validate(profile);
        repository.save(profile);
        return profile.getId();
    }

    public CertificateProfile findById(String profileId) {
        return repository.findById(new ProfileId(profileId))
                         .orElseThrow(() -> new IllegalArgumentException("Profile not found: " + profileId));
    }

    public List<CertificateProfile> findAll() {
        return repository.findAll(new ProfileSearchCriteria(null));
    }

    @Transactional
    public void updateDraft(UpdateProfileCommand request) {
        repository.findById(new ProfileId(request.id())).ifPresent(profile -> {
            if (profile.getStatus() != ProfileStatus.DRAFT) {
                throw new IllegalStateException("Only DRAFT profiles can be updated.");
            }
            var candidate = new CertificateProfile(profile.getId(), request.alias(), request.description(),
                profile.getStatus(), request.defaultValidityDays(), request.maxValidityDays(),
                request.subjectKeyPolicy(), profile.getAllowedSignatures(), request.sanPolicy(), request.dnPolicy(),
                request.certificateType(), request.pathLenConstraint(), request.keyUsages(), request.extendedKeyUsages());
            validate(candidate);
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

    @Transactional
    public void activate(String profileId) {
        repository.findById(new ProfileId(profileId)).ifPresent(CertificateProfile::activate);
    }

    @Transactional
    public void deactivate(String profileId) {
        repository.findById(new ProfileId(profileId)).ifPresent(CertificateProfile::deactivate);
    }

    public void delete(String profileId) {
        repository.delete(new ProfileId(profileId));
    }

    private void validate(CertificateProfile profile) {
        var violations = validator.validate(profile);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }
}
