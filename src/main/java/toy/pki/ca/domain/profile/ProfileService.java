package toy.pki.ca.domain.profile;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import toy.pki.ca.web.profile.form.ProfileSaveForm;
import toy.pki.ca.web.profile.form.ProfileUpdateForm;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final ProfileMapper profileMapper;

    public List<Profile> listProfiles() {
        return profileRepository.findAll();
    }


    public ProfileUpdateForm getProfile(UUID profileId) {
        Profile profile = profileRepository.findById(profileId);
        if (profile == null) {
            throw new IllegalArgumentException("Profile with ID " + profileId + " does not exist.");
        }
        return profileMapper.toUpdateForm(profile);
    }

    public UUID createProfile(ProfileSaveForm form) {
        log.info("Creating new profile: {}", form);
        Profile profile = profileMapper.toDomain(form);
        profileRepository.save(profile);
        return profile.getId();
    }

    public UUID updateProfile(UUID profileId, ProfileUpdateForm profileForm) {
        log.info("Updating profile with ID {}: {}", profileId, profileForm);
        Profile existingProfile = profileRepository.findById(profileId);
        if (existingProfile == null) {
            throw new IllegalArgumentException("Profile with ID " + profileId + " does not exist.");
        }
//        existingProfile.updateFromForm(profileForm);
        // the instance in the repository is already updated since it's the same object reference
        return existingProfile.getId();
    }

    public UUID deleteProfile(UUID profileId) {
        log.info("Deleting profile with ID {}", profileId);
        Profile existingProfile = profileRepository.findById(profileId);
        if (existingProfile == null) {
            throw new IllegalArgumentException("Profile with ID " + profileId + " does not exist.");
        }
        // Remove the profile from the repository
        UUID deletedProfileId = existingProfile.getId();
        profileRepository.remove(existingProfile);
        return profileId;
    }
}
