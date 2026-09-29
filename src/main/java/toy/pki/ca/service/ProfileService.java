package toy.pki.ca.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import toy.pki.ca.domain.profile.Profile;
import toy.pki.ca.dto.profile.ProfileForm;
import toy.pki.ca.repository.CertificateProfileRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final CertificateProfileRepository certificateProfileRepository;

    public List<Profile> findAll() {
        return certificateProfileRepository.findAll()
            .stream()
            .collect(Collectors.toList());
    }

    public Profile findById(UUID profileId) {
        return certificateProfileRepository.findById(profileId);
    }

    public void addProfile(ProfileForm profileForm) {
        certificateProfileRepository.save(profileForm.toProfile());
    }

    public void updateProfile(UUID profileId, ProfileForm profileForm) {

        Profile existingProfile = certificateProfileRepository.findById(profileId);

        if (existingProfile == null) {
            throw new IllegalArgumentException("Profile with ID " + profileId + " does not exist.");
        }

        existingProfile.updateFromForm(profileForm);
        // the instance in the repository is already updated since it's the same object reference
    }

    public void deleteProfile(UUID profileId) {
        Profile existingProfile = certificateProfileRepository.findById(profileId);

        if (existingProfile == null) {
            throw new IllegalArgumentException("Profile with ID " + profileId + " does not exist.");
        }

        // Remove the profile from the repository
        certificateProfileRepository.remove(existingProfile);
    }
}
